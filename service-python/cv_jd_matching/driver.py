from match import get_match_score

cv = {    "technical_skills": [        {"skill": "Java"}, {"skill": "Python"}, {"skill": "SpringBoot"}, {"skill": "HTML"},        {"skill": "C++"}, {"skill": "CSS"}, {"skill": "MySQL"}, {"skill": "Git"}, {"skill": "Angular"}    ],    "foreign_languages": [        {"language": "English", "proficiency": "fluent"},        {"language": "German", "proficiency": "classroom study"},        {"language": "Romanian", "proficiency": "native"}    ],    "education": [        {            "institution": "Technical University of Cluj-Napoca",            "degree": "Bachelor",            "field_of_study": "Computer Science",            "period": {"start_date": "2022-10", "end_date": "Present"},            "technologies": []        }    ],    "certifications": [        {"name": "AWS Certified Developer - Associate", "institution": None, "technologies": []},        {"name": "Microsoft Certified: Azure Developer Associate", "institution": None, "technologies": []},        {"name": "Google Professional Cloud Developer", "institution": None, "technologies": []}    ],    "project_experience": [        {            "title": "SpringLibrary",            "description": "A web application that enables the management of books from a catalogue...",            "technologies": ["SpringBoot", "Spring Security", "Lombok", "Gradle", "Thymeleaf", "HTML"]        }    ],    "work_experience": [    {        "type": "job",        "title": "Intern Java Software Engineer",        "company": "Accesa",        "period": { "start_date": "2023-09" },        "description": [            "During my one month at Accesa, I studied alongside a Senior Java Developer..."        ],        "technologies": ["Java", "SpringBoot"]    },    {        "type": "job",        "title": "Apprentice",        "company": "Accesa",        "period": { "start_date": "2021-07" },        "description": [            "This was a two week apprenticeship where I shadowed two Senior Java Developers and learned how the Agile methodology works."        ],        "technologies": []    }],    "others": {         "About Me": [             "The things that drive me are curiosity, ambition and a strong desire to become better than yesterday's version of myself.",             "I believe in kindness and authenticity, especially while working in team settings.",             "I have a strong sense of leadership, given that I have volunteered for 4 years as a team leader in a youth organization.",             "That is how I discovered that I thrive in environments where creativity, genuineness and hard work are valued.",             "That is what I'm searching for: a team with which I can create and work on amazing projects that make Monday mornings exciting."         ],         "Contact Information": [             {"Address": "Cluj-Napoca, Romania"},             {"Phone number": "+40737016376"},             {"Email": "molnar.sara.viviana@gmail.com"},             {"Website": "Portfolio Website"},             {"LinkedIn": "Linkedin"}         ],         "Hobbies": [             "Escaping into fictional worlds while reading.",             "Helping children discover their interests and strengths through Library volunteering.",             "Researching and soul-searching in order to write impactful articles and stories."         ],         "Interpersonal Skills": [             "Organization: As a Communications Manager for the Edubiz Association, I was responsible for creating the content calendar and coordinating the teams responsible for creating posts.",             "In this role, I developed effective communication skills to establish realistic schedules, manage situations where a team member did not complete their tasks, and prioritize and reallocate assignments to prevent significant gaps.",             "Additionally, I handled conflict resolution both within the team and between team members and senior management.",             "Adaptability and empathy: I developed these skills while volunteering in afterschool centers for young people aged 8 to 14.",             "Each week, I had to prepare engaging activities and select the most suitable ones based on their energy levels at the time.",             "Additionally, before the activities, I held sessions to help them with their homework. Through this experience, I learned to structure my thoughts and express them concisely, ensuring my explanations were clear and easy to understand.",             "Team leading: As a volunteer, I have coordinated multiple teams over time, such as:",             "- volunteer teams, for which I organized weekly meetings to plan activities for afterschool centers, ensured their participation in association-led workshops, and facilitated communication between them and my supervisors.",             "- the blog team, for which I organized and led weekly meetings where I provided feedback on previously written articles and assigned new tasks to team members."         ],         "Publications": [             "Timpul - avem si nu avem: In this article I explored the pitfalls of procrastination and how one can avoid going down the rabbit hole of time wasting.",             "1984 de George Orwell - impresii: This is my review for George Orwell's classic dystopian, 1984.",             "EduBiz lanseaza proiectul 'Aripi de file': This is the story of how I ended up coordinating a book club in my hometown."         ]     }}

