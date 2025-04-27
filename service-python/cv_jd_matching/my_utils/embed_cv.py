# Initialize Chroma DB client
import json
from typing import Optional
import chromadb
import numpy as np
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

from parsers.cv_parse import transform_dto_to_cv

# chroma_client = chromadb.PersistentClient(path="./chroma_data")
# cv_collection_concat = chroma_client.get_or_create_collection(name="cv_embeddings_concatenated")
#
# # Load Hugging Face model for embeddings
# embedding_model = SentenceTransformer('all-MiniLM-L6-v2')

class CvDTO(BaseModel):
    id: int
    technicalSkills: Optional[str]
    foreignLanguages: Optional[str]
    education: Optional[str]
    certifications: Optional[str]
    projectExperience: Optional[str]
    workExperience: Optional[str]
    others: Optional[str]

def normalize(text):
    """Normalizează textul: minuscule, elimină spațiile de la început/sfârșit."""
    return text.lower().strip() if isinstance(text, str) else ""

def prioritized_flatten(cv):
    """
    Extrage și prioritizează textul din diferite secțiuni ale CV-ului,
    incluzând descrierile din experiență cu prioritate dedicată.
    """
    print("\n--- Extragere și Prioritizare Text CV ---")

    # 1. Technical Skills (Prioritate maximă)
    tech_skills = [normalize(item.get("skill", "")) for item in cv.get("technical_skills", [])]
    print(f"Technical Skills: {len(tech_skills)} items")

    # 2. Project Experience (Tehnologii + Descrieri)
    project_techs = []
    project_descriptions = []
    print(f"Processing Project Experience ({len(cv.get('project_experience', []))} items)...")
    for i, proj in enumerate(cv.get("project_experience", [])):
        # Extrage Tehnologii
        techs = [normalize(tech) for tech in proj.get("technologies", [])]
        project_techs.extend(techs)
        # Extrage Descrierea
        description = proj.get("description", "")
        if isinstance(description, str):
            project_descriptions.append(normalize(description))
        elif isinstance(description, list): # Dacă descrierea e o listă de string-uri
            project_descriptions.extend([normalize(d) for d in description if isinstance(d, str)])
        # print(f"  Project {i+1}: Found {len(techs)} techs, Description length: {len(description) if isinstance(description, (str, list)) else 0}")
    print(f"Project Technologies: {len(project_techs)} items")
    # print(f"Project Descriptions: {len(project_descriptions)} items") # Afișare opțională

    # 3. Work Experience (Tehnologii + Descrieri)
    work_techs = []
    work_descriptions = []
    print(f"Processing Work Experience ({len(cv.get('work_experience', []))} items)...")
    for i, job in enumerate(cv.get("work_experience", [])):
        # Extrage Tehnologii
        techs = [normalize(tech) for tech in job.get("technologies", [])]
        work_techs.extend(techs)
        # Extrage Descrierea
        description = job.get("description", "")
        if isinstance(description, str):
            work_descriptions.append(normalize(description))
        elif isinstance(description, list): # Dacă descrierea e o listă de string-uri
            work_descriptions.extend([normalize(d) for d in description if isinstance(d, str)])
        # print(f"  Work {i+1}: Found {len(techs)} techs, Description length: {len(description) if isinstance(description, (str, list)) else 0}")
    print(f"Work Technologies: {len(work_techs)} items")
    # print(f"Work Descriptions: {len(work_descriptions)} items") # Afișare opțională

    # Combinăm descrierile și eliminăm duplicatele
    experience_descriptions = sorted(list(set(project_descriptions + work_descriptions)))
    print(f"Combined Experience Descriptions (Unique): {len(experience_descriptions)} items")


    # 4. Certifications (Tehnologii + Nume)
    cert_techs = []
    cert_names = []
    print(f"Processing Certifications ({len(cv.get('certifications', []))} items)...")
    for i, cert in enumerate(cv.get("certifications", [])):
        techs = [normalize(tech) for tech in cert.get("technologies", [])]
        cert_techs.extend(techs)
        if "name" in cert:
             name = normalize(cert.get("name", ""))
             cert_names.append(name)
    print(f"Certification Technologies: {len(cert_techs)} items")
    print(f"Certification Names: {len(cert_names)} items")


    # 5. Fallback Texts (Educație, Altele, și alte câmpuri din experiență/certificări)
    fallback_texts = []
    print("Processing Fallback Texts (Education, Others, etc.)...")
    # Includem și numele certificatelor în fallback pentru potrivire generală
    fallback_texts.extend(cert_names)

    # Iterăm prin secțiuni, dar EXCLUDEM explicit 'technologies' și 'description'
    # din Project/Work Experience, deoarece sunt deja tratate cu prioritate mai mare.
    sections_for_fallback = {
        "education": ["institution", "degree", "field_of_study"], # Adaugă câmpuri relevante
        "others": None, # Procesează toate valorile din 'others'
        "project_experience": ["title"], # Extrage doar titlul din proiecte ca fallback
        "work_experience": ["title", "company"], # Extrage titlu/companie din work ca fallback
        "certifications": ["institution"] # Extrage instituția certificatului ca fallback (numele e deja adăugat)
        # Adaugă/elimină secțiuni/câmpuri după nevoie
    }

    for section_key, relevant_keys in sections_for_fallback.items():
        entries = cv.get(section_key, [])
        if not entries: continue

        if isinstance(entries, dict): # Cazul 'others'
            # Procesăm toate valorile din dicționarul 'others'
            if section_key == 'others':
                for sub_key, sub_value in entries.items():
                    if isinstance(sub_value, str):
                        fallback_texts.append(normalize(sub_value))
                    elif isinstance(sub_value, list):
                        for item in sub_value:
                             if isinstance(item, str):
                                fallback_texts.append(normalize(item))
                             elif isinstance(item, dict):
                                fallback_texts.extend([normalize(v) for v in item.values() if isinstance(v, str)])
            else: # Convertim alte dicționare în liste (deși structura standard e listă)
                entries = [entries]

        if isinstance(entries, list): # Procesăm liste (cazul standard pt majoritatea secțiunilor)
             for entry in entries:
                if isinstance(entry, dict):
                    # Extragem doar cheile relevante specificate, dacă există
                    keys_to_extract = relevant_keys if relevant_keys is not None else entry.keys()
                    for key in keys_to_extract:
                        value = entry.get(key)
                        if isinstance(value, str):
                            fallback_texts.append(normalize(value))
                        elif isinstance(value, list):
                            for item in value:
                                if isinstance(item, str):
                                    fallback_texts.append(normalize(item))
                                # Extindem și din dicționare din liste (ex: Contact Information în Others)
                                elif isinstance(item, dict) and section_key == 'others':
                                     fallback_texts.extend([normalize(v) for v in item.values() if isinstance(v, str)])
                elif isinstance(entry, str): # Unele secțiuni pot fi liste de string-uri direct
                    fallback_texts.append(normalize(entry))


    # Eliminăm duplicatele și textele goale din fallback final
    fallback_texts = sorted(list(set(filter(None, fallback_texts))))
    print(f"Fallback Texts (Diverse - Unique): {len(fallback_texts)} items")
    print("--- Sfârșit Extragere CV ---")

    # Returnăm sursele în ordinea priorității dorite:
    # 1. Skill-uri Tehnice Directe
    # 2. Tehnologii Proiecte
    # 3. Tehnologii Work
    # 4. Tehnologii Certificări
    # 5. Descrieri Experiență (Proiecte + Work) <- NOU
    # 6. Texte Fallback (Educație, Altele, Nume Certificări, Titluri etc.)
    prioritized_sources = {
        "tech_skills": tech_skills,
        "project_techs": project_techs,
        "work_techs": work_techs,
        "cert_techs": cert_techs,
        "experience_descriptions":experience_descriptions, # Lista nouă inserată aici
        "fallback_texts": fallback_texts,
    }
    # Verificăm că toate sunt liste de stringuri
    for key, source_list in prioritized_sources.items():
        if not isinstance(source_list, list):
            print(f"Warning: Valoarea pentru '{key}' nu este o listă!")
        elif not all(isinstance(item, str) for item in source_list):
            print(f"Warning: Lista '{key}' conține elemente non-string!")

    return prioritized_sources

