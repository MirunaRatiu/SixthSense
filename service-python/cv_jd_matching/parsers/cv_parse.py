import re
import json

def normalize_quotes(s: str) -> str:
    return s.replace("’", "'").replace("‘", "'").replace("“", '"').replace("”", '"')

def unwrap_quotes_if_needed(value: str) -> str:
    value = value.strip()
    if value.startswith('"') and value.endswith('"'):
        return value[1:-1]
    return value

def java_style_to_json_objects(raw: str) -> list[dict]:
    raw = unwrap_quotes_if_needed(raw)
    raw = normalize_quotes(raw)

    # --- Block-aware object extraction ---
    blocks = []
    i = 0
    while i < len(raw):
        if raw[i] == '{':
            start = i
            depth = 1
            i += 1
            while i < len(raw) and depth > 0:
                if raw[i] == '{':
                    depth += 1
                elif raw[i] == '}':
                    depth -= 1
                i += 1
            if depth == 0:
                blocks.append(raw[start:i])
        else:
            i += 1

    parsed = []
    for block in blocks:
        try:
            items = []
            current = ''
            depth = 0
            for char in block[1:-1]:  # Skip outer {}
                if char in ['{', '[']:
                    depth += 1
                elif char in ['}', ']']:
                    depth -= 1
                if char == ',' and depth == 0:
                    items.append(current.strip())
                    current = ''
                else:
                    current += char
            if current.strip():
                items.append(current.strip())

            obj = {}
            for item in items:
                if '=' in item:
                    key, value = item.split('=', 1)
                    key = key.strip()
                    value = value.strip()

                    if value.startswith('{'):
                        sub_obj = java_style_to_json_objects(f"[{value}]")
                        if sub_obj:
                            obj[key] = sub_obj[0]

                    elif value.startswith('['):
                        inner = value[1:-1].strip()
                        if inner.startswith('{'):
                            obj[key] = java_style_to_json_objects(value)
                        elif '=' in inner:
                            nested_items = []
                            parts = inner.split('},')
                            for part in parts:
                                part = part.strip()
                                if not part.endswith('}'):
                                    part += '}'
                                nested_items += java_style_to_json_objects(part)
                            obj[key] = nested_items
                        else:
                            # FINAL FIX: Split text correctly on periods, not commas
                            text_items = [re.sub(r'^,?\s*', '', x.replace("|", "I")).strip() for x in
                                          re.split(r'\.\s*', inner) if x.strip()]
                            obj[key] = text_items
                    else:
                        if value.lower() == 'null':
                            obj[key] = None
                        else:
                            obj[key] = value.strip('"')

            parsed.append(obj)

        except Exception as e:
            print(f"[Parse Error] Block failed parsing:\n{block}\n→ {e}\n")

    return parsed

def transform_dto_to_cv(dto: dict) -> dict:
    def clean_description(desc):
        if isinstance(desc, list):
            return [s.replace("|", "I").replace("  ", " ").strip() for s in desc if isinstance(s, str)]
        elif isinstance(desc, str):
            return [desc.replace("|", "I").strip()]
        return []

    cv = {
        "technical_skills": java_style_to_json_objects(dto.get("technicalSkills", "")),
        "foreign_languages": java_style_to_json_objects(dto.get("foreignLanguages", "")),
        "education": java_style_to_json_objects(dto.get("education", "")),
        "certifications": java_style_to_json_objects(dto.get("certifications", "")),
        "project_experience": [],
        "work_experience": [],
        "others": {}
    }

    for project in java_style_to_json_objects(dto.get("projectExperience", "")):
        cleaned_project = {
            "title": project.get("title", ""),
            "description": project.get("description", "").replace("|", "I").strip(),
            "technologies": project.get("technologies", [])
        }
        cv["project_experience"].append(cleaned_project)

    for job in java_style_to_json_objects(dto.get("workExperience", "")):
        cleaned_job = {
            "type": job.get("type", ""),
            "title": job.get("title", ""),
            "company": job.get("company", ""),
            "period": job.get("period", {}),
            "description": clean_description(job.get("description", [])),
            "technologies": job.get("technologies", [])
        }
        cv["work_experience"].append(cleaned_job)

    # Handle 'others'
    raw_others = java_style_to_json_objects(dto.get("others", ""))
    if raw_others and isinstance(raw_others, list) and isinstance(raw_others[0], dict):
        cv["others"] = raw_others[0]

    return cv

