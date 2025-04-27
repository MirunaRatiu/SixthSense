
import json
import difflib
import re
import time
try:
    from sentence_transformers import SentenceTransformer
    from sklearn.metrics.pairwise import cosine_similarity
    import numpy as np
    SENTENCE_TRANSFORMER_AVAILABLE = True
except ImportError:
    SENTENCE_TRANSFORMER_AVAILABLE = False
    print("Warning: sentence-transformers not installed. Cosine similarity disabled.")

MODEL_NAME = 'all-MiniLM-L6-v2' # Model standard, rapid și eficient
#MODEL_NAME = 'paraphrase-mpnet-base-v2' # Model mai mare, potențial mai precis, dar mai lent
DEFAULT_COSINE_THRESHOLD = 0.65
DEFAULT_DIFFLIB_THRESHOLD = 0.7
TECHNICAL_KEYWORDS = [
    "aws", "azure", "google cloud", "docker", "jenkins", "kubernetes",
    "react", "angular", "vue.js", "nosql", "mysql", "sql", "java", "python",
    "spring boot", "html", "css", "git", "spring security", "lombok", "gradle",
    "thymeleaf", "agile", "express.js", "django", "restful api", "web services",
    "sharepoint", "powerapps", "power automate", "power bi", "rest api",
    "cloud computing", "cloud services", "cloud-based", "cloud-native", "cloud",
    "scrum", "hadoop", "spark", "kafka", "nlp", "natural language processing",
    "computer vision", "c++", "c#", "go", "golang", "rust", "typescript", "kotlin", "swift",
    "objective-c", "ruby", "php",  "scala", "dart", "perl", "bash", "shell",
    "next.js", "svelte", "solid.js", "jquery", "bootstrap", "tailwind", "material-ui",
    "webpack", "vite", "babel", "emotion", "styled-components",
    "flask", "fastapi", "nest.js", "asp.net", "asp.net core", "laravel", "symfony",
    "ruby on rails", "phoenix", "micronaut", "quarkus",
    "postgresql", "sql server", "sqlite", "oracle db", "mariadb", "cassandra", "redis",
    "neo4j", "elasticsearch", "dynamodb", "couchbase", "firestore", "bigquery", "influxdb",
    "gitlab ci", "circleci", "argo cd", "travis ci", "teamcity", "sonarqube",
    "terraform", "ansible", "helm", "prometheus", "grafana", "datadog", "new relic",
    "elk", "logstash", "filebeat", "zabbix", "pagerduty",
    "android", "ios", "react native", "flutter", "xamarin", "cordova", "ionic",
    "tensorflow", "pytorch", "scikit-learn", "keras", "pandas", "numpy", "matplotlib",
    "seaborn", "nltk", "spacy", "huggingface", "transformers", "openai", "llms", "mlflow",
    "hive", "pig", "airflow", "dbt", "snowflake", "databricks", "delta lake", "presto",
    "graphql", "grpc", "soap", "openapi", "swagger", "postman", "webhooks", "oauth2", "jwt",
    "junit", "testng", "selenium", "cypress", "playwright", "jest", "mocha", "chai",
    "robot framework", "karate", "postman", "figma", "adobe xd", "sketch", "invision", "zeplin",
    "github", "gitlab", "bitbucket", "svn", "mercurial", "jira", "confluence",
    "saml", "oauth", "tls", "ssl", "hashicorp vault", "penetration testing",
    "vulnerability scanning", "owasp", "crowdstrike", "fortinet",
    "ci/cd", "tdd", "bdd", "pair programming", "domain-driven design",
    "clean architecture", "hexagonal architecture", "microservices",
    "monolith", "serverless", "event-driven", "soa", "containerization",
    "tableau", "looker", "qlik", "superset", "metabase", "excel", "sap",
    "oracle erp", "salesforce", "microsoft dynamics",
    "json", "xml", "yaml", "toml", "protobuf", "avro", "http", "https",
    "linux", "unix", "windows", "macos", "wsl", "vmware", "virtualbox", "vagrant",
    "rabbitmq", "activemq", "mqtt", "zookeeper", "websockets", "apache pulsar",
    "assembly", "fortran", "cobol", "pascal", "ada", "scheme", "lisp", "elm", "clojure",
    "f#", "ocaml", "vhdl", "verilog", "matlab", "abap",
    "alpine.js", "htmx", "unocss", "uikit", "foundation", "bulma", "antd", "chakra ui",
    "remix", "nuxt.js", "astro", "qwik", "lit", "web components",
    "consul", "nomad", "vault", "k9s", "lens", "cilium", "linkerd", "istio", "envoy", "harbor",
    "tilt", "skaffold", "packer", "fluxcd",
    "opentelemetry", "jaeger", "tempo", "thanos", "loki", "splunk", "appdynamics",
    "spinnaker", "octopus deploy", "harness", "codefresh",
    "vertex ai", "azure ml", "sagemaker", "cognitiveservices", "whisper", "anthropic", "claude",
    "gemini", "llama", "mixtral", "mistral", "ollama",
    "timescaledb", "duckdb", "clickhouse", "trino", "mongodb atlas", "rockset",
    "firebase", "faunadb", "planet scale", "neo4j aura", "arangodb",
    "power query", "dax", "ssrs", "ssas", "qlik sense", "d3.js", "vega-lite", "plotly",
    "websphere", "jboss", "glassfish", "wildfly", "tibco", "ibm bpm", "camunda", "activiti",
    "curl", "wget", "httpie", "nmap", "tcpdump", "wireshark", "jq", "fzf", "tmux", "screen",
    "testflight", "firebase crashlytics", "expo", "detox", "appium",
    "coldfusion", "play framework", "struts", "tapestry", "dropwizard",
    "edge computing", "fog computing", "quantum computing", "blockchain", "web3",
    "ipfs", "ethereum", "solidity", "hardhat", "truffle", "metamask", "polygon",
    "notion", "monday.com", "clickup", "slack", "discord", "trello", "miro", "draw.io",
    "bare metal", "on-premise", "hybrid cloud", "multi-cloud", "edge nodes",
    "unity", "unreal engine", "godot", "three.js", "blender", "webgl", "directx", "opengl", "vulkan",
    "backstage", "porter", "gitness", "bicep", "opentofu", "cloud-init", "bpf", "ebpf",
    "eck", "argocd", "crossplane", "tanzu", "velero", "falco", "kyverno",
    "instana", "cloudwatch", "application insights", "xray", "datadog rum", "synthetics",
    "outsystems", "appgyver", "bubble", "retool", "adalo", "n8n", "xano", "budibase",
    "muleSoft", "boomi", "zapier", "make.com", "wsdl", "api gateway", "kong", "tyk", "ambassador",
    "wasm", "wasmtime", "wasi", "cloudflare workers", "vitepress", "turbopack", "rspack", "turso",
    "vanilla extract", "twin.macro", "framer motion", "motion one", "react query", "tanstack query",
    "zustand", "recoil", "valtio", "jotai", "xstate", "formkit", "react hook form", "vee-validate",
    "deepstream", "deepspeed", "ray", "langchain", "autogen", "llamaindex", "ctranslate2",
    "openvino", "onnx", "torchserve", "sagemaker pipelines", "replicate", "modal", "pandas-ai",
    "gradio", "streamlit", "autogpt", "agentic workflows", "semantic kernel", "openrouter", "cog",
    "apache beam", "apache flink", "apache iceberg", "apache druid", "datahub", "amundsen",
    "great expectations", "monte carlo", "trifacta", "openmetadata", "dbt cloud", "dagster",
    "gcp cloud run", "gcp cloud build", "aws lambda", "aws step functions", "aws fargate",
    "aws amplify", "azure functions", "azure pipelines", "azure synapse", "azure cosmos db",
    "snyk", "veracode", "checkmarx", "aquasec", "trivy", "grype", "burp suite", "openvas",
    "nessus", "nmap scripts", "seccomp", "tpm", "yubikey", "crowdsec", "suricata",
    "sap hana", "pega", "ab initio", "informatica", "talend", "ssis", "odoo", "axapta",
    "navision", "jbpm", "bpmn", "wso2", "jmeter", "gatling", "locust", "neoload",
    "near", "algorand", "cardano", "rust smart contracts", "solana", "cosmos", "chainlink",
    "alchemy", "moralis", "web3.js", "ethers.js",
    "quantconnect", "alpaca api", "ibkr", "pine script", "backtrader", "zipline", "quantlib",
    "open3d", "gazebo", "carla simulator", "unity ml-agents",
    "proto.io", "uxpin", "axure", "balsamiq", "marvelapp", "lunacy", "blocs", "webflow",
    "exa", "bat", "ripgrep", "fd", "glow", "neovim", "zsh", "oh-my-zsh", "starship", "powerlevel10k",
    "sap fiori", "sap abap", "sap ui5", "sap bw", "sap s/4hana",
    "sap pi/po", "sap btp", "sap bapi", "sap idoc", "sap cds views", "sap odata services",
    "stakeholder management", "project planning", "roadmap", "user stories", "kanban", "okrs",
    "risk management", "prince2", "pmp", "safe agile", "rfp", "rfq", "effort estimation",
    "team velocity", "jira align", "miro boards",
    "feature engineering", "model deployment", "autoML", "kaggle", "bayesian optimization",
    "data versioning", "mlops", "tensorflow serving", "torchscript", "onnx runtime",
    "clean code", "repository pattern", "service layer", "api rate limiting", "throttling",
    "caching strategies", "circuit breaker", "hmac", "open telemetry", "tracing",
    "bff", "server side rendering", "client side rendering", "hydration",
    "api integration", "responsive design", "mobile-first", "ssr caching", "isomorphic apps",
    "heuristics evaluation", "design tokens", "accessibility", "user journey mapping",
    "usability testing", "wireflows", "personas", "atomic design", "dark mode design",
    "contrast ratio", "mobile usability",
    "spfx", "graph api", "teams sdk", "adaptive cards", "mgt", "fluent ui",
    "office-js", "pnpjs", "database"
]

