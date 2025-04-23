from fastapi.testclient import TestClient
import json
from main import app, cv_collection  # Make sure this matches your filename

client = TestClient(app)

def test_embed_cv():
    payload = {
        "id": 99,
        "technicalSkills": "Python, FastAPI",
        "foreignLanguages": "English, Spanish",
        "education": "MSc in Computer Science",
        "certifications": "AWS Certified",
        "projectExperience": "Built microservices",
        "workExperience": "5 years in tech",
        "others": "Hackathon winner"
    }

    response = client.post("/embed/cv", json=payload)
    assert response.status_code == 200
    assert response.json() == "success"

def test_embed_jd():
    payload = {
        "id": 42,
        "jobTitle": "AI Engineer",
        "companyOverview": "Tech startup",
        "keyResponsibilities": "Build ML pipelines",
        "requiredQualifications": "CS degree",
        "preferredSkills": "FastAPI, PyTorch"
    }

    response = client.post("/embed/jd", json=payload)
    assert response.status_code == 200
    assert response.json() == "success"


def test_delete_cv_embeddings():
    # Step 1: Embed a CV with id 99
    payload = {
        "id": 99,
        "technicalSkills": "Python, FastAPI",
        "foreignLanguages": "English, Spanish",
        "education": "MSc in Computer Science",
        "certifications": "AWS Certified",
        "projectExperience": "Built microservices",
        "workExperience": "5 years in tech",
        "others": "Hackathon winner"
    }

    embed_response = client.post("/embed/cv", json=payload)
    assert embed_response.status_code == 200
    assert embed_response.json() == "success"

    # Step 2: Delete the CV embeddings with raw JSON number
    delete_response = client.request(
        method="DELETE",
        url="/delete/cv",
        data="99",  # Raw integer as a string (valid JSON)
        headers={"Content-Type": "application/json"}
    )

    assert delete_response.status_code == 200, f"Unexpected error: {delete_response.text}"
    response_data = delete_response.json()
    assert response_data["status"] == "success"
    assert all(str(i).startswith("99_") for i in response_data["deleted_ids"])

    # Step 3: Confirm deletion
    collection_contents = cv_collection.get(include=["metadatas"])
    remaining_ids = collection_contents["ids"]
    assert not any(id.startswith("99_") for id in remaining_ids)


