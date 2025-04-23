from typing import Optional
import asyncio

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import chromadb
from sentence_transformers import SentenceTransformer
import numpy as np

app = FastAPI()

embedding_counter = 0
lock = asyncio.Lock()
condition = asyncio.Condition()


# Initialize Chroma DB client
chroma_client = chromadb.PersistentClient(path="./chroma_data")
cv_collection = chroma_client.get_or_create_collection(name="cv_embeddings")
jd_collection = chroma_client.get_or_create_collection(name="jd_embeddings")

# Load Hugging Face model for embeddings
embedding_model = SentenceTransformer('all-MiniLM-L6-v2')

# Pydantic models for request/response validation
# class EmbedRequest(BaseModel):
#     sections: list[str]  # List of CV or JD sections to embed
#     metadata: dict       # Metadata (e.g., cv_id, jd_id)
#
# class MatchRequest(BaseModel):
#     jd_id: str           # Job Description ID
#     predefined_skills: dict  # Predefined technical skills and their weights
#
# class MatchResponse(BaseModel):
#     matches: dict  # Dictionary of CV IDs and their final scores

class CvDTO(BaseModel):
    id: int
    technicalSkills: Optional[str]
    foreignLanguages: Optional[str]
    education: Optional[str]
    certifications: Optional[str]
    projectExperience: Optional[str]
    workExperience: Optional[str]
    others: Optional[str]

class JobDescriptionDTO(BaseModel):
    id: int
    jobTitle: Optional[str]
    companyOverview: Optional[str]
    keyResponsibilities: Optional[str]
    requiredQualifications: Optional[str]
    preferredSkills: Optional[str]

@app.post("/embed/cv")
async def embed_sections_cv(request: CvDTO):
    global embedding_counter
    async with condition:
        embedding_counter += 1

    try:
        sections = {
            "technicalSkills": request.technicalSkills,
            "foreignLanguages": request.foreignLanguages,
            "education": request.education,
            "certifications": request.certifications,
            "projectExperience": request.projectExperience,
            "workExperience": request.workExperience,
            "others": request.others,
        }
        # Filter out None or empty sections
        valid_sections = {k: v for k, v in sections.items() if v}

        # Generate embeddings
        texts = list(valid_sections.values())
        section_names = list(valid_sections.keys())
        embeddings = embedding_model.encode(texts)

        # Store embeddings in the appropriate collection
        for i, (section_name, embedding) in enumerate(zip(section_names, embeddings)):
            cv_collection.add(
                ids=[f"{request.id}_{section_name}"],
                embeddings=[embedding.tolist()],
                metadatas=[{
                    "id": request.id,
                    "section_name": section_name
                }]
            )
        return "success"
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        async with condition:
            embedding_counter -= 1
            if embedding_counter == 0:
                condition.notify_all()  # Wake any match waiting

@app.post("/embed/jd")
async def embed_sections_jd(request: JobDescriptionDTO):
    global embedding_counter
    async with condition:
        embedding_counter += 1

    try:
        sections = {
            "jobTitle": request.jobTitle,
            "companyOverview": request.companyOverview,
            "keyResponsibilities": request.keyResponsibilities,
            "requiredQualifications": request.requiredQualifications,
            "preferredSkills": request.preferredSkills,
        }
        # Filter out None or empty sections
        valid_sections = {k: v for k, v in sections.items() if v}

        # Generate embeddings
        texts = list(valid_sections.values())
        section_names = list(valid_sections.keys())
        embeddings = embedding_model.encode(texts)

        # Store embeddings in the appropriate collection
        for i, (section_name, embedding) in enumerate(zip(section_names, embeddings)):
            jd_collection.add(
                ids=[f"{request.id}_{section_name}"],
                embeddings=[embedding.tolist()],
                metadatas=[{
                    "id": request.id,
                    "section_name": section_name
                }]
            )
        return "success"
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        async with condition:
            embedding_counter -= 1
            if embedding_counter == 0:
                condition.notify_all()  # Wake any match waiting

# @app.post("/match/jd", response_model=MatchResponse)
# async def match_cv_to_jd(request: MatchRequest):
#     async with condition:
#         # Wait until all embedding operations are done
#         await condition.wait_for(lambda: embedding_counter == 0)
#     try:
#         # Fetch the JD embeddings from the JD collection
#         jd_results = jd_collection.get(where={"id": request.jd_id})
#
#         if not jd_results["ids"]:
#             raise HTTPException(status_code=404, detail="Job Description not found.")
#
#         jd_embeddings = np.array(jd_results["embeddings"])
#
#         # Fetch all CV embeddings from the CV collection
#         cv_results = cv_collection.get()
#         cv_ids = cv_results["ids"]
#         cv_embeddings = np.array(cv_results["embeddings"])
#
#         # Calculate cosine similarity and final scores (same as before)
#         ...
#
#         return {"matches": top_matches}
#
#     except Exception as e:
#         raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="127.0.0.1", port=8081)