SYNONYM_MAP = {
    "teamwork": [
        "collaborate", "collaboration", "collaboratively", "team environment",
        "team player", "working in a team", "team setting", "group work",
        "team leader", "coordinated teams", "part of a team", "team member",
        "volunteer team", "blog team"  # Specific din CV-ul Sarey
    ],
    "collaboration": [  # Putem avea intrări separate sau le putem combina
        "collaborate", "collaboratively", "teamwork", "working together",
        "coordinated with", "partnered with"
    ],
    "leadership": [
        "team leader", "coordinated", "managed", "supervised", "lead",
        "leader", "communications manager", "team leading"  # Specific din CV
    ],
    "communication": [
        "communicated", "presentation", "public speaking", "spoken skills",
        "verbal communication", "written communication", "report writing",
        "active listening", "negotiated", "email correspondence", "communication", "communications"
    ],
    "problem solving": [
        "troubleshoot", "debugged", "resolved issues", "identified problems",
        "root cause analysis", "analytical thinking", "creative solutions",
        "problem resolution", "issue tracking"
    ],
    "adaptability": [
        "adapted", "flexible", "adjusted to change", "worked under pressure",
        "thrived in fast-paced environment", "quick learner", "handled change",
        "multitasking"
    ],
    "critical thinking": [
        "analyzed", "evaluated", "strategic thinking", "logic", "deduction",
        "reasoning", "assessed", "judgment", "decision making"
    ],
    "time management": [
        "prioritized", "met deadlines", "efficient", "schedule management",
        "handled multiple tasks", "on time", "organized workload"
    ],
    "creativity": [
        "innovative", "designed", "brainstormed", "created", "new approach",
        "conceptualized", "visualized", "developed new ideas"
    ],
    "empathy": [
        "empathetic", "understood others", "supportive", "emotionally aware",
        "patient", "helped peers", "interpersonal sensitivity"
    ],
    "conflict resolution": [
        "mediated", "resolved conflicts", "handled disputes", "de-escalated",
        "negotiated solutions", "intervened", "facilitated agreement"
    ],
    "collaboratively": ["team", "teamwork", "coordinated", "team leading", "team settings", "cooperation"],
    "rest api": ["rest", "api", "http api"],
    "integration techniques": ["integration", "middleware"],
    "agile": ["scrum", "kanban", "agile methodology"],
    "problem-solving": ["problem solving", "analytical skills", "troubleshooting", "manage situations"],
    "attention to detail": ["detail-oriented", "precision", "meticulous", "organization"],
    "technology industry": ["software", "development", "programming", "technology", "software engineering"],
    "consulting industry": ["consulting", "advisory"],
    "spark": ["apache spark"],
    "hadoop": ["apache hadoop"],
    "kafka": ["apache kafka"],
    "nlp": ["natural language processing"],
    "computer vision": ["cv", "vision techniques"],
    "sap": ["sap erp", "sap modules", "sap technologies"],
    "sap abap": ["advanced business application programming"],
    "sap fiori": ["fiori apps", "sap fiori apps", "fiori ux"],
    "sap ui5": ["openui5", "sap openui5"],
    "sap s/4hana": ["s4hana", "sap hana suite"],
    "mlops": ["machine learning operations", "ml operations", "ml devops"],
    "tensorflow": ["tf", "tensorflow library", "deep learning framework", "tensorflow model"],
    "pytorch": ["torch", "pytorch library", "deep learning framework"],
    "react": ["reactjs", "react.js", "react framework", "react library", "react components"],
    "angular": ["angularjs", "angular.js", "angular framework", "angular components"],
    "vue.js": ["vue", "vuejs", "vue framework", "vue components"],
    "jira": ["atlassian jira", "issue tracker"],
    "github": ["git", "github repository", "git version control", "version control", "gitHub"],
    "bitbucket": ["bitbucket repo", "bitbucket pipelines"],
    "gitlab": ["gitlab ci", "gitlab repository", "gitlab pipelines", "gitlab version control"],
    "jenkins": ["ci/cd tool", "jenkins pipelines", "continuous integration tool", "automated build system"],
    "kubernetes": ["k8s", "container orchestration"],
    "docker": ["containerization", "docker container"],
    "ci/cd": ["continuous integration", "continuous delivery", "continuous deployment"],
    "graphql": ["graph query language"],
    "restful api": ["rest api", "http api"],
    "jwt": ["json web token"],
    "oauth2": ["oauth", "authentication protocol"],
    "project planning": ["project management", "timeline planning", "resource planning"],
    "roadmap": ["product roadmap", "development roadmap"],
    "stakeholder management": ["stakeholder communication", "stakeholder coordination"],
    "usability testing": ["ux testing", "user testing"],
    "accessibility": ["a11y", "inclusive design"],
    "responsive design": ["mobile-friendly", "adaptive layout"],
    "mobile-first": [
        "mobile-first design", "mobile-first approach", "responsive mobile design"
    ],
    "event-driven": ["event based", "event-oriented"],
    "monolith": ["monolithic architecture"],
    "serverless": ["serverless architecture", "function-as-a-service"],
    "domain-driven design": ["ddd"],
    "hexagonal architecture": ["ports and adapters"],
    "clean architecture": ["uncle bob architecture", "layered architecture"],
    "open telemetry": ["otel", "observability"],
    "tracing": ["distributed tracing", "telemetry"],
    "dashboard": ["visualization", "reporting interface"],
    "power bi": ["bi dashboards", "microsoft bi"],
    "apache kafka": ["kafka", "streaming platform"],
    "tensorflow serving": ["tf serving"],
    "huggingface": ["transformers library", "hf"],
    "scrum": ["agile framework", "sprint planning", "agile", "kanban", "agile methodology"],
    "jira align": ["advanced roadmap", "portfolio planning"],
    "figma": ["ui prototyping", "figma design"],
    "miro boards": ["visual collaboration", "digital whiteboard"],
    "team velocity": ["sprint speed", "team output"],
    "adaptive cards": ["cards ui", "teams adaptive cards"],
    "react native": ["rn", "cross-platform react"],
    "flutter": ["dart ui framework", "cross-platform mobile"],
    "typescript": ["ts", "typed javascript"],
    "tailwind": ["tailwind css"],
    "material-ui": ["mui", "google material design"],
    "styled-components": ["sc", "css-in-js"],
    "react hook form": ["react forms", "form handling"],
    "zustand": ["react state management"],
    "redux": ["redux toolkit", "redux state"],
    "gcp": ["google cloud", "gcp platform"],
    "aws": ["amazon web services"],
    "azure": ["microsoft azure"],
    "bigquery": ["gcp bigquery", "google bigquery"],
    "snowflake": ["snowflake cloud", "snowflake warehouse"],
    "pandas": ["dataframe", "data analysis"],
    "numpy": ["numerical python", "array manipulation"],
    "matplotlib": ["python plotting", "data visualization"],
    "seaborn": ["statistical visualization"],
    "chatgpt": ["openai", "gpt model"],
    "llms": ["large language models"],
    "fastapi": ["python fast api", "fast api"],
    "flask": ["python microframework"],
    "data versioning": ["dvc", "ml data tracking"],
    "datahub": ["data catalog", "metadata management"],
    "dagster": ["data orchestration", "workflow scheduler"],
    "database": ["database design", "mysql", "sql", "relational database", "database management"],
    "web services": [
        "REST", "RESTful", "SOAP", "API development", "consumed APIs",
        "Web API", "Microservices", "HTTP endpoints", "service-oriented architecture",
        "exposed services", "built APIs", "integrated services", "JSON API",
        "XML services", "external APIs", "internal APIs", "web-based services",
        "web service integration"
    ],
    "cloud": [
        "cloud computing", "AWS", "Amazon Web Services", "Azure",
        "Microsoft Azure", "Google Cloud", "GCP", "cloud infrastructure",
        "deployed to cloud", "cloud-based", "cloud-native", "cloud environment",
        "cloud services", "cloud deployment", "serverless", "IaaS", "PaaS", "SaaS"
    ],
    "microservices": [
        "microservice architecture", "distributed services", "independent services",
        "microservices-based", "service decomposition", "modular architecture",
        "decomposed services", "RESTful microservices", "built microservices",
        "microservice-based system", "containerized services", "domain-driven design",
        "DDD architecture", "event-driven microservices", "microservices deployment", "microservice architecture", "msa"
    ],
    "programming": [
        "coding", "software development", "application development", "programming languages",
        "writing code", "developing software", "creating applications", "full-stack development",
        "frontend development", "backend development", "web development"
    ],
    "devops": [
        "CI/CD", "Continuous Integration", "Continuous Delivery", "deployment pipeline",
        "automation", "DevOps practices", "infrastructure as code", "Docker", "Kubernetes",
        "containerization", "cloud automation", "server management", "cloud infrastructure management"
    ],
    "machine learning": [
        "ML", "machine learning algorithms", "AI", "artificial intelligence", "deep learning",
        "neural networks", "reinforcement learning", "supervised learning", "unsupervised learning", "python", "r", "tensorflow", "pytorch", "scikit-learn", "keras", "pandas", "numpy", "ml", "ai",
                         "nlp", "computer vision"
    ],
    "data analysis": [
        "data mining", "data visualization", "data interpretation", "data processing", "statistical analysis",
        "data manipulation", "data science", "predictive analytics"
    ],
    "networking": [
        "network administration", "network management", "IP networking", "network security",
        "network protocols", "network architecture", "routing and switching", "LAN/WAN"
    ],
    "security": [
        "cybersecurity", "data security", "information security", "network security", "encryption",
        "firewalls", "threat analysis", "security protocols", "penetration testing"
    ],
    "automation": [
        "automated testing", "robotic process automation", "scripting", "automation tools",
        "Jenkins", "automation frameworks", "task automation", "unit testing", "system automation", "automated processes", "auto workflows", "automated workflows"
    ],
    "feature engineering": [
        "feature extraction", "data transformation", "data preprocessing", "feature selection"
    ],
    "model deployment": [
        "ml model deployment", "model serving", "model rollout", "production model deployment"
    ],
    "designing for web": ["html", "css", "javascript", "web design", "ui design", "ux design", "front end", "frontend",
                          "figma", "sketch", "photoshop", "illustrator", "responsive design", "angular", "react", "vue",
                          "thymeleaf"],  # Am adăugat Thymeleaf specific pt Sara
    "web development": ["html", "css", "javascript", "python", "java", "php", "node.js", "react", "angular", "vue",
                        "backend", "frontend", "full stack", "springboot", "mysql", "git", "gradle"],
    # Am adăugat SpringBoot, MySQL, Git, Gradle specifice pt Sara
    "front end development": ["html", "css", "javascript", "react", "angular", "vue", "typescript", "ui frameworks",
                              "responsive design", "frontend", "thymeleaf"],
    "back end development": ["java", "python", "node.js", "php", "ruby", "c#", "databases", "sql", "nosql", "api",
                             "rest", "springboot", "mysql", "git", "gradle"],
    "data visualization": ["tableau", "power bi", "d3.js", "matplotlib", "seaborn", "charts", "graphs", "reporting"],
    "cloud computing": ["aws", "azure", "gcp", "google cloud", "amazon web services", "microsoft azure",
                        "cloud deployment", "serverless", "docker", "kubernetes", "ci/cd", "terraform", "cloud", "cloud services", "cloud platform", "cloud architecture"],
    "agile methodology": ["agile", "scrum", "kanban", "jira", "sprint", "lean"],
    "database management": ["sql", "mysql", "postgresql", "oracle", "mongodb", "nosql", "database design",
                            "query optimization"],
"databases": ["sql", "mysql", "postgresql", "oracle", "mongodb", "nosql", "database design",
                            "query optimization"],
"mobile": [
    "flutter",
    "dart ui framework",
    "cross-platform mobile",
    "react native",
    "javascript mobile framework",
    "swift",
    "ios development",
    "iphone app development",
    "kotlin",
    "android development",
    "android apps",
    "java android",
    "android native development",
    "java mobile apps",
    "objective-c",
    "legacy ios development",
    "xamarin",
    "c# mobile apps",
    "android studio",
    "android development environment",
    "xcode",
    "ios development environment",
    "firebase",
    "mobile backend services",
    "authentication and database for mobile apps",
    "rest api",
    "api integration mobile",
    "backend communication mobile apps"
  ]

}
def normalize(text):
    """Normalizează textul: minuscule, elimină spațiile de la început/sfârșit."""
    return text.lower().strip() if isinstance(text, str) else ""