def extract_cv_text(cv: dict) -> str:
    sections = []

    for job in cv.get("work_experience", []):
        sections.extend(job.get("description", []))
        if job.get("technologies"):
            sections.append("Technologies used: " + ', '.join(job["technologies"]))

    for proj in cv.get("project_experience", []):
        sections.append(proj.get("description", ""))
        if proj.get("technologies"):
            sections.append("Project tech: " + ', '.join(proj["technologies"]))

    for cert in cv.get("certifications", []):
        sections.append(cert["name"])

    edu = cv.get("education", [])
    if edu:
        sections.append("Field of study: " + edu[0].get("field_of_study", ""))

    return "\n".join(sections)

def embed_sections_cv(request: CvDTO, embedding_model: SentenceTransformer, cv_collection_concat):

    transformed_cv = transform_dto_to_cv({"technicalSkills": request.technicalSkills,
                                          "foreignLanguages": request.foreignLanguages,
                                          "education": request.education,
                                          "certifications": request.certifications,
                                          "projectExperience": request.projectExperience,
                                          "workExperience": request.workExperience,
                                          "others": request.others })

    text_for_embedding = extract_cv_text(transformed_cv)
    cv_skills = [s['skill'] for s in transformed_cv.get('technical_skills', [])]
    cv_languages = [normalize(lang.get("language", "")) for lang in transformed_cv.get("foreign_languages", [])]
    cv_cert = [cert["name"] for cert in transformed_cv.get("certifications", [])]

    prioritized_sources = prioritized_flatten(transformed_cv)
    # print(prioritized_sources)

    try:
        embedding = embedding_model.encode(text_for_embedding, convert_to_tensor=True)
    except Exception as e:
        print(f"Embedding model error: {str(e)}")
        return "Failed"

    if not isinstance(embedding, (list, np.ndarray)):
        print(f"Embedding is not a list or array.")
    if len(embedding) == 0:
        print(f"Embedding is empty.")

    cv_collection_concat.add(
        ids=[f"{request.id}"],
        embeddings=[embedding.tolist()],
        documents= [text_for_embedding],
        metadatas=[{
            "skills": json.dumps(cv_skills),
            "languages": json.dumps(cv_languages),
            "certifications": json.dumps(cv_cert),
            "tech_skills": json.dumps(prioritized_sources.get("tech_skills", [])),
            "project_techs": json.dumps(prioritized_sources.get("project_techs", [])),
            "work_techs": json.dumps(prioritized_sources.get("work_techs", [])),
            "cert_techs": json.dumps(prioritized_sources.get("cert_techs", [])),
            "experience_descriptions": json.dumps(prioritized_sources.get("experience_descriptions", [])),
            "fallback_texts": json.dumps(prioritized_sources.get("fallback_texts", [])),
        }]
    )
    return "success"








