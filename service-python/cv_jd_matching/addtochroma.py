import json
import os
from typing import Optional
import google.generativeai as genai
import chromadb
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

from my_utils.embed_cv import embed_sections_cv
from my_utils.generate_related_words import extract_industry_keywords
from dotenv import load_dotenv
load_dotenv()

chroma_client = chromadb.PersistentClient(path="./chroma_data")
cv_collection_concat = chroma_client.get_or_create_collection(name="cv_embeddings_concatenated")
jd_collection_industry_keyw = chroma_client.get_or_create_collection(name="jd_industry_keywords")

model_sentence_transformer = SentenceTransformer('all-MiniLM-L6-v2')

api_key = os.getenv("GOOGLE_API_KEY")
if not api_key:
    raise ValueError("GOOGLE_API_KEY environment variable not set")
genai.configure(api_key=api_key)
model_genai = genai.GenerativeModel('gemini-1.5-pro')

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

cv = CvDTO(
    id=25,
    technicalSkills="[{skill=FPGA, level=VHDL}, {skill=Verilog}, {skill=Microcontrollers}, {skill=Arduino}, {skill=ESP32,STM32}, {skill=Data Structures}, {skill=C Java}, {skill=Software Development}, {skill=Java}, {skill=Python}, {skill=C/C++}, {skill=Machine Learning AI Basics}, {skill=Communication and collaboration}, {skill=Problem-Solving Analytical Thinking}]",
    foreignLanguages= "[{language=Romanian, proficiency=nativ}, {language=English, proficiency=proficiency Cambridge Certificate}, {language=Italian}]",
    education="[{institution=Universitatea Tehnic? din Cluj-Napoca, degree=Bachelor, field_of_study=Computer Engineering, period={start_date=2022, end_date=2026}, technologies=[]}]",
    certifications="",
    projectExperience="[{title=OpenGL 3D Graphics, description=Developed a C++ and OpenGL application with camera controls, lighting, and texture mapping, technologies=[C++, OpenGL]}, {title=Communication Protocols, description=Implemented SPI, I2C, and Ethernet protocols using VHDL for FPGA systems, technologies=[VHDL, FPGA]}, {title=Volume Hand Control, description=Hand tracking image processing-based project, technologies=[]}]",
    workExperience="[{type=job, title=Roboatelier, company=UTCN, period={date=2024}, description=[Completed a robotics camp focused on Arduino, including an Ultrasonic Radar project], technologies=[Arduino]}, {type=job, title=Cloud Workshop, company=Accenture, period={date=2024}, description=[Learned about AWS Cloud technologies through teamwork-focused exercises], technologies=[AWS]}]",
    others="[{Contact Information=[{Email=muresan.davide2003@yahoo.com}, {Address=Transilvania, Romania}, {LinkedIn=GitHub}], Summary=[Passionate student in Computer Engineering at UTCN, eager to innovate and apply technology to solve real-world problems, I love turning ideas into solutions, exploring new technologies, and constantly pushing my limits to grow, I’m not here to just “do the work.” I’m here to leave my mark and create something cool, Let’s build something awesome!, I am at your service!], Interests and hobbies=[Everything that is related to technology, Hardware, AI/Machine Learning, In terms of sports I’m a swimming enthusiast prefer hiking and exploring the wonders of nature]}]"
)

