package com.cv_jd_matching.HR;

import com.cv_jd_matching.HR.dto.CvDTO;
import com.cv_jd_matching.HR.dto.CvViewDTO;
import com.cv_jd_matching.HR.entity.Cv;
import com.cv_jd_matching.HR.mapper.CvMapper; // Asigură-te că importul este corect
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CvMapperTest {

    private Cv cvEntity;
    private String technicalSkillsData;
    private String foreignLanguagesData;
    private String educationData;

    @BeforeEach
    void setUp() {
        // Folosim exact formatul de date specificat
        technicalSkillsData = "[{skill=Python, level=4}, {skill=TensorFlow, level=4}, {skill=JavaScript, level=3}, {skill=ReactJS, level=3}, {skill=AWS SageMaker, level=2}, {skill=Docker, level=2}, {skill=SQL, level=3}, {skill=PostgreSQL, level=3}, {skill=Figma, level=2}, {skill=Adobe XD, level=2}]";
        foreignLanguagesData = "[{language=English, proficiency=C1}, {language=Spanish, proficiency=B1}]";
        educationData = "[{institution=University of Bucharest, degree=Bachelor, period={duration=4 years}, technologies=[]}, {institution=University Politehnica of Bucharest, degree=Master, period={duration=2 years}, technologies=[]}]";

        cvEntity = new Cv();
        cvEntity.setId(1);
        cvEntity.setFileName("cv_test_actual.pdf");
        cvEntity.setPathName("http://example.com/sas/cv_test_actual.pdf?token=123");
        cvEntity.setName("Vasile Pop");

        cvEntity.setTechnicalSkills(technicalSkillsData);
        cvEntity.setForeignLanguages(foreignLanguagesData);
        cvEntity.setEducation(educationData);
        cvEntity.setCertifications("[{name=Cert1}]"); // Exemplu simplu
        cvEntity.setProjectExperience("[{projectName=Proj1}]"); // Exemplu
        cvEntity.setWorkExperience("[{company=Comp1}]"); // Exemplu
        cvEntity.setOthers("Alte informatii");
    }

    @Test
    void testMapEntityToDTO() {
        // verificam daca DTO-ul conține string-urile exact asa cum sunt în entitate
        CvDTO cvDTO = CvMapper.mapEntityToDTO(cvEntity);

        assertNotNull(cvDTO);
        assertEquals(cvEntity.getId(), cvDTO.getId());
        assertEquals(technicalSkillsData, cvDTO.getTechnicalSkills());
        assertEquals(foreignLanguagesData, cvDTO.getForeignLanguages());
        assertEquals(educationData, cvDTO.getEducation());
        assertEquals("[{name=Cert1}]", cvDTO.getCertifications());
        assertEquals("[{projectName=Proj1}]", cvDTO.getProjectExperience());
        assertEquals("[{company=Comp1}]", cvDTO.getWorkExperience());
        assertEquals("Alte informatii", cvDTO.getOthers());
    }

    @Test
    void testMapEntityToViewDTO_SuccessfulParsing() {
        CvViewDTO cvViewDTO = CvMapper.mapEntityToViewDTO(cvEntity);

        assertNotNull(cvViewDTO);
        assertEquals(cvEntity.getId(), cvViewDTO.getId());
        assertEquals(cvEntity.getName(), cvViewDTO.getName());
        assertEquals(cvEntity.getPathName(), cvViewDTO.getAccessLink());

        // verifica listele extrase de metoda splitString
        List<String> expectedSkills = Arrays.asList("Python", "TensorFlow", "JavaScript", "ReactJS", "AWS SageMaker", "Docker", "SQL", "PostgreSQL", "Figma", "Adobe XD");
        assertEquals(expectedSkills, cvViewDTO.getSkills());

        List<String> expectedLanguages = Arrays.asList("English", "Spanish");
        assertEquals(expectedLanguages, cvViewDTO.getLanguages());

        // Câmpul 'education' NU este mapat/parsat în CvViewDTO în implementarea curentă a CvMapper
    }

    @Test
    void testMapEntityToViewDTO_NullEntity_ThrowsNPE() {
        // varif daca se arunca NullPointerException cand entitatea este null
        assertThrows(NullPointerException.class, () -> {
            CvMapper.mapEntityToViewDTO(null);
        }, "mapEntityToViewDTO ar trebui să arunce NullPointerException pentru input null");
    }

    @Test
    void testMapEntityToDTO_NullEntity_ThrowsNPE() {
        // varif daca se arunca NullPointerException cand entitatea este null
        assertThrows(NullPointerException.class, () -> {
            CvMapper.mapEntityToDTO(null);
        }, "mapEntityToDTO ar trebui să arunce NullPointerException pentru input null");
    }

    @Test
    void testMapEntityToViewDTO_EmptyListStringField() {
        // Test. cazul când inputul pentru splitString este "[]"
        Cv cvEmptyList = new Cv();
        cvEmptyList.setId(2);
        cvEmptyList.setName("Test Empty List");
        cvEmptyList.setPathName("link/emptyList");
        cvEmptyList.setTechnicalSkills("[]"); // String gol specific formatului listei
        cvEmptyList.setForeignLanguages("[]"); // String gol specific formatului listei

        CvViewDTO cvViewDTO = CvMapper.mapEntityToViewDTO(cvEmptyList);

        assertNotNull(cvViewDTO);
        assertEquals(cvEmptyList.getId(), cvViewDTO.getId());
        assertEquals(cvEmptyList.getName(), cvViewDTO.getName());
        assertEquals(cvEmptyList.getPathName(), cvViewDTO.getAccessLink());

        // Metoda splitString ar trebui să returneze liste goale pentru input "[]"
        assertNotNull(cvViewDTO.getSkills());
        assertTrue(cvViewDTO.getSkills().isEmpty(), "Skills list should be empty for '[]' input");
        assertNotNull(cvViewDTO.getLanguages());
        assertTrue(cvViewDTO.getLanguages().isEmpty(), "Languages list should be empty for '[]' input");
    }

    @Test
    void testMapEntityToViewDTO_EmptyStandardStringField_ThrowsNPE() {
        // testtez cazul când inputul pentru splitString este "" (string gol standard)
        Cv cvEmptyString = new Cv();
        cvEmptyString.setId(5);
        cvEmptyString.setName("Test Empty String");
        cvEmptyString.setPathName("link/emptyString");
        cvEmptyString.setTechnicalSkills("[]"); // Un câmp valid
        cvEmptyString.setForeignLanguages("");  // String gol standard care cauzează NPE în Gson

        // Verific că apelul mapEntityToViewDTO arunca NullPointerException
        // deoarece splitString va primi "" pentru foreignLanguages, Gson va returna null,
        // si apoi se va încerca iterarea pe null.
        assertThrows(NullPointerException.class, () -> {
            CvMapper.mapEntityToViewDTO(cvEmptyString);
        }, "mapEntityToViewDTO ar trebui să arunce NullPointerException când splitString primește un string gol standard (\"\")");
    }


    @Test
    void testMapEntityToViewDTO_NullField_ThrowsNPE() {
        Cv cvWithNullField = new Cv();
        cvWithNullField.setId(4);
        cvWithNullField.setName("Test Null Field");
        cvWithNullField.setPathName("link/null");
        cvWithNullField.setTechnicalSkills(null); // setam un câmp relevant la null
        cvWithNullField.setForeignLanguages("[]"); // celalat câmp e valid

        // verif  că apelul mapEntityToViewDTO arunca NullPointerException
        // deoarece splitString va fi apelat cu null pentru technicalSkills
        assertThrows(NullPointerException.class, () -> {
            CvMapper.mapEntityToViewDTO(cvWithNullField);
        }, "mapEntityToViewDTO ar trebui să arunce NullPointerException când splitString primește null");
    }


    @Test
    void testMapEntityToViewDTO_MalformedInputForSplitString_ThrowsException() {
        Cv cvMalformed = new Cv();
        cvMalformed.setId(3);
        cvMalformed.setName("Malformed Test");
        cvMalformed.setPathName("link/malformed");
        // String-uri care probabil vor cauza probleme metodei splitString (fără try-catch)
        // Acest string specific ar putea să nu fie JSON valid după regex și să cauzeze JsonSyntaxException
        cvMalformed.setTechnicalSkills("[{skill=Java, level:4}]"); // ':' în loc de '=' poate cauza probleme
        cvMalformed.setForeignLanguages("{language=English"); // Format invalid

        // Verifică dacă apelul mapEntityToViewDTO aruncă o excepție așteptată
        // (poate fi JsonSyntaxException sau alta, depinde de cum eșuează parsarea)
        // Folosim o clasă de excepție mai generală dacă nu suntem siguri exact ce va arunca
        assertThrows(Exception.class, () -> {
            CvMapper.mapEntityToViewDTO(cvMalformed);
        }, "mapEntityToViewDTO ar trebui să arunce o excepție pentru input malformat în splitString");
    }
}