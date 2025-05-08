# TalentMatch
<p align="right">
  <img src="https://github.com/user-attachments/assets/6efe34c2-9c49-425d-aac2-5673ae605748" alt="TalentMatch Logo" width="150"/>
</p>

### An AI-Powered CV–Job Description Matcher for Smarter HR Decisions  
*By Team SexthSense*

---

##  *Recognition*

**Second Place Winner** at the **AI Hackathon by Accesa**  

---


https://github.com/user-attachments/assets/3594ffd2-0bec-4f22-97ab-060244dcd9ed

---

##  *Overview*

**TalentMatch** is a smart web application designed to assist HR professionals in quickly and accurately identifying the best candidates for a job by matching CVs to job descriptions (JDs) and vice versa. It automates the manual, time-consuming resume screening process using a hybrid AI rule-based matching logic.

---

##  *Key Features*

- Upload, view, and delete multiple CVs and JDs (DOCX or PDF).
- Match one CV to multiple JDs or one JD to multiple CVs.
- Real-time similarity scoring based on multiple custom-weighted criteria.

---

##  *What Makes TalentMatch Stand Out*

1. **Smart Parsing & Logical Analysis**  
   Intelligent NLP parsing of documents using **Gemini 2.0** with custom prompts. Furthermore, sentences like "Python and Django or Java and SpringBoot" are parsed using logical structures to reflect intent and weigh combinations appropriately.

2. **Efficient & Targeted Use of AI**  
   We use AI-based semantic matching only where it adds meaningful value—reducing latency without sacrificing accuracy.

3. **ChromaDB for Precomputed Embeddings**  
   Sentence embeddings are generated when documents are uploaded, not at match time—ensuring fast response times.

4. **Custom Weighting & Section Delimitation**  
   Documents are processed into structured JSONs, enabling accurate and explainable matching.

5. **Modular Microservice Design**  
   Matching logic is isolated in a Python FastAPI microservice, ensuring scalability and independent upgrades.

---

##  *System Architecture*

- Frontend: Angular + Java
- Backend: Java (REST APIs)
- Matching Microservice: Python (FastAPI)
- Databases: Azure Database for MySQL + Chroma DB (for embeddings)
- Cloud Storage: Azure Blob Storage


---

##  *Matching Algorithm*

### Match CV → JD
A hybrid scoring model based on:

- **10%** Industry Knowledge  
- **30%** Job Skills *(input manually by HR via frontend)*  
- **60%** Actual Matching  
  - 30% Key Responsibilities  
  - 40% Required Qualifications  
  - 25% Preferred Skills  
  - 5% Foreign Languages  

### Match JD → CV  
Industry Knowledge and Job Skills are excluded; actual matching accounts for **100%** of the score.




---

## *Getting Started*

To run TalentMatch locally or in a development environment, follow the steps below for each major component of the system.

###  Prerequisites

- Python 3.12  
- Node.js (v16 or higher) and Angular CLI  
- Java 17+ and Maven  
- MySQL instance (e.g. Azure Database for MySQL)  
- Azure Blob Storage (for document uploads)  
- Chroma DB instance (for vector embeddings)

---

###  Python Microservice (Matching Logic)

**Path**: `service-python/cv_jd_matching/`

1. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

2. Use Python interpreter version **3.12**.

3. Run main.py


---

###  Java Backend (REST APIs)

**Path**: `service-java/`

1. Open the project in your preferred Java IDE (e.g., IntelliJ, Eclipse).

2. Configure the following file:
   - `src/main/resources/application.properties`  
      **Add credentials** for:
     - MySQL database
     - Azure Storage account
     - Any custom API keys

3. Build & run the Spring Boot application:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

---

###  Angular Frontend

**Path**: `service-UI/`

1. Install dependencies:
   ```bash
   npm install
   ```

2. Run the app:
   ```bash
   ng serve
   ```

3. The app will usually be available at:  
   `http://localhost:4200`

> Ensure that the frontend has environment configuration files (`environment.ts`) properly set up to point to the backend APIs and microservices.

---




##  *Future Improvements*

- **Explainable Match Scores**  
  Show HR professionals a detailed breakdown of each section's contribution to the final score.
  ![Screenshot 2025-05-08 142054](https://github.com/user-attachments/assets/29ddd49b-1314-4c5c-89d2-7216725ae81c)


- **Customizable Weighting**  
  Allow HR to adjust the importance of each CV/JD section to tailor results to their hiring strategy.
![Screenshot 2025-05-08 142010](https://github.com/user-attachments/assets/ef66e8ec-1fc6-48e6-98e0-79abb2fbf0b6)

  

- **Candidate Outreach Integration**  
  Enable the system to contact top-matched candidates directly through integrated communication channels.

---