def split_terms(text):
    """Împarte textul în termeni, inclusiv elemente cu caractere speciale."""
    # Spargem pe orice spațiu sau delimitator de propoziție, dar păstrăm termeni cu . sau +
    return re.findall(r'\w[\w\+\#\.]*', text.lower())

def extract_text(item):
    for key in ["skill", "task", "requirement"]:
        if key in item:
            text = normalize(item[key])
            text_terms = split_terms(text)
            skill_terms = [normalize(kw) for kw in TECHNICAL_KEYWORDS]

            if any(term in text_terms for term in skill_terms):
                print(f"Extracting TECHNICAL text: '{text}'")
                return text
            else:
                print(f"Extracting NON-TECHNICAL text: '{text}'")
                return text
    print(f"Warning: No skill/task/requirement key found in item: {item}")
    return ""

def prioritized_flatten(cv):
    """
    Extrage și prioritizează textul din diferite secțiuni ale CV-ului,
    incluzând descrierile din experiență cu prioritate dedicată.
    """
    print("\n--- Extragere și Prioritizare Text CV ---")

    # 1. Technical Skills (Prioritate maximă)
    tech_skills = [normalize(item.get("skill", "")) for item in cv.get("technical_skills", [])]
    print(f"Technical Skills: {len(tech_skills)} items")

    # 2. Project Experience (Tehnologii + Descrieri)
    project_techs = []
    project_descriptions = []
    print(f"Processing Project Experience ({len(cv.get('project_experience', []))} items)...")
    for i, proj in enumerate(cv.get("project_experience", [])):
        # Extrage Tehnologii
        techs = [normalize(tech) for tech in proj.get("technologies", [])]
        project_techs.extend(techs)
        # Extrage Descrierea
        description = proj.get("description", "")
        if isinstance(description, str):
            project_descriptions.append(normalize(description))
        elif isinstance(description, list): # Dacă descrierea e o listă de string-uri
            project_descriptions.extend([normalize(d) for d in description if isinstance(d, str)])
        # print(f"  Project {i+1}: Found {len(techs)} techs, Description length: {len(description) if isinstance(description, (str, list)) else 0}")
    print(f"Project Technologies: {len(project_techs)} items")
    # print(f"Project Descriptions: {len(project_descriptions)} items") # Afișare opțională

    # 3. Work Experience (Tehnologii + Descrieri)
    work_techs = []
    work_descriptions = []
    print(f"Processing Work Experience ({len(cv.get('work_experience', []))} items)...")
    for i, job in enumerate(cv.get("work_experience", [])):
        # Extrage Tehnologii
        techs = [normalize(tech) for tech in job.get("technologies", [])]
        work_techs.extend(techs)
        # Extrage Descrierea
        description = job.get("description", "")
        if isinstance(description, str):
            work_descriptions.append(normalize(description))
        elif isinstance(description, list): # Dacă descrierea e o listă de string-uri
            work_descriptions.extend([normalize(d) for d in description if isinstance(d, str)])
        # print(f"  Work {i+1}: Found {len(techs)} techs, Description length: {len(description) if isinstance(description, (str, list)) else 0}")
    print(f"Work Technologies: {len(work_techs)} items")
    # print(f"Work Descriptions: {len(work_descriptions)} items") # Afișare opțională

    # Combinăm descrierile și eliminăm duplicatele
    experience_descriptions = sorted(list(set(project_descriptions + work_descriptions)))
    print(f"Combined Experience Descriptions (Unique): {len(experience_descriptions)} items")


    # 4. Certifications (Tehnologii + Nume)
    cert_techs = []
    cert_names = []
    print(f"Processing Certifications ({len(cv.get('certifications', []))} items)...")
    for i, cert in enumerate(cv.get("certifications", [])):
        techs = [normalize(tech) for tech in cert.get("technologies", [])]
        cert_techs.extend(techs)
        if "name" in cert:
             name = normalize(cert.get("name", ""))
             cert_names.append(name)
    print(f"Certification Technologies: {len(cert_techs)} items")
    print(f"Certification Names: {len(cert_names)} items")


    # 5. Fallback Texts (Educație, Altele, și *alte* câmpuri din experiență/certificări)
    fallback_texts = []
    print("Processing Fallback Texts (Education, Others, etc.)...")
    # Includem și numele certificatelor în fallback pentru potrivire generală
    fallback_texts.extend(cert_names)

    # Iterăm prin secțiuni, dar EXCLUDEM explicit 'technologies' și 'description'
    # din Project/Work Experience, deoarece sunt deja tratate cu prioritate mai mare.
    sections_for_fallback = {
        "education": ["institution", "degree", "field_of_study"], # Adaugă câmpuri relevante
        "others": None, # Procesează toate valorile din 'others'
        "project_experience": ["title", "description"], # Extrage doar titlul din proiecte ca fallback
        "work_experience": ["title", "company", "description"], # Extrage titlu/companie din work ca fallback
        "certifications": ["institution"] # Extrage instituția certificatului ca fallback (numele e deja adăugat)
        # Adaugă/elimină secțiuni/câmpuri după nevoie
    }

    for section_key, relevant_keys in sections_for_fallback.items():
        entries = cv.get(section_key, [])
        if not entries: continue

        if isinstance(entries, dict): # Cazul 'others'
            # Procesăm toate valorile din dicționarul 'others'
            if section_key == 'others':
                for sub_key, sub_value in entries.items():
                    if isinstance(sub_value, str):
                        fallback_texts.append(normalize(sub_value))
                    elif isinstance(sub_value, list):
                        for item in sub_value:
                             if isinstance(item, str):
                                fallback_texts.append(normalize(item))
                             elif isinstance(item, dict):
                                fallback_texts.extend([normalize(v) for v in item.values() if isinstance(v, str)])
            else: # Convertim alte dicționare în liste (deși structura standard e listă)
                entries = [entries]

        if isinstance(entries, list): # Procesăm liste (cazul standard pt majoritatea secțiunilor)
             for entry in entries:
                if isinstance(entry, dict):
                    # Extragem doar cheile relevante specificate, dacă există
                    keys_to_extract = relevant_keys if relevant_keys is not None else entry.keys()
                    for key in keys_to_extract:
                        value = entry.get(key)
                        if isinstance(value, str):
                            fallback_texts.append(normalize(value))
                        elif isinstance(value, list):
                            for item in value:
                                if isinstance(item, str):
                                    fallback_texts.append(normalize(item))
                                # Extindem și din dicționare din liste (ex: Contact Information în Others)
                                elif isinstance(item, dict) and section_key == 'others':
                                     fallback_texts.extend([normalize(v) for v in item.values() if isinstance(v, str)])
                elif isinstance(entry, str): # Unele secțiuni pot fi liste de string-uri direct
                    fallback_texts.append(normalize(entry))


    # Eliminăm duplicatele și textele goale din fallback final
    fallback_texts = sorted(list(set(filter(None, fallback_texts))))
    print(f"Fallback Texts (Diverse - Unique): {len(fallback_texts)} items")
    print("--- Sfârșit Extragere CV ---")

    # Returnăm sursele în ordinea priorității dorite:
    # 1. Skill-uri Tehnice Directe
    # 2. Tehnologii Proiecte
    # 3. Tehnologii Work
    # 4. Tehnologii Certificări
    # 5. Descrieri Experiență (Proiecte + Work) <- NOU
    # 6. Texte Fallback (Educație, Altele, Nume Certificări, Titluri etc.)
    prioritized_sources = [
        tech_skills,
        project_techs,
        work_techs,
        cert_techs,
        experience_descriptions, # Lista nouă inserată aici
        fallback_texts
    ]
    # Verificăm că toate sunt liste de stringuri
    for i, source_list in enumerate(prioritized_sources):
        if not isinstance(source_list, list):
            print(f"Warning: Sursa {i} nu este o listă!")
        # Optional: Verifică tipul elementelor
        # if source_list and not all(isinstance(item, str) for item in source_list):
        #    print(f"Warning: Sursa {i} conține elemente non-string!")

    return prioritized_sources

