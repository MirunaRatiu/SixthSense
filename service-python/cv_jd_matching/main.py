import json
import os
from typing import Optional, Dict, List
import asyncio
import chromadb
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
import google.generativeai as genai

from parsers.cv_parse import transform_dto_to_cv
from parsers.jd_parse import transform_dto_to_jd
from matching_logic.modified_match import get_match_score
from my_utils.generate_related_words import extract_industry_keywords
from my_utils.embed_cv import embed_sections_cv

from dotenv import load_dotenv
load_dotenv()

chroma_client = chromadb.PersistentClient(path="./chroma_data")
cv_collection_concat = chroma_client.get_or_create_collection(name="cv_embeddings_concatenated")
jd_collection_industry_keyw = chroma_client.get_or_create_collection(name="jd_industry_keywords")

app = FastAPI()

model_sentence_transformer = SentenceTransformer('all-MiniLM-L6-v2')

api_key = os.getenv("GOOGLE_API_KEY")
if not api_key:
    raise ValueError("GOOGLE_API_KEY environment variable not set")
genai.configure(api_key=api_key)
model_genai = genai.GenerativeModel('gemini-1.5-pro')

embedding_counter = 0
lock = asyncio.Lock()
condition = asyncio.Condition()


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
    message: Optional[str]


class MatchRequest(BaseModel):
    cv: CvDTO
    jd: JobDescriptionDTO
    job_skills: Dict[str, int]  # e.g., {"Python": 30, "TensorFlow": 20}


class JobMatchRequest(BaseModel):
    jd: JobDescriptionDTO
    job_skills: Dict[str, int]  # e.g., {"Python": 30, "TensorFlow": 20}


class CvMatchRequest(BaseModel):
    cv: int
    jd: List[JobDescriptionDTO]


class MatchResponse(BaseModel):
    score: float
    explanation: Dict[str, str]
    id: int


# @app.post("/match/aux", response_model=MatchResponse)
# async def match_cv_to_jd(request: MatchRequest):
#     async with condition:
#         # Wait until all embedding operations are done
#         await condition.wait_for(lambda: embedding_counter == 0)
#     try:
#         transformed_cv = transform_dto_to_cv(request.cv.model_dump())
#         transformed_jd = transform_dto_to_jd(request.jd.model_dump())
#         cv_id = request.cv.id
#         job_id = request.jd.id
#         industry_keywords = json.loads(jd_collection_industry_keyw.get(ids=[f"{job_id}"], include=["documents"])["documents"][0])
#         score, explanation = get_match_score(
#             model_sentence_transformer,
#             cv_collection_concat,
#             int(cv_id),
#             transformed_jd,
#             request.job_skills,
#             industry_keywords
#         )
#         return MatchResponse(score=score, explanation=explanation)
#
#     except Exception as e:
#         raise HTTPException(status_code=500, detail=str(e))


@app.post("/match/jd", response_model=List[MatchResponse])
async def matchJd(request: JobMatchRequest):
    async with condition:
        # Wait until all embedding operations are done
        await condition.wait_for(lambda: embedding_counter == 0)

    try:
        # Check if the job description exists in the database
        job_id = request.jd.id
        jd_document = jd_collection_industry_keyw.get(ids=[f"{job_id}"], include=["documents"])

        # If the job description is not found, return a 404 with a custom message
        if not jd_document.get("documents"):
            raise HTTPException(status_code=404, detail=f"Job description with ID {job_id} not found in the database.")

        print(job_id)
        # Proceed with the matching logic if the job description exists
        transformed_jd = transform_dto_to_jd(request.jd.model_dump())
        industry_keywords = json.loads(jd_document["documents"][0])
        results = cv_collection_concat.get()

        all_ids = results["ids"]

        matches = []

        for id in all_ids:
            score, explanation = get_match_score(
                model_sentence_transformer,
                cv_collection_concat,
                int(id),
                transformed_jd,
                request.job_skills,
                industry_keywords
            )
            matches.append((score, explanation, id))

        matches.sort(key=lambda x: x[0], reverse=True)

        top_matches = matches[:20]

        responses = [MatchResponse(score=score, explanation=explanation, id=id) for score, explanation, id in top_matches]

        return responses

    except HTTPException as e:
        # Handle specific HTTP exceptions (like 404)
        raise e
    except Exception as e:
        # Handle any other unexpected exceptions and return a 500 error
        raise HTTPException(status_code=500, detail=f"An unexpected error occurred: {str(e)}")


@app.post("/match/cv", response_model=List[MatchResponse])
async def matchCv(request: CvMatchRequest):
    async with condition:
        # Wait until all embedding operations are done
        await condition.wait_for(lambda: embedding_counter == 0)

    try:
        matches = []
        cv_document = cv_collection_concat.get(ids=[f"{request.cv}"], include=["documents"])
        if not cv_document.get("documents"):
            raise HTTPException(status_code=404, detail=f"CV with ID {request.cv} not found in the database.")

        for jobDescription in request.jd:
            job_id = jobDescription.id
            # Check if the job description ID exists in the database
            jd_document = jd_collection_industry_keyw.get(ids=[f"{job_id}"], include=["documents"])

            # If the job description is not found, return a 404 or a custom message
            if not jd_document.get("documents"):
                raise HTTPException(status_code=404, detail=f"Job description with ID {job_id} not found in the database.")


            # Continue with the matching process if the job description exists
            transformed_jd = transform_dto_to_jd(jobDescription.model_dump())
            industry_keywords = json.loads(jd_document["documents"][0])
            score, explanation = get_match_score(
                model_sentence_transformer,
                cv_collection_concat,
                request.cv,
                transformed_jd,
                {},
                industry_keywords
            )
            matches.append((score, explanation, jobDescription.id))

        matches.sort(key=lambda x: x[0], reverse=True)

        top_matches = matches[:20]

        responses = [MatchResponse(score=score, explanation=explanation, id=jid) for score, explanation, jid in top_matches]

        return responses

    except HTTPException as e:
        # Handle specific HTTP exceptions if needed
        raise e
    except Exception as e:
        # Handle other unexpected exceptions
        raise HTTPException(status_code=500, detail=f"An error occurred: {str(e)}")



@app.post("/embed/cv")
async def embed_cv(request: CvDTO):
    embed_sections_cv(request, model_sentence_transformer, cv_collection_concat)
    return "success"

@app.post("/embed/jd")
async def embed_jd(request: JobDescriptionDTO):
    domain = request.message
    industry_keywords = extract_industry_keywords(model_genai, domain)

    try:
        embedding = model_sentence_transformer.encode(domain, convert_to_tensor=True)
    except Exception as e:
        print(f"Embedding model error: {str(e)}")
        return "Failed"
    jd_collection_industry_keyw.add(
        ids=[f"{request.id}"],
        embeddings=[embedding.tolist()],
        documents=[json.dumps(industry_keywords)]
    )
    return "success"

@app.delete("/delete/{item_type}/{item_id}")
async def delete_by_id(item_type: str, item_id: int):
    """
        Delete all vectors in the specified collection (cv or jd) for the given item_id.
        Waits for all embedding operations to finish before deleting.
        """
    if item_type not in ["cv", "jd"]:
        raise HTTPException(status_code=400, detail="item_type must be 'cv' or 'jd'")

    collection = cv_collection_concat if item_type == "cv" else jd_collection_industry_keyw

    # Wait until all embeddings are finished
    async with condition:
        await condition.wait_for(lambda: embedding_counter == 0)

    try:

        collection.delete(ids=[f"{item_id}"])

        return "Success"

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))



if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="127.0.0.1", port=8081)