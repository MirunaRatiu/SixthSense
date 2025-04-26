from sentence_transformers import SentenceTransformer, util
from rapidfuzz import fuzz
import re

# Load embedding model in main for performance
# model = SentenceTransformer('all-MiniLM-L6-v2')

# ---------- Config ----------
CONFIG = {
    "weights": {
        "industry": 0.1,
        "technical": 0.3,
        "semantic": 0.6,
        "cert_bonus_cap": 5.0
    },
    "thresholds": {
        "fuzzy_skill_match": 90,
        "fuzzy_industry_match": 80,
        "cert_match": 80
    },
    "semantic_scaling": {
        "low": 20,
        "medium": 30,
        "low_scale": 0.1,
        "medium_scale": 0.4
    }
}

# ---------- Normalization ----------
def normalize(text: str) -> str:
    return re.sub(r'[^\w\s+#.-]', '', text.lower()).strip()

# ---------- CV Text Extractor ----------
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

# ---------- Fuzzy Matching ----------
def fuzzy_match(s1: str, s2: str, threshold: int = None) -> bool:
    threshold = threshold or CONFIG["thresholds"]["fuzzy_skill_match"]
    s1_clean = normalize(s1)
    s2_clean = normalize(s2)
    score = fuzz.partial_ratio(s1_clean, s2_clean)
    print(f"[Fuzzy] '{s1_clean}' vs '{s2_clean}' → Score: {score}")
    return score >= threshold

# ---------- Industry Score ----------
def calculate_industry_score(cv_text: str, industry_keywords: list[str], threshold: int = None) -> float:
    threshold = threshold or CONFIG["thresholds"]["fuzzy_industry_match"]
    lines = [line.strip() for line in cv_text.lower().splitlines() if line.strip()]
    ignored_tokens = ['technologies used', 'project tech', 'field of study', 'education']
    matches = []
    for kw in industry_keywords:
        for line in lines:
            if any(token in line for token in ignored_tokens):
                continue
            score = fuzz.partial_ratio(normalize(kw), normalize(line))
            print(f"[Industry] '{kw}' vs '{line}' → Score: {score}")
            if score >= threshold:
                matches.append(kw)
                break
    return min(100.0, len(matches) * 50) if matches else 0.0

# ---------- Technical Skill Score ----------
def calculate_technical_score(cv_skills: list[str], job_skills: dict[str, int]) -> float:
    total_weight = sum(job_skills.values())
    matched_weight = 0
    for job_skill, weight in job_skills.items():
        matched = False
        for cv_skill in cv_skills:
            if fuzzy_match(job_skill, cv_skill):
                print(f"[Tech Match] '{job_skill}' matched with '{cv_skill}'")
                matched_weight += weight
                matched = True
                break
        if not matched:
            print(f"[Tech Miss] '{job_skill}' not found in CV skills")
    return (matched_weight / total_weight) * 100 if total_weight else 0.0

# ---------- Certification Bonus ----------
def calculate_certification_bonus(cv: dict, job_skills: dict[str, int]) -> float:
    threshold = CONFIG["thresholds"]["cert_match"]
    cert_names = [cert["name"] for cert in cv.get("certifications", [])]
    matched = 0
    for skill in job_skills:
        for cert in cert_names:
            if fuzzy_match(skill, cert, threshold):
                print(f"[Cert Match] '{skill}' ≈ '{cert}'")
                matched += 1
                break
    return min(CONFIG["weights"]["cert_bonus_cap"], matched * 2.5)

# ---------- Semantic Scoring ----------
def embed_match_score(model: SentenceTransformer, text: str, cv_text: str, label: str = "Task") -> float:
    embedding1 = model.encode(text, convert_to_tensor=True)
    embedding2 = model.encode(cv_text, convert_to_tensor=True)
    raw_score = float(util.pytorch_cos_sim(embedding1, embedding2).item()) * 100
    print(f"[Semantic] {label}: '{text}' → Raw Score: {raw_score:.2f}")

    scale = CONFIG["semantic_scaling"]
    if raw_score < scale["low"]:
        return raw_score * scale["low_scale"]
    elif raw_score < scale["medium"]:
        return raw_score * scale["medium_scale"]
    return min(raw_score, 100.0)

def score_group(model: SentenceTransformer, group: dict, cv_text: str, match_type: str = "task") -> float:
    label = "Task" if match_type == "task" else "Requirement"
    if match_type in group:
        return embed_match_score(model, group[match_type], cv_text, label)
    if "group" in group:
        sub_scores = [score_group(sub, cv_text, match_type) for sub in group["group"]]
        if not sub_scores:
            return 0.0
        group_type = group.get("group_type", "AND")
        group_score = sum(sub_scores) / len(sub_scores) if group_type == "AND" else max(sub_scores)
        print(f"[Group-{group_type.upper()} | {label}] Sub-scores: {sub_scores} → Group score: {group_score:.2f}")
        return group_score
    return 0.0

def calculate_semantic_score(model: SentenceTransformer, responsibilities: list, qualifications: list, cv_text: str) -> tuple[float, float, float]:
    resp_scores = [score_group(model, r, cv_text, match_type="task") for r in responsibilities]
    resp_score = sum(resp_scores) / len(resp_scores) if resp_scores else 0.0

    qual_scores = [score_group(model, q, cv_text, match_type="requirement") for q in qualifications]
    qual_score = sum(qual_scores) / len(qual_scores) if qual_scores else 0.0

    combined = 0.4 * resp_score + 0.6 * qual_score
    print(f"[Semantic Combined] Responsibilities: {resp_score:.2f}%, Qualifications: {qual_score:.2f}% → Final: {combined:.2f}%")
    return combined, resp_score, qual_score

# ---------- Final Scoring ----------
def get_match_score(model: SentenceTransformer, cv: dict, job: dict, job_skills: dict[str, int], industry_keywords: list[str]) -> tuple[float, dict]:
    cv_text = extract_cv_text(cv)
    cv_skills = [s['skill'] for s in cv.get('technical_skills', [])]

    print("\n==== Normalized CV Text ====")
    print(cv_text)
    print("============================\n")

    industry_score = calculate_industry_score(cv_text, industry_keywords)
    technical_score = calculate_technical_score(cv_skills, job_skills)
    cert_bonus = calculate_certification_bonus(cv, job_skills)

    responsibilities = job.get("key_responsibilities", [])
    qualifications = job.get("required_qualifications", [])
    print("Responsibilities found:", responsibilities)
    print("Qualifications found:", qualifications)
    semantic_score, resp_score, qual_score = calculate_semantic_score(model, responsibilities, qualifications, cv_text)

    w = CONFIG["weights"]
    final_score = w["industry"] * industry_score + w["technical"] * (technical_score + cert_bonus) + w["semantic"] * semantic_score
    final_score = min(final_score, 100.0)

    explanation = {
        "industry_match": f"Industry score: {industry_score:.2f}%",
        "technical_match": f"Matched weighted score: {technical_score:.2f}%",
        "semantic_match": f"Responsibilities: {resp_score:.2f}%, Qualifications: {qual_score:.2f}%, Combined: {semantic_score:.2f}%",
        "certification_bonus": f"Bonus from certifications: {cert_bonus:.2f}%",
        "total_score": f"{final_score:.2f}%"
    }

    print(
        f"\nFinal Breakdown:\nIndustry: {industry_score:.2f}, Tech: {technical_score:.2f}, "
        f"Semantic: {semantic_score:.2f}, Cert Bonus: {cert_bonus:.2f}, Total: {final_score:.2f}\n"
    )

    return final_score, explanation