# --- IMPORTANT ---
# Trebuie să actualizezi și maparea numelor surselor în `is_text_match_priority`


# --- Funcții de Potrivire ---

def load_model(model_name=MODEL_NAME):
    """Încarcă modelul SentenceTransformer o singură dată."""
    if not SENTENCE_TRANSFORMER_AVAILABLE:
        print("SentenceTransformer nu este disponibil. Modelul nu poate fi încărcat.")
        return None
    try:
        print(f"\nÎncărcare model SentenceTransformer ('{model_name}')...")
        start_time = time.time()
        model = SentenceTransformer(model_name)
        end_time = time.time()
        print(f"Model încărcat cu succes în {end_time - start_time:.2f} secunde.")
        return model
    except Exception as e:
        print(f"Eroare la încărcarea modelului SentenceTransformer: {e}")
        return None

def compute_cosine_similarity(text1, text2, model):
    """
    Calculează similaritatea cosinus între două texte folosind un model preîncărcat.

    Args:
        text1 (str): Primul text.
        text2 (str): Al doilea text.
        model: Obiectul model SentenceTransformer preîncărcat.

    Returns:
        float: Scorul de similaritate cosinus (între -1.0 și 1.0),
               sau -1.0 dacă modelul nu este disponibil sau apare o eroare.
    """
    if not model or not SENTENCE_TRANSFORMER_AVAILABLE:
        # print("Debug: Model sau bibliotecă indisponibilă pentru cosine similarity.")
        return -1.0 # Indicăm că nu s-a putut calcula
    if not text1 or not text2:
        # print(f"Debug: Text gol furnizat pentru cosine similarity ('{text1}', '{text2}').")
        return 0.0 # Similaritate zero pentru texte goale

    try:
        # Encodăm ambele texte
        embeddings = model.encode([text1, text2], convert_to_tensor=True)
        # Calculăm similaritatea cosinus
        sim_score = cosine_similarity(embeddings[0].reshape(1, -1), embeddings[1].reshape(1, -1))[0][0]
        # Convertim din tensor în float scalar dacă e necesar (depinde de versiunea sklearn/torch)
        if not isinstance(sim_score, float):
             sim_score = sim_score.item()
        # print(f"Cosine similarity între '{text1}' și '{text2}': {sim_score:.3f}") # Afișare opțională pentru debug
        return sim_score
    except Exception as e:
        print(f"Eroare la calculul cosine similarity între '{text1}' și '{text2}': {e}")
        return -1.0 # Indicăm eroare

