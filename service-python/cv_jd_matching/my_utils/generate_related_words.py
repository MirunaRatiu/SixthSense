import json
import google.generativeai as genai
from google.generativeai import GenerativeModel


def extract_industry_keywords(model: GenerativeModel, domain):

    prompt = f"""
You are a keyword extractor. Given an industry domain, generate exactly 20 simple, basic, and widely recognized keywords.
Rules:
- Use only lowercase letters.
- Replace spaces in multi-word phrases with underscores (e.g., online_banking, mobile_app).
- No punctuation, no special characters.
- Keywords must be accessible to non-experts.
- Focus on common tools, concepts, and terms people use in that domain.
- Output only a JSON object with a single key "INDUSTRY_KEYWORDS" containing an array of exactly 50 entries.
Domain: {domain}
Respond ONLY with the JSON.
    """

    response_schema = {
        "type": "object",
        "properties": {
            "INDUSTRY_KEYWORDS": {
                "type": "array",
                "items": {"type": "string"}
            }
        },
        "required": ["INDUSTRY_KEYWORDS"]
    }

    try:
        response = model.generate_content(
            prompt,
            generation_config=genai.GenerationConfig(
                response_mime_type="application/json",
                response_schema=response_schema
            )
        )

        # Parsează răspunsul JSON
        keywords_json = json.loads(response.text)
        keywords = keywords_json.get("INDUSTRY_KEYWORDS", [])

        if len(keywords) < 20:
            print(f"Warning: Received {len(keywords)} keywords instead of 20")

        print(f"\nINDUSTRY_KEYWORDS for '{domain}':")
        print(json.dumps(keywords, indent=2))

        return keywords[:20]

    except Exception as e:
        print(f"Error during keyword extraction: {e}")
        return []

#
# if __name__ == "__main__":
#     test_domain = "Healthcare"
#     keywords = extract_industry_keywords(test_domain)
#     if not keywords:
#         print("Failed to extract keywords")