jd = JobDescriptionDTO(
    id=4,
    jobTitle="Machine Learning Engineer Tech Lead",
    companyOverview="Innovative Solutions Inc. is a leading technology company dedicated to transforming industries through cutting-edge artificial intelligence and machine learning solutions. Our mission is to empower businesses with intelligent systems that drive efficiency, enhance decision-making, and unlock new opportunities. We pride ourselves on fostering a collaborative and inclusive work environment where creativity and innovation thrive.",
    keyResponsibilities="{original_statement=Lead and mentor a team of machine learning engineers, providing technical guidance and career development support., group=[{group=[{task=Lead a team of machine learning engineers}, {task=Mentor a team of machine learning engineers}], group_type=AND}, {task=Provide technical guidance}, {task=Provide career development support}], group_type=AND} {original_statement=Design, develop, and deploy scalable machine learning models and algorithms to solve complex business problems., group=[{task=Design scalable machine learning models and algorithms to solve complex business problems}, {task=Develop scalable machine learning models and algorithms to solve complex business problems}, {task=Deploy scalable machine learning models and algorithms to solve complex business problems}], group_type=AND} {original_statement=Collaborate with cross-functional teams, including data scientists, software engineers, and product managers, to integrate machine learning solutions into products and services., task=Collaborate with cross-functional teams, including data scientists, software engineers, and product managers, to integrate machine learning solutions into products and services} {original_statement=Evaluate and implement state-of-the-art machine learning techniques and tools to enhance model performance and accuracy., group=[{task=Evaluate state-of-the-art machine learning techniques and tools to enhance model performance and accuracy}, {task=Implement state-of-the-art machine learning techniques and tools to enhance model performance and accuracy}], group_type=AND} {original_statement=Oversee the end-to-end lifecycle of machine learning projects, from data collection and preprocessing to model training and deployment., task=Oversee the end-to-end lifecycle of machine learning projects, from data collection and preprocessing to model training and deployment} {original_statement=Ensure best practices in code quality, testing, and documentation are followed within the team., group=[{task=Ensure best practices in code quality are followed within the team}, {task=Ensure best practices in testing are followed within the team}, {task=Ensure best practices in documentation are followed within the team}], group_type=AND} {original_statement=Stay updated with the latest advancements in machine learning and AI technologies, and drive innovation within the team., group=[{task=Stay updated with the latest advancements in machine learning and AI technologies}, {task=Drive innovation within the team}], group_type=AND}",
    requiredQualifications="{original_statement=Bachelor’s or Master’s degree in Computer Science, Engineering, Mathematics, or a related field., group=[{group=[{requirement=Bachelor’s degree in Computer Science, Engineering, Mathematics, or a related field}, {requirement=Master’s degree in Computer Science, Engineering, Mathematics, or a related field}], group_type=OR}], group_type=AND} {original_statement=Minimum of 5 years of experience in machine learning, with at least 2 years in a leadership or tech lead role., group=[{requirement=Minimum of 5 years of experience in machine learning}, {requirement=At least 2 years in a leadership role}, {requirement=At least 2 years in a tech lead role}], group_type=AND} {original_statement=Strong proficiency in Python and experience with machine learning frameworks such as TensorFlow, PyTorch, or Scikit-learn., group=[{requirement=Strong proficiency in Python}, {requirement=Experience with machine learning frameworks such as TensorFlow, PyTorch, or Scikit-learn}], group_type=AND} {original_statement=Proven track record of deploying machine learning models in production environments., requirement=Proven track record of deploying machine learning models in production environments} {original_statement=Excellent problem-solving skills and the ability to work independently and collaboratively in a fast-paced environment., group=[{requirement=Excellent problem-solving skills}, {requirement=The ability to work independently in a fast-paced environment}, {requirement=The ability to work collaboratively in a fast-paced environment}], group_type=AND} {original_statement=Strong communication skills, with the ability to convey complex technical concepts to non-technical stakeholders., requirement=Strong communication skills, with the ability to convey complex technical concepts to non-technical stakeholders}",
    preferredSkills="{original_statement=Experience with cloud platforms such as AWS, Google Cloud, or Azure., group=[{group=[{skill=Experience with cloud platforms such as AWS}, {skill=Experience with cloud platforms such as Google Cloud}, {skill=Experience with cloud platforms such as Azure}], group_type=OR}], group_type=AND} {original_statement=Familiarity with big data technologies like Hadoop, Spark, or Kafka., group=[{group=[{skill=Familiarity with big data technologies like Hadoop}, {skill=Familiarity with big data technologies like Spark}, {skill=Familiarity with big data technologies like Kafka}], group_type=OR}], group_type=AND} {original_statement=Knowledge of deep learning architectures and natural language processing., group=[{skill=Knowledge of deep learning architectures}, {skill=Knowledge of natural language processing}], group_type=AND} {original_statement=Experience with version control systems, such as Git, and CI/CD pipelines., group=[{skill=Experience with version control systems, such as Git}, {skill=Experience with CI/CD pipelines}], group_type=AND} {original_statement=Strong understanding of data structures, algorithms, and software design principles., group=[{skill=Strong understanding of data structures}, {skill=Strong understanding of algorithms}, {skill=Strong understanding of software design principles}], group_type=AND}",
    message= "AI and Machine Learning Solutions industry"
)