def is_text_match_priority(needle, sources, model, cache=None,
                           cosine_threshold=DEFAULT_COSINE_THRESHOLD,
                           difflib_threshold=DEFAULT_DIFFLIB_THRESHOLD):
    """
    Verifică dacă 'needle' (text din JD) se potrivește cu vreun text din 'sources' (liste de texte din CV),
    folosind o strategie prioritarizată și un model preîncărcat.

    Args:
        needle (str): Textul de căutat (din JD), normalizat.
        sources (list[list[str]]): Lista de liste de texte din CV, în ordinea priorității.
        model: Modelul SentenceTransformer preîncărcat (poate fi None).
        cache (dict): Cache pentru rezultate (opțional).
        cosine_threshold (float): Pragul pentru similaritatea cosinus.
        difflib_threshold (float): Pragul pentru similaritatea secvențială.

    Returns:
        bool: True dacă se găsește o potrivire suficientă, False altfel.
    """
    needle = normalize(needle)
    if not needle:
        # print("Debug: Needle gol, nu se poate potrivi.")
        return False

    cache = cache if cache is not None else {}
    if needle in cache:
        # print(f"Folosind cache pentru '{needle}': {'✅ Potrivire' if cache[needle] else '❌ Nepotrivire'}")
        return cache[needle]

    print(f"\n-- Căutare potrivire pentru: '{needle}' --")

    # 1. Verificare Sinonime (potrivire cuvânt întreg)
    # Găsim cheia din SYNONYM_MAP care se potrivește cu `needle` (ca un cuvânt întreg)
    matched_synonym_key = None
    synonyms_to_check = []
    for key, synonyms in SYNONYM_MAP.items():
        if re.search(r'\b' + re.escape(normalize(key)) + r'\b', needle, re.IGNORECASE):
             matched_synonym_key = key
             synonyms_to_check = [normalize(s) for s in synonyms]
             print(f"  (Potrivire sinonim: '{needle}' conține cheia '{key}'. Căutăm {synonyms_to_check})")
             break # Am găsit o cheie, nu mai căutăm altele

    if matched_synonym_key:
        for i, source in enumerate(sources):
            source_name = ["TechSkills", "ProjectTech", "WorkTech", "CertTech", "ExperienceDesc","FallbackTexts"][i]
            for text in source:
                for syn in synonyms_to_check:
                    if re.search(r'\b' + re.escape(syn) + r'\b', text, re.IGNORECASE):
                        print(f"  ✅ Potrivire SINONIM: '{syn}' găsit în '{text}' (Sursa: {source_name}) pentru '{needle}'.")
                        cache[needle] = True
                        return True

    # 2. Verificare Cuvinte Cheie Tehnice directe (potrivire cuvânt întreg)
    # Extragem cuvintele cheie tehnice *din needle*
    terms_in_needle = split_terms(needle)
    needle_keywords = [kw for kw in TECHNICAL_KEYWORDS if normalize(kw) in terms_in_needle]

    if needle_keywords:
        print(f"  (Cuvinte cheie tehnice în needle: {needle_keywords})")
        for i, source in enumerate(sources):
            source_name = ["TechSkills", "ProjectTech", "WorkTech", "CertTech", "ExperienceDesc","FallbackTexts"][i]
            for text in source:
                terms_in_text = split_terms(text)
                for keyword in needle_keywords:
                    # Căutăm cuvântul cheie exact (ca un cuvânt întreg) în textul sursă
                    if normalize(keyword) in terms_in_text:
                        print(f"  ✅ Potrivire CUVÂNT CHEIE: '{keyword}' găsit în '{text}' (Sursa: {source_name}) pentru '{needle}'.")
                        cache[needle] = True
                        return True

    # 3. Potrivire Semantică (Cosine Similarity) și Secvențială (Difflib)
    # Iterăm prin surse în ordinea priorității
    for i, source in enumerate(sources):
        source_name = ["TechSkills", "ProjectTech", "WorkTech", "CertTech","ExperienceDesc", "FallbackTexts"][i]
        print(f"  Verificare Sursa: {source_name} ({len(source)} elemente)")
        for text in source:
            if not text: continue # Skip texte goale din surse

            # 3a. Similaritate Cosinus (dacă modelul e disponibil)
            if model:
                sim_score = compute_cosine_similarity(needle, text, model)
                # print(f"    -> Cosine sim. cu '{text}': {sim_score:.3f} (prag: {cosine_threshold})") # Debug detaliat
                if sim_score >= cosine_threshold:
                    print(f"  ✅ Potrivire SEMANTICĂ (Cosine): Scorul {sim_score:.3f} >= {cosine_threshold} între '{needle}' și '{text}' (Sursa: {source_name}).")
                    cache[needle] = True
                    return True

            # 3b. Similaritate Secvențială (Difflib)
            ratio = difflib.SequenceMatcher(None, needle, text).ratio()
            # print(f"    -> Difflib ratio cu '{text}': {ratio:.3f} (prag: {difflib_threshold})") # Debug detaliat
            if ratio >= difflib_threshold:
                print(f"  ✅ Potrivire SECVENȚIALĂ (Difflib): Ratio {ratio:.3f} >= {difflib_threshold} între '{needle}' și '{text}' (Sursa: {source_name}).")
                cache[needle] = True
                return True

            # 3c. Potrivire Substring (cuvânt întreg) - ca ultimă soluție
            # Verificăm dacă needle *ca un cuvânt întreg* apare în text
            # Acest lucru prinde cazuri simple cum ar fi "Java" în "Intern Java Developer"
            if re.search(r'\b' + re.escape(needle) + r'\b', text, re.IGNORECASE):
                print(f"  ✅ Potrivire SUBSTRING (Cuvânt Întreg): '{needle}' găsit în '{text}' (Sursa: {source_name}).")
                cache[needle] = True
                return True

    # Dacă am ajuns aici, nu s-a găsit nicio potrivire suficientă
    print(f"  ❌ Nepotrivire finală pentru '{needle}' în toate sursele.")
    cache[needle] = False
    return False

