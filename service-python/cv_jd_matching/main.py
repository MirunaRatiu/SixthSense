from typing import Optional
import asyncio

from fastapi import FastAPI, HTTPException, Body
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
        # Prepare sections
        sections = {
            "technicalSkills": request.technicalSkills,
            "foreignLanguages": request.foreignLanguages,
            "education": request.education,
            "certifications": request.certifications,
            "projectExperience": request.projectExperience,
            "workExperience": request.workExperience,
            "others": request.others,
        }

        # Filter out empty sections
        valid_sections = {k: v for k, v in sections.items() if v}
        texts = list(valid_sections.values())
        section_names = list(valid_sections.keys())

        if not texts:
            raise HTTPException(status_code=400, detail="No valid sections provided for embedding.")

        # Encode texts
        try:
            embeddings = embedding_model.encode(texts)
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Embedding model error: {str(e)}")

        # Check model output type and content
        if embeddings is None or not isinstance(embeddings, (list, np.ndarray)):
            raise HTTPException(status_code=500, detail="Model returned invalid embedding format.")
        if any(e is None for e in embeddings):
            raise HTTPException(status_code=500, detail="One or more embeddings returned None.")

        # Extra validation and logging
        print(f"[DEBUG] Computed {len(embeddings)} embeddings for CV ID {request.id}")
        for i, emb in enumerate(embeddings):
            if not isinstance(emb, (list, np.ndarray)):
                raise HTTPException(status_code=500, detail=f"Embedding at index {i} is not a list or array.")
            if len(emb) == 0:
                raise HTTPException(status_code=500, detail=f"Embedding at index {i} is empty.")
            print(f"[DEBUG] Embedding {i} preview: {emb[:5]}")

        # Store embeddings
        for section_name, embedding in zip(section_names, embeddings):
            if embedding is None:
                continue  # Just in case, but shouldn't happen
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
        raise HTTPException(status_code=500, detail=f"CV embedding failed: {str(e)}")

    finally:
        async with condition:
            embedding_counter -= 1
            if embedding_counter == 0:
                condition.notify_all()

@app.post("/embed/jd")
async def embed_sections_jd(request: JobDescriptionDTO):
    global embedding_counter
    async with condition:
        embedding_counter += 1

    try:
        # Prepare sections
        sections = {
            "jobTitle": request.jobTitle,
            "companyOverview": request.companyOverview,
            "keyResponsibilities": request.keyResponsibilities,
            "requiredQualifications": request.requiredQualifications,
            "preferredSkills": request.preferredSkills,
        }

        # Filter out empty sections
        valid_sections = {k: v for k, v in sections.items() if v}
        texts = list(valid_sections.values())
        section_names = list(valid_sections.keys())

        if not texts:
            raise HTTPException(status_code=400, detail="No valid sections provided for embedding.")

        # Encode texts
        try:
            embeddings = embedding_model.encode(texts)
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Embedding model error: {str(e)}")

        # Check model output type and content
        if embeddings is None or not isinstance(embeddings, (list, np.ndarray)):
            raise HTTPException(status_code=500, detail="Model returned invalid embedding format.")
        if any(e is None for e in embeddings):
            raise HTTPException(status_code=500, detail="One or more embeddings returned None.")

        # Extra validation and logging
        print(f"[DEBUG] Computed {len(embeddings)} embeddings.")
        for i, emb in enumerate(embeddings):
            if not isinstance(emb, (list, np.ndarray)):
                raise HTTPException(status_code=500, detail=f"Embedding at index {i} is not a list or array.")
            if len(emb) == 0:
                raise HTTPException(status_code=500, detail=f"Embedding at index {i} is empty.")
            print(f"[DEBUG] Embedding {i} preview: {emb[:5]}")

        # Store embeddings
        for section_name, embedding in zip(section_names, embeddings):
            if embedding is None:
                continue  # Shouldn't happen, but fail-safe
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
        raise HTTPException(status_code=500, detail=f"JD embedding failed: {str(e)}")

    finally:
        async with condition:
            embedding_counter -= 1
            if embedding_counter == 0:
                condition.notify_all()

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

@app.delete("/delete/{item_type}")
async def delete_by_id(item_type: str, item_id: int = Body(...)):
    """
    Delete all vectors in the specified collection (cv or jd) for the given item_id.
    Waits for all embedding operations to finish before deleting.
    """
    if item_type not in ["cv", "jd"]:
        raise HTTPException(status_code=400, detail="item_type must be 'cv' or 'jd'")

    collection = cv_collection if item_type == "cv" else jd_collection

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

        return {"status": "success", "deleted_ids": ids_to_delete}

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


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


def show_all_jds():
    print("\n===== JD COLLECTION =====")
    try:
        jd_results = jd_collection.get(include=["embeddings", "metadatas"])

        ids = jd_results.get("ids", [])
        metadatas = jd_results.get("metadatas", [])
        embeddings = jd_results.get("embeddings", [])

        print(f"Total JD Entries: {len(ids)}")

        for i in range(min(15, len(ids))):
            entry_id = ids[i]
            metadata = metadatas[i] if i < len(metadatas) else None
            embedding = embeddings[i] if i < len(embeddings) else None

            print(f"ID: {entry_id}")
            print(f"Metadata: {metadata}")
            if embedding is not None and len(embedding) > 0:
                print(f"Embedding Sample (first 5 values): {embedding[:5]}")
            else:
                print("Embedding: None")
            print("-" * 40)

    except Exception as e:
        print(f"Error fetching JD collection: {e}")


#show_all_cvs()
#show_all_jds()

if __name__ == "__main__":
    import uvicorn

    show_all_cvs()
    uvicorn.run(app, host="127.0.0.1", port=8081)