cv1 = CvDTO(
    id=22,
    technicalSkills="[{skill=Python}, {skill=TensorFlow}, {skill=JavaScript}, {skill=ReactJS}, {skill=AWS SageMaker}, {skill=Docker}, {skill=SQL}, {skill=PostgreSQL}, {skill=Figma}, {skill=Adobe XD}]",
    foreignLanguages= "[{language=English, proficiency=C1}, {language=Spanish, proficiency=B2}]",
    education="[{institution=University Politehnica of Bucharest, degree=Bachelor, period={duration=4 years}, technologies=[]}, {institution=University Politehnica of Bucharest, degree=Master, period={duration=2 years}, technologies=[]}]",
    certifications="[{name=AWS Certified Machine Learning - Specialty, institution=AWS, technologies=[Machine Learning]}, {name=TensorFlow Developer Certificate, institution=TensorFlow, technologies=[TensorFlow]}, {name=Google Cloud Professional Data Engineer, institution=Google, technologies=[]}, {name=Certified Kubernetes Application Developer (CKAD), institution=Kubernetes, technologies=[]}]",
    projectExperience="[{title=Machine Learning Model Deployment, description=Spearheaded the deployment of a scalable machine learning model using AWS SageMaker and Docker, enhancing the company's predictive analytics capabilities. Collaborated with data scientists and engineers to ensure seamless integration with existing systems. Conducted performance testing and optimization, resulting in a 30% reduction in processing time, technologies=[AWS SageMaker, Docker, Machine Learning]}, {title=Interactive Web Application Development, description=Led a team of developers in creating a dynamic web application using JavaScript and ReactJS, improving user engagement by 40%. Coordinated with UI/UX designers to implement responsive design principles, ensuring a consistent user experience across devices. Utilized agile methodologies to manage project timelines and deliverables effectively, technologies=[JavaScript, ReactJS, Agile]}, {title=Data-Driven Strategic Planning, description=Developed a comprehensive data analysis framework using SQL and PostgreSQL to support strategic decision-making processes. Worked closely with executive leadership to identify key performance indicators and generate actionable insights. Presented findings in a series of workshops, leading to a 15% improvement in operational efficiency, technologies=[SQL, PostgreSQL]}]",
    workExperience="",
    others="[{Professional Skills=[Python, TensorFlow, JavaScript, ReactJS, AWS SageMaker, Docker, SQL, PostgreSQL, Figma, Adobe XD]}]"
)