def evaluate_group_preferred(group_data, cv, model, cache, depth=0):
    """Evaluează un grup de skill-uri preferate (AND/OR)."""
    group_type = group_data.get("group_type", "OR") # Implicit OR dacă nu e specificat
    # Gestionăm structura unde un item poate fi direct un skill sau un grup
    if "group" in group_data:
        group_items = group_data.get("group", [])
    else:
        # Dacă nu există cheia 'group', considerăm că obiectul curent este un singur item (skill)
        # Îl punem într-o listă pentru a uniformiza procesarea
        group_items = [group_data]
        # Dacă itemul nu are group_type specificat, îl tratăm ca un item singular (nu AND/OR)
        # Dar pentru a menține logica, vom considera tipul default OR, care va evalua acest unic item.

    # Extragem sursele de text din CV o singură dată pentru acest grup
    # (Optimizare: Am putea face asta o singură dată per evaluare totală, dar așa e mai clar)
    prioritized_sources = prioritized_flatten(cv) # Atenție: Afișează mesajele de extragere de fiecare dată

    indent = "  " * depth
    print(f"{indent}Evaluare grup preferat (Tip: {group_type}, Nr. iteme: {len(group_items)}):")

    if not group_items:
         print(f"{indent}Grup gol, scor: 0.00%")
         return 0.0

    if group_type == "AND":
        results = []
        for i, item in enumerate(group_items):
            print(f"{indent}  Procesare item AND {i + 1}/{len(group_items)}")
            if "group" in item or "group_type" in item: # Verificăm dacă e un sub-grup
                print(f"{indent}    -> Sub-grup detectat...")
                nested_score = evaluate_group_preferred(item, cv, model, cache, depth + 1)
                print(f"{indent}    <- Scor sub-grup: {nested_score * 100:.2f}%")
                results.append(nested_score)
            else:
                # Este un item singular (skill/task/requirement)
                text_to_match = extract_text(item)
                if not text_to_match:
                    print(f"{indent}    Item gol/invalid, sărim peste: {item}")
                    # Într-un grup AND, un item gol poate fi interpretat diferit.
                    # Aici alegem să nu penalizăm scorul (nu adăugăm 0), dar nici să nu contribuie.
                    # Alternativ, am putea adăuga 0.0, făcând grupul AND să eșueze.
                    # Sau am putea să îl considerăm True (ex: "cunoștințe generale" implicit îndeplinite).
                    # Alegerea depinde de logica de business dorită. Aici îl ignorăm.
                    continue # Trecem la următorul item

                matched = is_text_match_priority(text_to_match, prioritized_sources, model, cache)
                print(f"{indent}    Verificare item: '{text_to_match}' -> {'✅ Potrivit' if matched else '❌ Nepotrivit'}")
                results.append(1.0 if matched else 0.0)

        if not results: # Dacă toate itemele au fost goale/ignorate
             print(f"{indent}Grup AND nu a avut iteme valide, scor: 0.00%")
             score = 0.0
        else:
             score = sum(results) / len(results)
             print(f"{indent}Scor final grup AND: {score * 100:.2f}% ({sum(results):.0f}/{len(results)} potriviri)")
        return score

    elif group_type == "OR":
        for i, item in enumerate(group_items):
            print(f"{indent}  Procesare item OR {i + 1}/{len(group_items)}")
            if "group" in item or "group_type" in item: # Verificăm dacă e un sub-grup
                print(f"{indent}    -> Sub-grup detectat...")
                nested_score = evaluate_group_preferred(item, cv, model, cache, depth + 1)
                print(f"{indent}    <- Scor sub-grup: {nested_score * 100:.2f}%")
                if nested_score > 0: # O potrivire în sub-grupul OR este suficientă
                    print(f"{indent}Scor final grup OR: 100.00% (potrivire în sub-grup)")
                    return 1.0
            else:
                # Este un item singular (skill/task/requirement)
                text_to_match = extract_text(item)
                if not text_to_match:
                    print(f"{indent}    Item gol/invalid, sărim peste: {item}")
                    continue # Trecem la următorul item

                matched = is_text_match_priority(text_to_match, prioritized_sources, model, cache)
                print(f"{indent}    Verificare item: '{text_to_match}' -> {'✅ Potrivit' if matched else '❌ Nepotrivit'}")
                if matched:
                    print(f"{indent}Scor final grup OR: 100.00% (potrivire item individual)")
                    return 1.0 # Short-circuit: o potrivire este suficientă

        # Dacă am ajuns aici, niciun item din grupul OR nu a fost potrivit
        print(f"{indent}Scor final grup OR: 0.00% (niciun item potrivit)")
        return 0.0

    else:
        print(f"Tip de grup necunoscut: {group_type}. Se returnează 0.0.")
        return 0.0


