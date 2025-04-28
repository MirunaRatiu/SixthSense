from typing import Dict, Optional
from collections import defaultdict
import numpy as np
from fastapi import HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
from sklearn.metrics.pairwise import cosine_similarity
import chromadb
print("0")
# === Setup ChromaDB & Model ===
chroma_client = chromadb.PersistentClient(path="../chroma_data")
print("1")
cv_collection = chroma_client.get_or_create_collection(name="cv_embeddings")
print("2")
jd_collection = chroma_client.get_or_create_collection(name="jd_embeddings")
print("3")
embedding_model = SentenceTransformer('all-MiniLM-L6-v2')
print("4")

# === Data Models ===
class CvDTO(BaseModel):
    id: int
    technicalSkills: Optional[str] = None
    foreignLanguages: Optional[str] = None
    education: Optional[str] = None
    certifications: Optional[str] = None
    projectExperience: Optional[str] = None
    workExperience: Optional[str] = None
    others: Optional[str] = None

class JobDescriptionDTO(BaseModel):
    id: int
    jobTitle: Optional[str] = None
    companyOverview: Optional[str] = None
    keyResponsibilities: Optional[str] = None
    requiredQualifications: Optional[str] = None
    preferredSkills: Optional[str] = None

class MatchRequest(BaseModel):
    jd_id: int
    company_domain: str
    predefined_skills: Dict[str, int]

class MatchResponse(BaseModel):
    matches: Dict[str, float]

# === Embedding CVs ===
def embed_sections_cv(request: CvDTO):
    deleted = cv_collection.delete(where={"id": request.id})
    print(f"Deleted previous CV entries for ID {request.id}: {deleted}")

    sections = {
        "technicalSkills": request.technicalSkills,
        "foreignLanguages": request.foreignLanguages,
        "education": request.education,
        "certifications": request.certifications,
        "projectExperience": request.projectExperience,
        "workExperience": request.workExperience,
        "others": request.others,
    }
    valid_sections = {k: v for k, v in sections.items() if v}
    texts = list(valid_sections.values())
    section_names = list(valid_sections.keys())
    embeddings = embedding_model.encode(texts)

    for section_name, embedding, text in zip(section_names, embeddings, texts):
        cv_collection.add(
            ids=[f"{request.id}_{section_name}"],
            embeddings=[embedding.tolist()],
            documents=[text],
            metadatas=[{
                "id": request.id,
                "section_name": section_name
            }]
        )
    return "success"

# === Embedding JDs ===
def embed_sections_jd(request: JobDescriptionDTO):
    deleted = jd_collection.delete(where={"id": request.id})
    print(f"Deleted previous JD entries for ID {request.id}: {deleted}")

    sections = {
        "jobTitle": request.jobTitle,
        "companyOverview": request.companyOverview,
        "keyResponsibilities": request.keyResponsibilities,
        "requiredQualifications": request.requiredQualifications,
        "preferredSkills": request.preferredSkills,
    }
    valid_sections = {k: v for k, v in sections.items() if v}
    texts = list(valid_sections.values())
    section_names = list(valid_sections.keys())
    embeddings = embedding_model.encode(texts)

    for section_name, embedding, text in zip(section_names, embeddings, texts):
        jd_collection.add(
            ids=[f"{request.id}_{section_name}"],
            embeddings=[embedding.tolist()],
            documents=[text],
            metadatas=[{
                "id": request.id,
                "section_name": section_name
            }]
        )
    return "success"

# === Scoring Utilities ===
def get_industry_match_score(text: str, domain: str) -> float:
    text = text.lower()
    domain = domain.lower()
    if domain in text:
        count = text.count(domain)
        if count > 2:
            return 100.0
        elif count == 1:
            return 50.0
        else:
            return 20.0
    return 0.0

def get_weighted_skill_score(text: str, skills_weights: Dict[str, float]) -> float:
    text = text.lower()
    total_score = 0.0
    for skill, weight in skills_weights.items():
        if skill.lower() in text:
            total_score += weight
    return min(total_score, 100.0)

