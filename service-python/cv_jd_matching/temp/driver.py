import os

import chromadb

from parsers.cv_parse import transform_dto_to_cv
from my_utils.generate_related_words import extract_industry_keywords
from parsers.jd_parse import transform_dto_to_jd
# from matching_logic.match import get_match_score
from matching_logic.modified_match import get_match_score
from sentence_transformers import SentenceTransformer
import google.generativeai as genai

api_key = os.getenv("GOOGLE_API_KEY")
if not api_key:
    raise ValueError("GOOGLE_API_KEY environment variable not set")
genai.configure(api_key=api_key)
model_genai = genai.GenerativeModel('gemini-1.5-pro')

chroma_client = chromadb.PersistentClient(path="../chroma_data")
cv_collection_concat = chroma_client.get_or_create_collection(name="cv_embeddings_concatenated")


cv = {
            "id": 101,
            "technicalSkills": "[{skill=Java}, {skill=Python}, {skill=SpringBoot}, {skill=HTML}, {skill=C++}, {skill=CSS}, {skill=MySQL}, {skill=Git}]",
            "foreignLanguages": "[{language=English, proficiency=fluent}, {language=German, proficiency=classroom study}, {language=Romanian, proficiency=native}]",
            "education": "[{institution=Technical University of Cluj-Napoca, degree=Bachelor, field_of_study=Science, period={start_date=2022-10, end_date=Present}, technologies=[]}]",
            "certifications": "[{name=AWS Certified Developer - Associate, institution=null, technologies=[]}, {name=Microsoft Certified: Azure Developer Associate, institution=null, technologies=[]}, {name=Google Professional Cloud Developer, institution=null, technologies=[]}]",
            "projectExperience": "[{title=SpringLibrary, description=A web application that enables the management of books from a catalogue. It has different types of users that can perform actions based on their roles. e The login system is based on Java Spring Security. - The project has an architecture that combines Layers and MVC while adhering to SOLID principles., technologies=[SpringBoot, Spring Security, Lombok, Gradle, Thymeleaf, HTML]}, {title=Library, description=A desktop application which was designed to be used in a real-life bookstore, where employees can add or sell books and managers can obtain reports. - It has different types of users that can perform actions based on their roles. Design patterns: Decorator, Builder and FactoryMethod., technologies=[Java, Gradle]}, {title=TheShire, description=An interactive application built using OpenGL which features free first-person exploration, dynamic scene transitions, advanced lighting and graphical effects., technologies=[C++, OpenGL, GLSL, GLM]}]",
            "workExperience": "[{type=job, title=Intern Java Software Engineer, company=Accesa, period={start_date=2023-09}, description=[During my one month at Accesa, | studied alongside a Senior Java Developer and worked on Java and SpringBoot based applications.], technologies=[Java, SpringBoot]}, {type=job, title=Apprentice, company=Accesa, period={start_date=2021-07}, description=[This was a two week apprenticeship where | shadowed two Senior Java Developers and learned how the Agile methodology works.], technologies=[Java]}]",
            "others": "[{About Me=[The things that drive me are curiosity, ambition and a strong desire to become better than yesterday's version of myself., | believe in kindness and authenticity, especially while working in team settings., | have a strong sense of leadership, given that | have volunteered for 4 years as a team leader in a youth organization., That is how | discovered that | thrive in environments where creativity, genuineness and hard work are valued., That is what I'm searching for: a team with which | can create and work on amazing projects that make Monday mornings exiting.], Contact Information=[{Address=cluj-Napoca, Romania}, {Phone number=+40) 737 016 376}, {Email=molnar.sara.viviana@gmail.com}, {Website=Portfolio Website}, {LinkedIn=Linkedin}], Hobbies=[Escaping into fictional worlds while reading., Helping children discover their interests and strengths through Library volunteering., Researching and soul-searching in order to write impactful articles and stories.], Interpersonal Skills=[Organization, Adaptability and empathy, Team leading], Publications=[Timpul - avem si nu avem, In this article I explored the pitfalls of procrastination and how one can avoid going down the rabbit hole of time wasting., 1984 de George Orwell - impresii, This is my review for George Orwell's classic dystopian, 1984., EduBiz lanseaza proiectul \"Aripi de file\", This is the story of how | ended up coordinating a book club in my hometown.]}]"
        }