def score_only_preferred_skills(jd_preferred_skills_list, cv, model):
    """Calculează scorul total pentru skill-urile preferate."""
    if not jd_preferred_skills_list:
        print("Lista de skill-uri preferate este goală.")
        return 0.0

    scores = []
    # Inițializăm cache-ul pentru potriviri text
    match_cache = {}

    print("\n===== Evaluare Skill-uri Preferate =====")
    for i, group_data in enumerate(jd_preferred_skills_list):
        print(f"\n--- Evaluare Grup Principal {i+1} ---")
        # Verificăm dacă 'group_data' este un dicționar (format așteptat)
        if isinstance(group_data, dict):
             score = evaluate_group_preferred(group_data, cv, model, match_cache)
             scores.append(score)
             print(f"--- Scor Grup Principal {i+1}: {score * 100:.2f}% ---")
        else:
             print(f"Elementul {i+1} din 'preferred_skills' nu este un dicționar valid, va fi ignorat: {group_data}")


    if not scores:
        print("Nu s-au putut calcula scoruri pentru niciun grup.")
        return 0.0

    total_score = sum(scores) / len(scores)
    print("\n===== Rezumat Scoruri Preferate =====")
    for i, score in enumerate(scores):
        print(f"Grup {i+1}: {score * 100:.2f}%")
    print("===================================")
    return total_score