# === Matching Logic ===
def match_cv_to_jd(request: MatchRequest):
    jd_results = jd_collection.get(include=["embeddings"], where={"id": request.jd_id})
    if not jd_results.get("ids"):
        raise HTTPException(status_code=404, detail="Job Description not found.")
    jd_embeddings = jd_results.get("embeddings", [])

    cv_results = cv_collection.get(include=["embeddings", "documents", "metadatas"])
    cv_embeddings = cv_results.get("embeddings", [])    # vectorizarile
    cv_documents = cv_results.get("documents", []) # textele care au fost vectorizate
    cv_metadatas = cv_results.get("metadatas", []) # metadatele (id, section_name)

    # map in care am id + toate embeddingurile lui (asta pentru toate idurile existente)
    cv_embeddings_map = defaultdict(list)
    # map in care am id + toate embeddingurile lui + textul original din care s-a facut embeddingul
    cv_metadata_map = defaultdict(lambda: defaultdict(str))

    for i, (meta, text, embedding) in enumerate(zip(cv_metadatas, cv_documents, cv_embeddings)):
        if embedding is None:
            print(f"[WARN] Skipping null embedding at index {i} for CV ID {meta.get('id')} - section {meta.get('section_name')}")
            continue

        cv_id = str(meta["id"])
        section = meta["section_name"]
        cv_embeddings_map[cv_id].append(np.array(embedding))
        cv_metadata_map[cv_id][section] = text

    matches = {}
    for cv_id, embeddings in cv_embeddings_map.items():
        section_texts = cv_metadata_map[cv_id]
        industry_text = section_texts.get("projectExperience", "") + " " + section_texts.get("workExperience", "")
        skill_text = section_texts.get("technicalSkills", "")

        industry_score = get_industry_match_score(industry_text, request.company_domain) # functie pentru aia 10% din scorul final
        skill_score = get_weighted_skill_score(skill_text, request.predefined_skills) # functie pentru aia 30% din scorul final
        sim = cosine_similarity(np.array(embeddings), jd_embeddings)
        semantic_score = np.max(sim) * 100

        #formula finala
        total_score = 0.10 * industry_score + 0.30 * skill_score + 0.60 * semantic_score
        matches[cv_id] = round(total_score, 2)

    top_matches = dict(sorted(matches.items(), key=lambda x: x[1], reverse=True)[:5])
    return {"matches": top_matches}

# === Test Function ===
def run_cv_jd_matching():
    print("\n==== Running CV-JD Matching Test ====\n")
    # modifica aici pentru ce jd sa faci matching
    match_request = MatchRequest(
        jd_id=1002,
        company_domain="tech",
        predefined_skills={"JavaScript": 40, "React": 40, "Figma": 20}
    )
    print("Matching CVs to JD...")
    result = match_cv_to_jd(match_request)

    print("\n===== MATCHING RESULTS =====")
    for cv_id, score in result["matches"].items():
        print(f"CV {cv_id} → Score: {score}")
    print("\n==== Match Completed ====\n")


def show_all_cvs():
    print("===== CV COLLECTION =====")
    try:
        # Explicitly request embeddings and metadata
        cv_results = cv_collection.get(include=["embeddings", "metadatas"])

        ids = cv_results.get("ids", [])
        metadatas = cv_results.get("metadatas", [])
        embeddings = cv_results.get("embeddings", [])

        print(f"Total CV Entries: {len(ids)}")

        for i in range(min(13, len(ids))):
            entry_id = ids[i]
            metadata = metadatas[i] if i < len(metadatas) else None
            embedding = embeddings[i] if i < len(embeddings) else None

            print(f"ID: {entry_id}")
            print(f"Metadata: {metadata}")
            if embedding is not None and len(embedding) > 0:
                print(f"Embedding Sample (first 13 values): {embedding[:13]}")
            else:
                print("Embedding: None")
            print("-" * 40)

    except Exception as e:
        print(f"Error fetching CV collection: {e}")

def delete_cv_by_id_simple(id: int):
    deleted = cv_collection.delete(where={"id": id})
    print(f"Deleted previous CV entries for ID {id}: {deleted}")

def delete_jd_by_id_simple(id: int):
    deleted = jd_collection.delete(where={"id": id})
    print(f"Deleted previous JD entries for ID {id}: {deleted}")