cv2 = CvDTO(
    id=28,
    technicalSkills="[{skill=HTML}, {skill=CSS}, {skill=JavaScript}, {skill=React}, {skill=MySQL}, {skill=Git}, {skill=IntelliJ}, {skill=Visual Studio}, {skill=Vs Code}, {skill=PyCharm}, {skill=Unity}, {skill=Blender}, {skill=Software Development}, {skill=Object-Oriented Programming (OOP)}, {skill=Algorithm Design and Analysis}, {skill=Database Management}, {skill=Systems Architecture}, {skill=Software Testing and Debugging}, {skill=Agile Development}, {skill=Technical Documentation}, {skill=C}, {skill=C++}, {skill=Java}, {skill=C#}]",
    foreignLanguages= "[{language=English}, {language=Romanian}]",
    education="[{institution=Universitatea Tehnic? din Cluj-Napoca (UTCN), degree=Bachelor, field_of_study=of Science in Computer ScienceExpected, period={}, technologies=[], notes=Relevant Coursework: Data Structures and Algorithms, Software Engineering, Database Management Systems, Web Development, Arti?cial Intelligence}]",
    certifications="[{name=Software Engineering Job Simulation, institution=Electronic Arts, date=2025, technologies=[]}, {name=Introduction to Front-End Development, institution=Meta, date=2024, technologies=[]}, {name=Programming with JavaScript, institution=Meta, date=2024, technologies=[JavaScript]}, {name=React Basics, institution=Meta, date=2024, technologies=[React]}]",
    projectExperience="[{title=Car racing game, description=After about 4 months of developing this game I released it publicly on the Google Play Store Designed and developed the unique graphics and atmosphere, 3D models, animations, C code and interactivity, technologies=[Unity, C#, Blender, Google API]}, {title=Open World 3D game, description=Developed an unique user experience from the main screen untill the end of the game, technologies=[Unity, C#, Blender]}, {title=Ecommerce Webiste, description=Worked on developing the frontend and backend of a modern ecommerce website, technologies=[React, JavaScript, MySQL, Node.js]}]",
    workExperience="[{type=job, title=Indie Game Developer, company=null, period={}, description=[Published and developed several game titles for mobile and PC platforms, using Unity and object orientated languages, Developed 3D models and scenes using Blender], technologies=[Unity, Blender]}, {type=job, title=Sportsman, company=Shooting sportsman at Univeristy of Cluj Napoca, period={}, description=[], technologies=[]}]",
    others="[{Contact Information=[{Phone number=0770832968}, {Email=alexandru030417@yahoo.com}, {Website=Github}, {LinkedIn=Linkedin}]}]"
)


cv3 = CvDTO(
    id=69,
    technicalSkills="[{skill=Python, level=4}, {skill=TensorFlow, level=4}, {skill=JavaScript, level=3}, {skill=ReactJS, level=3}, {skill=AWS SageMaker, level=2}, {skill=Docker, level=2}, {skill=SQL, level=3}, {skill=PostgreSQL, level=3}, {skill=Figma, level=2}, {skill=Adobe XD, level=2}]",
    foreignLanguages= "[{language=English, proficiency=B2}, {language=Spanish, proficiency=A2}, {language=French, proficiency=B1}]",
    education="[{institution=University of Bucharest, degree=Bachelor, field_of_study=Computer Science, period={duration=4 years}, technologies=[]}, {institution=University of Bucharest, degree=Master, field_of_study=Computer Science, period={duration=2 years}, technologies=[]}]",
    certifications="[{name=AWS Certified Machine Learning - Specialty, institution=null, technologies=[Machine Learning, AWS]}, {name=TensorFlow Developer Certificate, institution=null, technologies=[TensorFlow]}]",
    projectExperience="[{title=Machine Learning Model Deployment on AWS SageMaker, description=Led a project to deploy a machine learning model for predictive analytics using AWS SageMaker. Utilized Python and TensorFlow to develop and train the model, achieving a 95% accuracy rate in predictions. Implemented Docker containers to streamline the deployment process, ensuring scalability and efficient resource management., technologies=[Python, TensorFlow, AWS SageMaker, Docker]}, {title=Interactive Dashboard for Data Visualization, description=Developed an interactive web-based dashboard using ReactJS and PostgreSQL to visualize complex datasets for a financial services client. The dashboard provided real-time data insights and analytics, enhancing decision-making processes. Integrated SQL queries to efficiently manage and retrieve data, resulting in a 50% reduction in data processing time., technologies=[JavaScript, ReactJS, SQL, PostgreSQL]}, {title=UX/UI Design for a Mobile Health Application, description=Designed a user-centric mobile health application interface using Figma and Adobe XD. Conducted extensive user research and usability testing to ensure the application met user needs and expectations. Collaborated closely with developers to integrate design elements seamlessly, resulting in a 30% increase in user engagement., technologies=[Figma, Adobe XD]}]",
    workExperience="",
    others=""
)