# Job Description
jd_json = json.dumps({
    "preferred_skills": [
    {
      "original_statement": "Experience with front-end development technologies such as HTML, CSS, and JavaScript.",
      "group": [
        {
          "group": [
            {
              "skill": "Experience with front-end development technologies such as HTML"
            },
            {
              "skill": "Experience with front-end development technologies such as CSS"
            },
            {
              "skill": "Experience with front-end development technologies such as JavaScript"
            }
          ],
          "group_type": "AND"
        }
      ],
      "group_type": "AND"
    },
    {
      "original_statement": "Familiarity with agile methodologies and working in an agile environment.",
      "group": [
        {
          "group": [
            {
              "skill": "Familiarity with agile methodologies"
            },
            {
              "skill": "Working in an agile environment"
            }
          ],
          "group_type": "AND"
        }
      ],
      "group_type": "AND"
    },
    {
      "original_statement": "Knowledge of accessibility standards and best practices in design.",
      "skill": "Knowledge of accessibility standards and best practices in design"
    },
    {
      "original_statement": "Experience in designing for a variety of platforms, including web, mobile, and emerging technologies like AR/VR.",
      "group": [
        {
          "group": [
            {
              "skill": "Experience in designing for web"
            },
            {
              "skill": "Experience in designing for mobile"
            },
            {
              "skill": "Experience with databases"
            }
          ],
          "group_type": "AND"
        }
      ],
      "group_type": "AND"
    }
  ],
})

# CV
sara_cv_dict = {
    "technical_skills": [
  {"skill": "C"},
  {"skill": "C++"},
  {"skill": "Python"},
  {"skill": "Java"},
  {"skill": "SQL"},
  {"skill": "HTML"},
  {"skill": "CSS"},
  {"skill": "VHDL"},
  {"skill": "Django"},
  {"skill": "Arduino"},
  {"skill": "Jupyter Notebook"},
  {"skill": "Vivado"},
  {"skill": "Vitis"},
  {"skill": "Git"}
],

    "foreign_languages": [
      {"language": "Romanian", "proficiency": "mother tongue"},
      {"language": "English", "proficiency": "B2/Upper Intermediate level for CEFR"}
    ],
"education": [
      {
        "institution": "Faculty of Automation and Computer Science, Technical University of Cluj-Napoca",
        "degree": "Bachelor",
        "field_of_study": "Automation and Computer Science",
        "period": {
          "start_date": "2022-10",
          "end_date": "Present"
        }
      },
      {
        "institution": "Colegiul National \"Mihai Eminescu\" Petrosani",
        "field_of_study": "Nature Sciences Intensive English",
        "period": {
          "start_date": "2018-09",
          "end_date": "2022-07"
        }
      }
    ],
  "certifications": [],
  "project_experience": [
    {
      "title": "TicketFever: Event Reservation Platform",
      "description": "Developed a web application for event reservations, allowing users to book, manage, and get notified about event changes.",
      "technologies": ["Python", "Django", "HTML", "CSS", "JavaScript"]
    },
    {
      "title": "Intelligent system for miner's helmet",
      "description": "Built a system to protect coal mine workers from health hazards and possible explosions. It detects dangerous gases (methane or carbon monoxide), takes measures against high temperatures and features an automatic lighting system.",
      "technologies": ["C++", "Arduino"]
    },
    {
      "title": "Embedded Software for Image Binarization",
      "description": "Developed an efficient image binarization system that converts grayscale images into images with only black and white values, by programming the FPGA on a SoC board (Pynq Z1).",
      "technologies": ["VHDL", "Python", "Vivado", "Jupyter Notebook"]
    }
  ],
  "work_experience": [
    {
      "type": "competition",
      "name": "WiDS Datathon++ University Edition",
      "organization": "University Edition",
      "period": {
        "start_date": "2025-02",
        "end_date": "Present"
      },
      "description": "Collaborating in a team to apply Machine Learning knowledge on real data sets."
    },
    {
      "type": "competition",
      "name": "Cloudflight Coding Contest",
      "period": {
        "date": "2023-10"
      },
      "description": "Learnt to work and communicate effectively in a team on the spot."
    },
    {
      "type": "competition",
      "name": "Cloudflight Coding Contest",
      "period": {
        "date": "2024-04"
      },
      "description": "Learnt to work and communicate effectively in a team on the spot."
    },
    {
      "type": "job",
      "title": "Cashier",
      "company": "UNTOLD SRL",
      "period": {
        "start_date": "2023-08",
        "end_date": "2023-08"
      },
      "description": "Gained experience with working with customers under pressure."
    },
    {
      "type": "volunteer",
      "title": "Volunteer",
      "company": "Red Cross Romania",
      "period": {
        "start_date": "2020-01",
        "end_date": "2021-12"
      },
      "description": "Developed teamwork and crisis response skills through first aid training."
    }
  ],
  "others": {

    "contact_information": {
      "email": "lupulescu.lorenal7@gmail.com",
      "phone_number": "0731127650"
    },
    "summary": [
      "I approach challenges with creativity, patience, persistence and a problem-solving mindset.",
      "Always eager to learn and grow, I embrace every opportunity to develop my skills and deliver exceptional results!",
      "When I’m not immersed in the world of ones and zeros you can find me behind my camera capturing life’s beauty or outdoors seeking adventure through hiking or running."
    ]
  }
}

# Run
jd = json.loads(jd_json)
sentence_model = load_model()
total_score = score_only_preferred_skills(jd["preferred_skills"], sara_cv_dict, sentence_model)
print(f"Total Match Score: {total_score * 100:.2f}%")