jd =  {
            "id": 101,
            "jobTitle": "Senior UI/UX Designer",
            "message" : "Technology Services industry",
            "companyOverview": "InnovateTech Solutions is a leading technology company dedicated to creating cutting-edge digital products that enhance user experiences across various platforms. Our team is passionate about innovation, creativity, and delivering exceptional solutions that meet the evolving needs of our clients. We pride ourselves on fostering a collaborative and inclusive work environment where every team member's ideas are valued and contribute to our success.",
            "keyResponsibilities": "{original_statement=Lead the design and development of user interfaces and experiences for web and mobile applications, ensuring a seamless and intuitive user journey., group=[{group=[{task=Lead the design of user interfaces and experiences for web and mobile applications, ensuring a seamless and intuitive user journey}, {task=Lead the development of user interfaces and experiences for web and mobile applications, ensuring a seamless and intuitive user journey}], group_type=AND}], group_type=AND} {original_statement=Collaborate with cross-functional teams, including product managers, developers, and other designers, to translate business requirements into innovative design solutions., task=Collaborate with cross-functional teams, including product managers, developers, and other designers, to translate business requirements into innovative design solutions} {original_statement=Conduct user research and usability testing to gather insights and validate design concepts, iterating based on feedback to enhance user satisfaction., group=[{group=[{task=Conduct user research to gather insights and validate design concepts, iterating based on feedback to enhance user satisfaction}, {task=Conduct usability testing to gather insights and validate design concepts, iterating based on feedback to enhance user satisfaction}], group_type=AND}], group_type=AND} {original_statement=Create wireframes, prototypes, and high-fidelity designs using industry-standard design tools, ensuring consistency with brand guidelines and design systems., group=[{group=[{task=Create wireframes using industry-standard design tools, ensuring consistency with brand guidelines and design systems}, {task=Create prototypes using industry-standard design tools, ensuring consistency with brand guidelines and design systems}, {task=Create high-fidelity designs using industry-standard design tools, ensuring consistency with brand guidelines and design systems}], group_type=AND}], group_type=AND} {original_statement=Mentor and provide guidance to junior designers, fostering a culture of continuous learning and improvement within the design team., group=[{group=[{task=Mentor junior designers, fostering a culture of continuous learning and improvement within the design team}, {task=Provide guidance to junior designers, fostering a culture of continuous learning and improvement within the design team}], group_type=AND}], group_type=AND} {original_statement=Stay updated with the latest UI/UX trends, techniques, and technologies, and apply them to improve design processes and deliverables., task=Stay updated with the latest UI/UX trends, techniques, and technologies, and apply them to improve design processes and deliverables} {original_statement=Present design concepts and solutions to stakeholders, articulating design rationale and incorporating feedback to refine designs., task=Present design concepts and solutions to stakeholders, articulating design rationale and incorporating feedback to refine designs} ",
            "requiredQualifications": "{original_statement=Bachelor’s degree in Design, Human-Computer Interaction, or a related field., group=[{group=[{requirement=Bachelor’s degree in Design}, {requirement=Bachelor’s degree in Human-Computer Interaction}, {requirement=Bachelor’s degree in a related field}], group_type=OR}], group_type=AND} {original_statement=Minimum of 5 years of experience in UI/UX design, with a strong portfolio showcasing diverse design projects., requirement=Minimum of 5 years of experience in UI/UX design, with a strong portfolio showcasing diverse design projects} {original_statement=Proficiency in design software such as Adobe Creative Suite, Sketch, Figma, or similar tools., group=[{group=[{requirement=Proficiency in design software such as Adobe Creative Suite}, {requirement=Proficiency in design software such as Sketch}, {requirement=Proficiency in design software such as Figma}, {requirement=Proficiency in design software such as similar tools}], group_type=OR}], group_type=AND} {original_statement=Strong understanding of user-centered design principles and best practices., requirement=Strong understanding of user-centered design principles and best practices} {original_statement=Excellent communication and presentation skills, with the ability to articulate design decisions effectively., group=[{group=[{requirement=Excellent communication skills, with the ability to articulate design decisions effectively}, {requirement=Excellent presentation skills, with the ability to articulate design decisions effectively}], group_type=AND}], group_type=AND}",
            "preferredSkills": "{original_statement=Experience with front-end development technologies such as HTML, CSS, and JavaScript., group=[{group=[{skill=Experience with front-end development technologies such as HTML}, {skill=Experience with front-end development technologies such as CSS}, {skill=Experience with front-end development technologies such as JavaScript}], group_type=AND}], group_type=AND} {original_statement=Familiarity with agile methodologies and working in an agile environment., group=[{group=[{skill=Familiarity with agile methodologies}, {skill=Working in an agile environment}], group_type=AND}], group_type=AND} {original_statement=Knowledge of accessibility standards and best practices in design., skill=Knowledge of accessibility standards and best practices in design} {original_statement=Experience in designing for a variety of platforms, including web, mobile, and emerging technologies like AR/VR., group=[{group=[{skill=Experience in designing for a variety of platforms, including web}, {skill=Experience in designing for a variety of platforms, including mobile}, {skill=Experience in designing for a variety of platforms, including emerging technologies like AR/VR}], group_type=AND}], group_type=AND}"

        }

job_skills = {
    "Python": 30,
    "TensorFlow": 20,
    "PyTorch": 20,
    "Scikit-learn": 10,
    "AWS": 10,
    "Git": 10
}

# industry_keywords = [
#     "machine learning",
#     "artificial intelligence",
#     "AI",
#     "data science"
# ]
# industry_keywords = [
#     "banking",
#     "payments",
#     "Savings account",
#     "Loans"
# ]



model = SentenceTransformer('all-MiniLM-L6-v2')

transformed_cv = transform_dto_to_cv(cv)
transformed_jd = transform_dto_to_jd(jd)

print(transformed_cv)
print(transformed_jd)

domain = jd.get("message", "none")
print("Domain: ", domain)
industry_keywords = extract_industry_keywords(model_genai, domain)
if not industry_keywords:
    print("Failed to extract keywords")
else:
    print(industry_keywords)

score, explanation = get_match_score(
    model,
    cv_collection_concat,
    121,
    transformed_jd,
    job_skills,
    industry_keywords
)
# score, explanation = get_match_score(model, cv, job, job_skills, industry_keywords)
print(f"Match Score: {score:.2f}%")
print("Explanation:", explanation)