job = {
  "job_title": "Machine Learning Engineer Tech Lead",
  "company_overview": "InnovateTech Solutions is a leading technology company dedicated to transforming industries through cutting-edge artificial intelligence and machine learning solutions. Our mission is to empower businesses by providing innovative tools and insights that drive efficiency and growth. We foster a collaborative and inclusive work environment where creativity and innovation thrive.",
  "message": "AI and Machine Learning Solutions industry",
  "key_responsibilities": [
    {
      "original_statement": "Lead and mentor a team of machine learning engineers to design, develop, and deploy scalable machine learning models.",
      "group": [
        {
          "group": [
            {
              "task": "Lead a team of machine learning engineers"
            },
            {
              "task": "Mentor a team of machine learning engineers"
            }
          ],
          "group_type": "AND"
        },
        {
          "group": [
            {
              "task": "design scalable machine learning models"
            },
            {
              "task": "develop scalable machine learning models"
            },
            {
              "task": "deploy scalable machine learning models"
            }
          ],
          "group_type": "AND"
        }
      ],
      "group_type": "AND"
    },
    {
      "original_statement": "Collaborate with cross-functional teams to define project requirements and deliver high-quality solutions that meet business objectives.",
      "group": [
        {
          "task": "Collaborate with cross-functional teams to define project requirements"
        },
        {
          "task": "Collaborate with cross-functional teams to deliver high-quality solutions that meet business objectives"
        }
      ],
      "group_type": "AND"
    },
    {
      "original_statement": "Architect and implement robust machine learning pipelines and frameworks to support data-driven decision-making.",
      "group": [
        {
          "task": "Architect robust machine learning pipelines and frameworks to support data-driven decision-making"
        },
        {
          "task": "Implement robust machine learning pipelines and frameworks to support data-driven decision-making"
        }
      ],
      "group_type": "AND"
    },
    {
      "original_statement": "Conduct research and stay abreast of the latest advancements in machine learning and AI technologies to integrate into our solutions.",
      "task": "Conduct research and stay abreast of the latest advancements in machine learning and AI technologies to integrate into our solutions"
    },
    {
      "original_statement": "Oversee the end-to-end lifecycle of machine learning projects, from data collection and preprocessing to model evaluation and deployment.",
      "task": "Oversee the end-to-end lifecycle of machine learning projects, from data collection and preprocessing to model evaluation and deployment"
    },
    {
      "original_statement": "Ensure best practices in code quality, testing, and documentation are maintained across the team.",
      "group": [
        {
          "group": [
            {
              "task": "Ensure best practices in code quality are maintained across the team"
            },
            {
              "task": "Ensure best practices in testing are maintained across the team"
            },
            {
              "task": "Ensure best practices in documentation are maintained across the team"
            }
          ],
          "group_type": "AND"
        }
      ],
      "group_type": "AND"
    },
    {
      "original_statement": "Drive innovation by proposing and implementing new methodologies and tools to enhance the efficiency and effectiveness of machine learning processes.",
      "group": [
        {
          "task": "Drive innovation by proposing new methodologies and tools to enhance the efficiency and effectiveness of machine learning processes"
        },
        {
          "task": "Drive innovation by implementing new methodologies and tools to enhance the efficiency and effectiveness of machine learning processes"
        }
      ],
      "group_type": "AND"
    }
  ],
  "required_qualifications": [
    {
      "original_statement": "Bachelor’s or Master’s degree in Computer Science, Engineering, Mathematics, or a related field.",
      "group": [
        {
          "group": [
            {
              "requirement": "Bachelor’s degree in Computer Science"
            },
            {
              "requirement": "Bachelor’s degree in Engineering"
            },
            {
              "requirement": "Bachelor’s degree in Mathematics"
            },
            {
              "requirement": "Bachelor’s degree in a related field"
            },
            {
              "requirement": "Master’s degree in Computer Science"
            },
            {
              "requirement": "Master’s degree in Engineering"
            },
            {
              "requirement": "Master’s degree in Mathematics"
            },
            {
              "requirement": "Master’s degree in a related field"
            }
          ],
          "group_type": "OR"
        }
      ],
      "group_type": "OR"
    },
    {
      "original_statement": "Minimum of 5 years of experience in machine learning, with at least 2 years in a leadership or tech lead role.",
      "group": [
        {
          "requirement": "Minimum of 5 years of experience in machine learning"
        },
        {
          "requirement": "At least 2 years in a leadership role"
        },
        {
          "requirement": "At least 2 years in a tech lead role"
        }
      ],
      "group_type": "AND"
    },
    {
      "original_statement": "Strong proficiency in programming languages such as Python, R, or Java, and experience with machine learning frameworks like TensorFlow, PyTorch, or Scikit-learn.",
      "group": [
        {
          "group": [
            {
              "requirement": "Strong proficiency in programming language Python"
            },
            {
              "requirement": "Strong proficiency in programming language R"
            },
            {
              "requirement": "Strong proficiency in programming language Java"
            }
          ],
          "group_type": "OR"
        },
        {
          "group": [
            {
              "requirement": "Experience with machine learning framework TensorFlow"
            },
            {
              "requirement": "Experience with machine learning framework PyTorch"
            },
            {
              "requirement": "Experience with machine learning framework Scikit-learn"
            }
          ],
          "group_type": "OR"
        }
      ],
      "group_type": "AND"
    },
    {
      "original_statement": "Proven track record of deploying machine learning models in production environments.",
      "requirement": "Proven track record of deploying machine learning models in production environments"
    },
    {
      "original_statement": "Excellent problem-solving skills and the ability to work independently and collaboratively in a fast-paced environment.",
      "group": [
        {
          "requirement": "Excellent problem-solving skills"
        },
        {
          "requirement": "The ability to work independently in a fast-paced environment"
        },
        {
          "requirement": "The ability to work collaboratively in a fast-paced environment"
        }
      ],
      "group_type": "AND"
    },
    {
      "original_statement": "Strong communication skills with the ability to convey complex technical concepts to non-technical stakeholders.",
      "requirement": "Strong communication skills with the ability to convey complex technical concepts to non-technical stakeholders"
    }
  ],
  "preferred_skills": [
    {
      "original_statement": "Experience with cloud platforms such as AWS, Google Cloud, or Azure for deploying machine learning solutions.",
      "group": [
        {
          "group": [
            {
              "skill": "Experience with cloud platform AWS for deploying machine learning solutions"
            },
            {
              "skill": "Experience with cloud platform Google Cloud for deploying machine learning solutions"
            },
            {
              "skill": "Experience with cloud platform Azure for deploying machine learning solutions"
            }
          ],
          "group_type": "OR"
        }
      ],
      "group_type": "OR"
    },
    {
      "original_statement": "Familiarity with big data technologies like Hadoop, Spark, or Kafka.",
      "group": [
        {
          "group": [
            {
              "skill": "Familiarity with big data technology Hadoop"
            },
            {
              "skill": "Familiarity with big data technology Spark"
            },
            {
              "skill": "Familiarity with big data technology Kafka"
            }
          ],
          "group_type": "OR"
        }
      ],
      "group_type": "OR"
    },
    {
      "original_statement": "Knowledge of deep learning techniques and natural language processing.",
      "group": [
        {
          "skill": "Knowledge of deep learning techniques"
        },
        {
          "skill": "Knowledge of natural language processing"
        }
      ],
      "group_type": "AND"
    },
    {
      "original_statement": "Experience with version control systems such as Git and continuous integration/continuous deployment (CI/CD) pipelines.",
      "group": [
        {
          "skill": "Experience with version control systems such as Git"
        },
        {
          "skill": "Experience with continuous integration/continuous deployment (CI/CD) pipelines"
        }
      ],
      "group_type": "AND"
    },
    {
      "original_statement": "Prior experience in a startup or agile development environment.",
      "group": [
        {
          "group": [
            {
              "skill": "Prior experience in a startup"
            },
            {
              "skill": "Prior experience in an agile development environment"
            }
          ],
          "group_type": "OR"
        }
      ],
      "group_type": "OR"
    }
  ],
  "benefits": [
    {
      "original_statement": "Competitive salary and performance-based bonuses.",
      "benefit": "Competitive salary"
    },
    {
      "original_statement": "Competitive salary and performance-based bonuses.",
      "benefit": "Performance-based bonuses"
    },
    {
      "original_statement": "Comprehensive health, dental, and vision insurance plans.",
      "benefit": "Comprehensive health insurance plans"
    },
    {
      "original_statement": "Comprehensive health, dental, and vision insurance plans.",
      "benefit": "Comprehensive dental insurance plans"
    },
    {
      "original_statement": "Comprehensive health, dental, and vision insurance plans.",
      "benefit": "Comprehensive vision insurance plans"
    },
    {
      "original_statement": "Flexible working hours and remote work opportunities.",
      "benefit": "Flexible working hours"
    },
    {
      "original_statement": "Flexible working hours and remote work opportunities.",
      "benefit": "Remote work opportunities"
    },
    {
      "original_statement": "Generous paid time off and holiday schedule.",
      "benefit": "Generous paid time off"
    },
    {
      "original_statement": "Generous paid time off and holiday schedule.",
      "benefit": "Generous holiday schedule"
    },
    {
      "original_statement": "Professional development opportunities and access to industry conferences.",
      "benefit": "Professional development opportunities"
    },
    {
      "original_statement": "Professional development opportunities and access to industry conferences.",
      "benefit": "Access to industry conferences"
    },
    {
      "original_statement": "Collaborative and inclusive company culture that values diversity and innovation.",
      "benefit": "Collaborative and inclusive company culture that values diversity and innovation"
    }
  ]
}

job_skills = {
    "Python": 30,
    "TensorFlow": 20,
    "PyTorch": 20,
    "Scikit-learn": 10,
    "AWS": 10,
    "Git": 10
}

industry_keywords = [
    "machine learning",
    "artificial intelligence",
    "AI",
    "data science"
]

score, explanation = get_match_score(cv, job, job_skills, industry_keywords)
print(f"Match Score: {score:.2f}%")
print("Explanation:", explanation)