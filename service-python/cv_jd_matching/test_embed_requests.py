from fastapi.testclient import TestClient
from main import app  # Make sure this matches your filename

client = TestClient(app)

def test_embed_cv():
    payload = {
        "id": 1,
        "technicalSkills": "Python, FastAPI",
        "foreignLanguages": "English",
        "education": "MSc CS",
        "certifications": "AWS",
        "projectExperience": "API dev",
        "workExperience": "2 years at Meta",
        "others": ""
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