# cv_dto_raw = {
#     "technicalSkills": "[{skill=Java}, {skill=Python}, {skill=SpringBoot}, {skill=HTML}, {skill=C++}, {skill=CSS}, {skill=MySQL}, {skill=Git}]",
#     "foreignLanguages": "[{language=English, proficiency=fluent}, {language=German, proficiency=classroom study}, {language=Romanian, proficiency=native}]",
#     "education": "[{institution=Technical University of Cluj-Napoca, degree=Bachelor, field_of_study=Science, period={start_date=2022-10, end_date=Present}, technologies=[]}]",
#     "certifications": "[{name=AWS Certified Developer - Associate, institution=null, technologies=[]}, {name=Microsoft Certified: Azure Developer Associate, institution=null, technologies=[]}, {name=Google Professional Cloud Developer, institution=null, technologies=[]}]",
#     "projectExperience": "[{title=SpringLibrary, description=A web application that enables the management of books from a catalogue. It has different types of users that can perform actions based on their roles. e The login system is based on Java Spring Security. - The project has an architecture that combines Layers and MVC while adhering to SOLID principles., technologies=[SpringBoot, Spring Security, Lombok, Gradle, Thymeleaf, HTML]}, {title=Library, description=A desktop application which was designed to be used in a real-life bookstore, where employees can add or sell books and managers can obtain reports. - It has different types of users that can perform actions based on their roles. Design patterns: Decorator, Builder and FactoryMethod., technologies=[Java, Gradle]}, {title=TheShire, description=An interactive application built using OpenGL which features free first-person exploration, dynamic scene transitions, advanced lighting and graphical effects., technologies=[C++, OpenGL, GLSL, GLM]}]",
#     "workExperience": "[{type=job, title=Intern Java Software Engineer, company=Accesa, period={start_date=2023-09}, description=[During my one month at Accesa, | studied alongside a Senior Java Developer and worked on Java and SpringBoot based applications.], technologies=[Java, SpringBoot]}, {type=job, title=Apprentice, company=Accesa, period={start_date=2021-07}, description=[This was a two week apprenticeship where | shadowed two Senior Java Developers and learned how the Agile methodology works.], technologies=[Java]}]",
#     "others": "[{About Me=[The things that drive me are curiosity, ambition and a strong desire to become better than yesterday's version of myself., | believe in kindness and authenticity, especially while working in team settings., | have a strong sense of leadership, given that | have volunteered for 4 years as a team leader in a youth organization., That is how | discovered that | thrive in environments where creativity, genuineness and hard work are valued., That is what I'm searching for: a team with which | can create and work on amazing projects that make Monday mornings exiting.], Contact Information=[{Address=cluj-Napoca, Romania}, {Phone number=+40) 737 016 376}, {Email=molnar.sara.viviana@gmail.com}, {Website=Portfolio Website}, {LinkedIn=Linkedin}], Hobbies=[Escaping into fictional worlds while reading., Helping children discover their interests and strengths through Library volunteering., Researching and soul-searching in order to write impactful articles and stories.], Interpersonal Skills=[Organization, Adaptability and empathy, Team leading], Publications=[Timpul - avem si nu avem, In this article I explored the pitfalls of procrastination and how one can avoid going down the rabbit hole of time wasting., 1984 de George Orwell - impresii, This is my review for George Orwell's classic dystopian, 1984., EduBiz lanseaza proiectul \"Aripi de file\", This is the story of how | ended up coordinating a book club in my hometown.]}]"
# }
#
# # Test it:
# parsed_cv = transform_dto_to_cv(cv_dto_raw)
#
# # Print result
# from pprint import pprint
# pprint(parsed_cv)
# for item in parsed_cv["others"]["Publications"]:
#     print(f"- {item}")



