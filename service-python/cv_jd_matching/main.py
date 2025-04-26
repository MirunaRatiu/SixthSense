from typing import Optional, Dict, List
import asyncio

from fastapi import FastAPI, HTTPException, Body
from pydantic import BaseModel
import chromadb
from sentence_transformers import SentenceTransformer
import numpy as np

from cv_parse import transform_dto_to_cv
from jd_parse import transform_dto_to_jd
from match import get_match_score

app = FastAPI()

model = SentenceTransformer('all-MiniLM-L6-v2')

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

class MatchRequest(BaseModel):
    cv: CvDTO
    jd: JobDescriptionDTO
    job_skills: Dict[str, int]  # e.g., {"Python": 30, "TensorFlow": 20}
    industry_keywords: List[str]  # e.g., ["machine learning", "AI"]

class MatchResponse(BaseModel):
    score: float
    explanation: Dict[str, str]

# function takes a CvDTO, JobDescriptionDTO, job_skillsDTO, industry_keywordsDTO

@app.post("/match/aux", response_model=MatchResponse)
async def match_cv_to_jd(request: MatchRequest):
    async with condition:
        # Wait until all embedding operations are done
        await condition.wait_for(lambda: embedding_counter == 0)
    try:
        transformed_cv = transform_dto_to_cv(request.cv.model_dump())
        transformed_jd = transform_dto_to_jd(request.jd.model_dump())

        score, explanation = get_match_score(
            model,
            transformed_cv,
            transformed_jd,
            request.job_skills,
            request.industry_keywords
        )
        return MatchResponse(score=score, explanation=explanation)

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))