# jd_example2 = JobDescriptionDTO(
#     id=1002,
#     jobTitle="Machine Learning Engineer Frontend Developer",
#     companyOverview="Innovative Tech Solutions Inc. is a leading technology company dedicated to transforming the digital landscape through cutting-edge solutions. We specialize in developing advanced software and applications that empower businesses to achieve their goals. Our team is composed of passionate professionals who are committed to driving innovation and delivering exceptional results for our clients.",
#     keyResponsibilities="Collaborate with cross-functional teams to design and implement intuitive and responsive user interfaces for machine learning applications. Develop and maintain frontend architecture to support seamless integration with backend services and machine learning models. Optimize web applications for maximum speed and scalability, ensuring a smooth user experience. Translate UI/UX design wireframes into high-quality code using HTML, CSS, and JavaScript frameworks. Implement data visualization techniques to effectively communicate complex machine learning insights to users. Conduct code reviews and provide constructive feedback to team members to maintain high coding standards. Stay updated with the latest frontend technologies and industry trends to continuously improve application performance and user experience.",
#     requiredQualifications="Bachelor’s degree in Computer Science, Software Engineering, or a related field. Proven experience as a Frontend Developer, with a strong portfolio showcasing web applications. Proficiency in HTML, CSS, JavaScript, and modern frontend frameworks such as React, Angular, or Vue.js. Familiarity with machine learning concepts and the ability to integrate machine learning models into frontend applications. Experience with version control systems like Git. Strong problem-solving skills and attention to detail. Excellent communication and teamwork abilities.",
#     preferredSkills="Experience with data visualization libraries such as D3.js or Chart.js. Understanding of RESTful APIs and asynchronous request handling. Knowledge of web performance optimization techniques. Familiarity with cloud platforms and services (e.g., AWS, Azure, Google Cloud). Experience with Agile development methodologies"
# )
#
# cv_example3 = CvDTO( # Elena Radu Marin
#     id=103,
#     technicalSkills="JavaScript, ReactJS, TypeScript, HTML, CSS, AngularJS, Bootstrap, Git, REST APIs, VueJS, TypeScript, HTML, CSS, Git, ReactJS, JavaScript, Figma, Adobe XD",
#     foreignLanguages="English, Spanish",
#     education="University Name: University Politehnica of Bucharest(Program Duration: 4 years), Master degree Name: University Politehnica of Bucharest(Program Duration: 2 years)",
#     certifications="AWS Certified Developer – Associate, Microsoft Certified: Azure Developer Associate, Google Professional Cloud Developer",
#     projectExperience="Interactive Dashboard for Data Visualization Developed an interactive data visualization dashboard using ReactJS and TypeScript to provide real-time analytics for business users. Implemented REST APIs to fetch and display data dynamically, ensuring seamless user interaction and data accuracy. Utilized Bootstrap for responsive design and Git for version control, enabling efficient collaboration and deployment. Technologies and tools used: ReactJS, TypeScript, REST APIs, Bootstrap, Git, Cloud-Based E-commerce Platform Led the development of a cloud-based e-commerce platform leveraging AngularJS for the frontend and integrated with AWS services for backend infrastructure. Implemented secure payment processing and user authentication, enhancing the platform's reliability and user trust. Optimized the application for scalability and performance using best practices in cloud computing. Technologies and tools used: AngularJS, AWS (EC2, S3), REST APIs, Bootstrap, Git.",
#     others=""
# )
# cv_example4 = CvDTO( # Sorin Mihai Bădescu
#     id=104,
#     technicalSkills="Python, TensorFlow: 4, JavaScript, ReactJS: 3, AWS SageMaker, Docker: 2, SQL, PostgreSQL: 3, Figma, Adobe XD: 2",
#     foreignLanguages="English, Spanish",
#     education="University Name: University Politehnica of Bucharest(Program Duration: 4 years)",
#     certifications="AWS Certified Cloud Practitioner, Microsoft Certified: Azure AI Fundamentals",
#     projectExperience="Machine Learning Model for Customer Churn Prediction Developed a machine learning model using Python and TensorFlow to predict customer churn for a telecommunications company. The project involved data preprocessing, feature engineering, and model training, resulting in a 15% improvement in prediction accuracy. Deployed the model on AWS SageMaker, leveraging Docker for containerization to ensure a scalable and efficient deployment process. Technologies and tools used: Python, TensorFlow, AWS SageMaker, Docker., Interactive Data Visualization Dashboard Created an interactive data visualization dashboard using ReactJS and PostgreSQL to provide real-time insights into sales data for a retail business. The project included designing a user-friendly interface and implementing dynamic data fetching and filtering capabilities. Collaborated with stakeholders to refine dashboard features, enhancing decision-making processes., Technologies and tools used: JavaScript, ReactJS, SQL, PostgreSQL, Figma., Cloud-based AI Chatbot  Developed a cloud-based AI chatbot using Python and Azure AI services to automate customer support for an e-commerce platform. The chatbot was integrated into the company's website, handling common customer queries and reducing response time by 50%. Utilized Docker for containerization and ensured seamless deployment and scaling on AWS infrastructure. Technologies and tools used: Python, Azure AI, Docker, AWS.",
#     others=""
# )
#
# cv_example5 = CvDTO(  # Florin Neagu
#     id=105,
#     technicalSkills="Python, TensorFlow: 4, JavaScript, ReactJS: 3, AWS SageMaker, Docker: 2, SQL, PostgreSQL: 3, Figma, Adobe XD: 2",
#     foreignLanguages="English, Spanish",
#     education="University Name: University Politehnica of Bucharest(Program Duration: 4 years)",
#     certifications="AWS Certified Machine Learning – Specialty, TensorFlow Developer Certificate",
#     projectExperience="Predictive Analytics Platform for Retail Led the development of a predictive analytics platform using Python and TensorFlow to forecast retail sales trends. Implemented machine learning models that improved sales prediction accuracy by 25%, enabling better inventory management and reducing waste. Utilized AWS SageMaker for model training and deployment, ensuring scalability and efficient resource management. Technologies and tools used: Python, TensorFlow, AWS SageMaker, Docker., Interactive Web Application for Real-time Data Visualization Developed an interactive web application using ReactJS and JavaScript to visualize real-time data from IoT devices. The application provided users with dynamic dashboards and insights, enhancing decision-making processes for industrial clients. Integrated PostgreSQL for efficient data storage and retrieval, ensuring quick access to historical data. Technologies and tools used: JavaScript, ReactJS, SQL, PostgreSQL., User-centric Mobile App Design for Fitness Tracking Designed a mobile application interface focused on fitness tracking, employing Figma and Adobe XD to create a seamless user experience. Conducted extensive user testing sessions to refine the design, resulting in a 40% increase in user engagement. Collaborated closely with developers to ensure the design was effectively translated into a functional application. Technologies and tools used: Figma, Adobe XD, React Native.",
#     others=""
# )
#
# cv_example6 = CvDTO(  # Raluca Mihaiță Dumitrescu
#     id=106,
#     technicalSkills="Python, TensorFlow, JavaScript, ReactJS, AWS SageMaker, Docker, SQL, PostgreSQL, Figma, Adobe XD",
#     foreignLanguages="English, Spanish",
#     education="University Name: University Politehnica of Bucharest(Program Duration: 4 years)",
#     certifications="AWS Certified Machine Learning – Specialty, Docker Certified Associate, TensorFlow Developer Certificate",
#     projectExperience="Machine Learning Model Deployment on AWS SageMaker Developed and deployed a scalable machine learning model using Python and TensorFlow on AWS SageMaker. Utilized Docker to containerize the application, ensuring seamless integration and deployment across different environments. Implemented a robust data pipeline with PostgreSQL for data storage and retrieval, enhancing model accuracy by 20%. Technologies and tools used: Python, TensorFlow, AWS SageMaker, Docker, PostgreSQL., Interactive Web Application Development Created an interactive web application using JavaScript and ReactJS, focusing on delivering a dynamic user experience. Designed the user interface with Figma and Adobe XD, incorporating user feedback to refine the design iteratively. Integrated SQL databases to manage user data efficiently, resulting in a 35% improvement in data retrieval times. Technologies and tools used: JavaScript, ReactJS, SQL, Figma, Adobe XD.",
#     others=""
# )
#
# cv_example7 = CvDTO(  # Cătălin Mihai Dumitrescu
#     id=107,
#     technicalSkills="JavaScript, ReactJS, TypeScript, HTML, CSS, Bootstrap, AngularJS, VueJS, REST APIs, Git",
#     foreignLanguages="English, Spanish",
#     education="University Name: University Politehnica of Bucharest(Program Duration: 4 years)",
#     certifications="Microsoft Certified: Azure Developer Associate, AWS Certified Developer – Associate, Google Professional Cloud Developer",
#     projectExperience="Interactive Dashboard for Data Visualization Developed an interactive data visualization dashboard using ReactJS and TypeScript, aimed at providing real-time analytics for business intelligence. The project involved integrating REST APIs to fetch dynamic data and utilizing Bootstrap for responsive design. Implemented state management with Redux to ensure efficient data handling and user interface updates. Technologies and tools used: ReactJS, TypeScript, Redux, Bootstrap, REST APIs, Git., Progressive Web Application for Task Management Created a progressive web application (PWA) for task management using AngularJS and VueJS, focusing on enhancing user productivity through intuitive design and offline capabilities. Leveraged REST APIs for seamless data synchronization across devices and employed Git for version control. The application was designed to be mobile-first, utilizing HTML and CSS for a clean and responsive user interface. Technologies and tools used: AngularJS, VueJS, REST APIs, HTML, CSS, Git.",
#     others=""
# )
#
#
# cv_example8 = CvDTO(  # Andrei Mihailescu
#     id=108,
#     technicalSkills="JavaScript, ReactJS, Node.js, SQL, HTML, CSS, Bootstrap, AngularJS, Python, Django, PostgreSQL, REST APIs, TypeScript, VueJS, AWS, Docker, Java, Spring Boot, OracleSQL, Kubernetes",
#     foreignLanguages="English, Spanish, French",
#     education="University Name: Politehnica University of Bucharest(Program Duration: 4 years)",
#     certifications="AWS Certified Solutions Architect – Associate, Certified Kubernetes Administrator (CKA), Oracle Certified Professional, Java SE 11 Developer",
#     projectExperience="Inventory Management System Developed a robust inventory management system using Java and Spring Boot for the backend, with an OracleSQL database to handle complex queries and data storage. Implemented REST APIs to facilitate seamless communication between the backend and a responsive frontend built with AngularJS and Bootstrap. Deployed the application on a Kubernetes cluster, ensuring scalability and high availability. Technologies and tools used: Java, Spring Boot, OracleSQL, AngularJS, Bootstrap, Kubernetes., Real-time Data Analytics Platform Created a real-time data analytics platform leveraging Python and Django for the backend, with PostgreSQL as the database to manage large datasets efficiently. Utilized ReactJS and TypeScript to build a dynamic and interactive user interface. Integrated AWS services for cloud storage and Docker for containerization, enabling smooth deployment and scalability. Technologies and tools used: Python, Django, PostgreSQL, ReactJS, TypeScript, AWS, Docker.",
#     others=""
# )
#
# cv_example9 = CvDTO(  # Adrian Mihai Radu
#     id=109,
#     technicalSkills="Java, Spring Boot, Python, Django, SQL, PostgreSQL, Docker, Kubernetes",
#     foreignLanguages="English, Spanish",
#     education="University Name: University Politehnica of Bucharest(Program Duration: 4 years)",
#     certifications="Oracle Certified Associate, Java SE 8 Programmer, Docker Certified Associate, Certified Kubernetes Administrator",
#     projectExperience="Inventory Management System Developed an inventory management system using Java and Spring Boot to streamline stock tracking and order processing for small businesses. Implemented RESTful APIs to facilitate seamless integration with third-party applications and enhance data accessibility. Utilized PostgreSQL for efficient data storage and retrieval, ensuring robust performance and reliability. Deployed the application using Docker containers, enabling consistent environments across development and production stages. Technologies and tools used: Java, Spring Boot, PostgreSQL, Docker., Online Learning Platform Created an online learning platform using Python and Django, providing users with access to a wide range of educational courses and resources. Designed a user-friendly interface and implemented authentication features to ensure secure access to course materials. Managed course data and user profiles using PostgreSQL, optimizing queries for faster data retrieval. Utilized Kubernetes for orchestrating containerized applications, ensuring scalability and high availability. Technologies and tools used: Python, Django, PostgreSQL, Kubernetes.",
#     others=""
# )



# function to see all the cvs from the db (there is one for jd)
#show_all_cvs()

run_cv_jd_matching()

# add a jd to the data base
# embed_sections_jd(jd_example2)

# add a cv to the data base
# embed_sections_cv(cv_example3)