cv4 = CvDTO(
    id=84,
    technicalSkills="[{skill=Figma}, {skill=Adobe XD}, {skill=HTML}, {skill=CSS}, {skill=JavaScript}, {skill=ReactJS}, {skill=TypeScript}, {skill=Sketch}, {skill=InVision}]",
    foreignLanguages= "[{language=English, proficiency=C1}, {language=Spanish, proficiency=B2}, {language=French, proficiency=A2}]",
    education="[{institution=University of Bucharest, degree=Bachelor, period={duration=4 years}, technologies=[]}, {institution=University of Bucharest, degree=Master, period={duration=2 years}, technologies=[]}]",
    certifications="[{name=Adobe Certified Expert (ACE), institution=Adobe, technologies=[]}, {name=Google Mobile Web Specialist, institution=Google, technologies=[Web]}, {name=Microsoft Certified: Azure Developer Associate, institution=Microsoft, technologies=[Azure]}]",
    projectExperience="[{title=Interactive Web Application for Real Estate Listings, description=Developed a dynamic web application for real estate listings using ReactJS and TypeScript, enhancing user interaction and data visualization. Implemented responsive design principles with HTML and CSS to ensure seamless user experience across devices. Utilized Figma and Adobe XD for prototyping and design, ensuring a visually appealing and intuitive interface. Collaborated with a team to integrate Google Maps API, providing users with real-time location data and navigation features., technologies=[ReactJS, TypeScript, HTML, CSS, Figma, Adobe XD, Google Maps API]}, {title=Mobile App Prototype for Fitness Tracking, description=Designed a comprehensive mobile app prototype for fitness tracking using Sketch and InVision, focusing on user-centric design and functionality. Conducted extensive user research and usability testing to refine the app's interface and improve user engagement. Leveraged Adobe XD to create interactive prototypes that facilitated stakeholder feedback and iterative design improvements. Worked closely with developers to ensure the design was accurately translated into the final product, enhancing the app's usability and aesthetic appeal., technologies=[Sketch, InVision, Adobe XD]}]",
    workExperience="",
    others=""
)


