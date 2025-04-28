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
        transformed_jd = transform_dto_to_jd(request.jd.model_dump())
        job_id = request.jd.id
        industry_keywords = json.loads(jd_collection_industry_keyw.get(ids=[f"{job_id}"], include=["documents"])["documents"][0])
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

        responses = [MatchResponse(score=score, explanation=explanation, id=id) for score, explanation, id in
                     top_matches]

        return responses

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/match/cv", response_model=List[MatchResponse])
async def matchCv(request: CvMatchRequest):
    async with condition:
        # Wait until all embedding operations are done
        await condition.wait_for(lambda: embedding_counter == 0)

    try:
        matches = []

        for jobDescription in request.jd:
            transformed_jd = transform_dto_to_jd(jobDescription.model_dump())
            job_id = jobDescription.id
            industry_keywords = json.loads(jd_collection_industry_keyw.get(ids=[f"{job_id}"], include=["documents"])["documents"][0])
            score, explanation = get_match_score(
                model_sentence_transformer,
                cv_collection_concat,
                request.cv,
                transformed_jd,
                {},
                industry_keywords
            )
            matches.append((score, explanation, transformed_jd["id"]))

        matches.sort(key=lambda x: x[0], reverse=True)

        top_matches = matches[:20]

        responses = [MatchResponse(score=score, explanation=explanation, id=jid) for score, explanation, jid in top_matches]

        return responses

    except Exception as e:
        raise HTTPException(status_code=500,detail=str(e))


@app.post("/embed/cv")
async def embed_cv(request: CvDTO):
    embed_sections_cv(request, model_sentence_transformer, cv_collection_concat)
    return "success"

@app.post("/embed/jd")
async def embed_cv(request: JobDescriptionDTO):
    domain = request["message"]
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
        # Get all entries in the collection
        results = collection.get(include=["metadatas"])

        # Filter IDs where metadata.id == item_id
        ids_to_delete = [
            entry_id for entry_id, metadata in zip(results["ids"], results["metadatas"])
            if metadata.get("id") == item_id
        ]

        if not ids_to_delete:
            raise HTTPException(status_code=404, detail=f"No entries found for ID {item_id}")

        # Delete the matching IDs
        collection.delete(ids=ids_to_delete)

        return "Success"

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))



if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="127.0.0.1", port=8081)