# model_sentence_transformer = SentenceTransformer('all-MiniLM-L6-v2')
# import os
# import google.generativeai as genai
#
# api_key = os.getenv("GOOGLE_API_KEY")
# if not api_key:
#     raise ValueError("GOOGLE_API_KEY environment variable not set")
# genai.configure(api_key=api_key)
# model_genai = genai.GenerativeModel('gemini-1.5-pro')
#
# chroma_client = chromadb.PersistentClient(path="./chroma_data")
# cv_collection_concat = chroma_client.get_or_create_collection(name="cv_embeddings_concatenated")
# jd_collection_industry_keyw = chroma_client.get_or_create_collection(name="jd_industry_keywords")
#
#
# # domain = "banking"
# # industry_keywords = extract_industry_keywords(model_genai, domain)
# #
# # try:
# #     embedding = model_sentence_transformer.encode(domain, convert_to_tensor=True)
# # except Exception as e:
# #     print(f"Embedding model error: {str(e)}")
# #
# # jd_collection_industry_keyw.add(
# #     ids=[f"{1030}"],
# #     embeddings=[embedding.tolist()],
# #     documents=[json.dumps(industry_keywords)]
# # )
#
# job_id = 1030
# # json.loads(jd_collection_industry_keyw.get(ids=[f"{job_id}"], include=["documents"])["documents"])
# a = json.loads(jd_collection_industry_keyw.get(ids=[f"{job_id}"], include=["documents"])["documents"][0])
# print(a)
# print(type(a))