cv5 = CvDTO(
    id=25,
    technicalSkills="[{skill=Java}, {skill=SpringBoot}, {skill=C/C++}, {skill=MySQL}, {skill=Python}, {skill=HTML}, {skill=CSS}, {skill=Git}]",
    foreignLanguages= "[{language=English, proficiency=fluent}, {language=German, proficiency=classroom study}, {language=Romanian, proficiency=native}]",
    education="[{institution=Technical University of Cluj-Napoca, degree=Bachelor, field_of_study=Computer Science, period={start_date=2022-10, end_date=Present}, technologies=[]}]",
    certifications="[{name=Adobe Certified Expert (ACE), institution=Adobe, technologies=[]}, {name=Google Mobile Web Specialist, institution=Google, technologies=[Web]}, {name=Microsoft Certified: Azure Developer Associate, institution=Microsoft, technologies=[Azure]}]",
    projectExperience="[{title=SpringLibrary, description=A web application that enables the management of books from a catalogue. It has different types of users that can perform actions based on their roles. The login system is based on Java Spring Security. The project has an architecture that combines Layers and MVC while adhering to SOLID principles., technologies=[SpringBoot, Spring Security, Lombok, Gradle, Thymeleaf, HTML]}, {title=Library, description=A desktop application which was designed to be used in a real-life bookstore, where employees can add or sell books and managers can obtain reports. It has different types of users that can perform actions based on their roles. Design patterns: Decorator, Builder and FactoryMethod., technologies=[Java, Gradle]}, {title=TheShire, description=An interactive application built using OpenGL which features free first-person exploration, dynamic scene transitions, advanced lighting and graphical effects., technologies=[C++, OpenGL, GLSL, GLM]}]",
    workExperience="[{type=job, title=Intern Java Software Engineer, company=Accesa, period={start_date=2023-09}, description=[During my one month at Accesa, I studied alongside a Senior Java Developer and worked on Java and SpringBoot based applications.], technologies=[Java, SpringBoot]}, {type=job, title=Apprentice, company=Accesa, period={date=2021-07}, description=[This was a two week apprenticeship where I shadowed two Senior Java Developers and learned how the Agile methodology works.], technologies=[Java]}]",
    others="[{Contact=[{Phone number=+40737016376}, {Email=molnar.sara.viviana@gmail.com}, {Website=Portfolio Website}, {LinkedIn=LinkedIn}], Hobbies=[Escaping into fictional worlds while reading., Helping children discover their interests and strengths through volunteering., Researching and soul-searching in order to write impactful articles and stories.], About Me=[The things that drive me are curiosity, ambition and a strong desire to become better than yesterday’s version of myself., I believe in kindness and authenticity, especially while working in team settings., I have a strong sense of leadership, given that I have volunteered for 4 years as a team leader in a youth organization., That is how I discovered that I thrive in environments where creativity, genuineness and hard work are valued., That is what I’m searching for: a team with which I can create and work on amazing projects that make Monday mornings exiting.], Interpersonal Skills=[Organization, As a Communications Manager for the Edubiz Association, I was responsible for creating the content calendar and coordinating the teams responsible for creating posts., In this role, I developed effective communication skills to establish realistic schedules, manage situations where a team member did not complete their tasks, and prioritize and reallocate assignments to prevent significant gaps., Additionally, I handled conflict resolution both within the team and between team members and senior management.], Adaptability and empathy=[I developed these skills while volunteering in afterschool centers for young people aged 8 to 14., Each week, I had to prepare engaging activities and select the most suitable ones based on their energy levels at the time., Additionally, before the activities, I held sessions to help them with their homework., Through this experience, I learned to structure my thoughts and express them concisely, ensuring my explanations were clear and easy to understand.], Team leading=[As a volunteer, I have coordinated multiple teams over time, such as:, volunteer teams, for which I organized weekly meetings to plan activities for afterschool centers, ensured their participation in association-led workshops, and facilitated communication between them and my supervisors., the blog team, for which I organized and led weekly meetings where I provided feedback on previously written articles and assigned new tasks to team members.], Publications=[Timpul - avem si nu avem, 1984 de George Orwell - impresii, EduBiz lanseaza proiectul \"Aripi de file\", In this article I explored the pitfalls of procrastination and how one can avoid going down the rabbit hole of time wasting., This is my review for George Orwell’s classic dystopian, 1984., This is the story of how I ended up coordinating a book club in my hometown.]}]"
)





embed_sections_cv(cv1, model_sentence_transformer, cv_collection_concat)
embed_sections_cv(cv2, model_sentence_transformer, cv_collection_concat)
embed_sections_cv(cv3, model_sentence_transformer, cv_collection_concat)
embed_sections_cv(cv4, model_sentence_transformer, cv_collection_concat)
embed_sections_cv(cv5, model_sentence_transformer, cv_collection_concat)

#domain = jd.message
# industry_keywords = extract_industry_keywords(model_genai, domain)
#
#
# embedding = model_sentence_transformer.encode(domain, convert_to_tensor=True)
# jd_collection_industry_keyw.add(
#     ids=[f"{jd.id}"],
#     embeddings=[embedding.tolist()],
#     documents=[json.dumps(industry_keywords)]
# )


#
# cv_collection_concat.delete(ids=["7"])