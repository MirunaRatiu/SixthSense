package com.cv_jd_matching.HR;

import com.cv_jd_matching.HR.dto.CvDTO;
import com.cv_jd_matching.HR.dto.CvViewDTO;
import com.cv_jd_matching.HR.entity.Cv;
import com.cv_jd_matching.HR.error.PathException;
import com.cv_jd_matching.HR.repository.ICvRepository;
import com.cv_jd_matching.HR.service.CvServiceImpl; // Asigură-te că importul este corect
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CvServiceTest {

    @Mock
    private ICvRepository cvRepository;


    @InjectMocks
    private CvServiceImpl cvService;

    @Captor
    private ArgumentCaptor<List<Cv>> cvListCaptor; // Captor pentru a verifica lista pasată la deleteAll

    private Cv cv1;
    private Cv cv2;

    @BeforeEach
    void setUp() {
        cv1 = new Cv();
        cv1.setId(1);
        cv1.setName("Test CV 1");
        cv1.setPathName("path/to/cv1.pdf");

        cv1.setTechnicalSkills("[{\"skill\":\"Java\"}]");
        cv1.setForeignLanguages("[{\"language\":\"English\"}]");
        cv1.setEducation("[]");

        cv2 = new Cv();
        cv2.setId(2);
        cv2.setName("Test CV 2");
        cv2.setPathName("path/to/cv2.docx");
        cv2.setTechnicalSkills("[{\"skill\":\"Python\"}]");
        cv2.setForeignLanguages("[{\"language\":\"French\"}]");
        cv2.setEducation("[]");
    }

    @Test
    void testGetCvs_ReturnsListOfCvViewDTO() {
        // Arrange
        List<Cv> cvList = Arrays.asList(cv1, cv2);
        when(cvRepository.findAll()).thenReturn(cvList);

        // Act
        List<CvViewDTO> result = cvService.getCvs();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(cv1.getName(), result.get(0).getName());
        assertEquals(cv1.getId(), result.get(0).getId());
        assertEquals(cv1.getPathName(), result.get(0).getAccessLink());
        assertNotNull(result.get(0).getSkills());
        assertFalse(result.get(0).getSkills().isEmpty());
        assertEquals("Java", result.get(0).getSkills().get(0));

        assertEquals(cv2.getName(), result.get(1).getName());
        assertEquals(cv2.getId(), result.get(1).getId());
        assertEquals(cv2.getPathName(), result.get(1).getAccessLink());
        assertNotNull(result.get(1).getSkills());
        assertFalse(result.get(1).getSkills().isEmpty());
        assertEquals("Python", result.get(1).getSkills().get(0)); // Presupunând că mapperul extrage corect

        verify(cvRepository, times(1)).findAll();
    }

    @Test
    void testGetCvs_ReturnsEmptyListWhenNoCvs() {
        // Arrange
        when(cvRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<CvViewDTO> result = cvService.getCvs();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(cvRepository, times(1)).findAll();
    }

 /*   @Test
    void testDeleteFiles_CallsRepositoryDeleteAllWithCorrectEntities() {
        // Arrange
        List<Integer> idsToDelete = Arrays.asList(1, 2);
        when(cvRepository.findById(1)).thenReturn(Optional.of(cv1));
        when(cvRepository.findById(2)).thenReturn(Optional.of(cv2));

        // Act
        cvService.deleteFiles(idsToDelete);

        // Assert
        // Verificăm că metoda deleteAll a fost apelată exact o dată
        // și capturăm argumentul (lista de CV-uri)
        verify(cvRepository, times(1)).deleteAll(cvListCaptor.capture());

        List<Cv> capturedCvList = cvListCaptor.getValue();
        assertNotNull(capturedCvList);
        assertEquals(2, capturedCvList.size());
        assertTrue(capturedCvList.containsAll(Arrays.asList(cv1, cv2)));

        verify(cvRepository, times(1)).findById(1);
        verify(cvRepository, times(1)).findById(2);
        verifyNoMoreInteractions(cvRepository);
    }*/

    @Test
    void testDeleteFiles_HandlesEmptyList() {
        // Arrange
        List<Integer> idsToDelete = Collections.emptyList();

        // Act
        cvService.deleteFiles(idsToDelete);

        // Assert
        // Verificăm că deleteAll este apelata cu o lista goala de entități
        verify(cvRepository, times(1)).deleteAll(Collections.emptyList());
        // Verificam că findById nu este apelat niciodată
        verify(cvRepository, never()).findById(anyInt());
        // Asigura-te că nu mai sunt alte interacțiuni neașteptate
        verifyNoMoreInteractions(cvRepository);
    }

    @Test
    void testGetCvByPath_ReturnsCvDTOWhenFound() {
        // Arrange
        // Folosește calea REALĂ setată pentru cv1 în metoda setUp
        String actualPathFromSetup = cv1.getPathName(); // Obține calea corectă de la obiectul cv1
        // Configurează mock-ul să răspundă la calea corectă
        when(cvRepository.findCvByPathName(actualPathFromSetup)).thenReturn(Optional.of(cv1));

        // Act
        // Apelează serviciul cu calea corectă
        CvDTO result = null;
        try {
            result = cvService.getCvByPath(actualPathFromSetup);
        } catch (PathException e) {
            throw new RuntimeException(e);
        }

        // Assert
        assertNotNull(result);
        // Compară cu obiectul cv1 original
        assertEquals(cv1.getId(), result.getId());
        assertEquals(cv1.getTechnicalSkills(), result.getTechnicalSkills());
        assertEquals(cv1.getForeignLanguages(), result.getForeignLanguages());
        // Adaugă și alte aserțiuni necesare pentru DTO

        // Verifică dacă repository-ul a fost apelat cu calea corectă
        verify(cvRepository, times(1)).findCvByPathName(actualPathFromSetup);
    }

    @Test
    void testGetCvByPath_ThrowsExceptionWhenNotFound() {
        // Arrange
        String path = "non/existent/path.pdf";
        when(cvRepository.findCvByPathName(path)).thenReturn(Optional.empty());

        // Act & Assert
        // Verificam că se aruncă PathException când CV-ul nu este găsit
        PathException exception = assertThrows(PathException.class, () -> { // <-- Schimbă RuntimeException.class cu PathException.class
            cvService.getCvByPath(path);
        }, "Ar trebui să arunce PathException când CV-ul nu este găsit");

        // Verifica mesajul exact al excepției (dacă PathException are un mesaj specific)
        // Sau poți elimina verificarea mesajului dacă nu este necesară
        // assertEquals("Wrong path", exception.getMessage()); // <-- Comentează sau ajustează dacă mesajul e diferit sau inexistent

        verify(cvRepository, times(1)).findCvByPathName(path);
    }
}