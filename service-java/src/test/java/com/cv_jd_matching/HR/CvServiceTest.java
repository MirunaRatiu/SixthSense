package com.cv_jd_matching.HR;

import com.cv_jd_matching.HR.dto.CvDTO;
import com.cv_jd_matching.HR.dto.CvViewDTO;
import com.cv_jd_matching.HR.entity.Cv;
import com.cv_jd_matching.HR.error.InputException;
import com.cv_jd_matching.HR.error.PathException;
import com.cv_jd_matching.HR.repository.ICvRepository;
import com.cv_jd_matching.HR.service.CvServiceImpl;
import com.cv_jd_matching.HR.service.MatchingClient; // Import necesar
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono; // Import necesar pentru Mono

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CvServiceTest {

    @Mock
    private ICvRepository cvRepository;

    @Mock
    private MatchingClient matchingClient; // Mock pentru MatchingClient

    @InjectMocks
    private CvServiceImpl cvService;

    @Captor
    private ArgumentCaptor<List<Cv>> cvListCaptor;

    private Cv cv1;
    private Cv cv2;
    private CvViewDTO cvViewDTO1;
    private CvViewDTO cvViewDTO2;
    private CvDTO cvDTO1;


    @BeforeEach
    void setUp() {
        cv1 = new Cv();
        cv1.setId(1);
        cv1.setName("Test CV 1");
        cv1.setPathName("path/to/cv1.pdf");
        cv1.setTechnicalSkills("[{\"skill\":\"Java\"}]");
        cv1.setForeignLanguages("[{\"language\":\"English\"}]");
        cv1.setEducation("[]");
        // Adaugă și alte câmpuri dacă sunt necesare pentru mapare

        cv2 = new Cv();
        cv2.setId(2);
        cv2.setName("Test CV 2");
        cv2.setPathName("path/to/cv2.docx");
        cv2.setTechnicalSkills("[{\"skill\":\"Python\"}]");
        cv2.setForeignLanguages("[{\"language\":\"French\"}]");
        cv2.setEducation("[]");
        // Adaugă și alte câmpuri dacă sunt necesare pentru mapare

        // Inițializează DTO-urile folosind maparea reală (dacă CvMapper e disponibil și stabil)
        // Sau creează manual DTO-urile așteptate
        cvViewDTO1 = CvViewDTO.builder()
                .id(cv1.getId())
                .name(cv1.getName())
                .accessLink(cv1.getPathName())
                .skills(Arrays.asList("Java")) // Presupunând că mapperul extrage corect
                .languages(Arrays.asList("English")) // Presupunând că mapperul extrage corect
                .build();

        cvViewDTO2 = CvViewDTO.builder()
                .id(cv2.getId())
                .name(cv2.getName())
                .accessLink(cv2.getPathName())
                .skills(Arrays.asList("Python")) // Presupunând că mapperul extrage corect
                .languages(Arrays.asList("French")) // Presupunând că mapperul extrage corect
                .build();

        cvDTO1 = CvDTO.builder()
                .id(cv1.getId())
                .technicalSkills(cv1.getTechnicalSkills())
                .foreignLanguages(cv1.getForeignLanguages())
                .education(cv1.getEducation())
                // Adaugă și alte câmpuri mapate în CvMapper.mapEntityToDTO
                .build();
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
        // Compară DTO-urile rezultate cu cele așteptate
        assertEquals(cvViewDTO1, result.get(0));
        assertEquals(cvViewDTO2, result.get(1));

        verify(cvRepository, times(1)).findAll();
        verifyNoMoreInteractions(cvRepository);
        verifyNoInteractions(matchingClient);
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
        verifyNoInteractions(matchingClient);
    }

    @Test
    void testDeleteFiles_CallsRepositoryAndDeleteClient() {
        // Arrange
        List<Integer> idsToDelete = Arrays.asList(1, 2);
        when(cvRepository.findById(1)).thenReturn(Optional.of(cv1));
        when(cvRepository.findById(2)).thenReturn(Optional.of(cv2));
        // Mock apelul către matchingClient pentru fiecare ID găsit
        when(matchingClient.deleteCv(1)).thenReturn(Mono.empty()); // Sau Mono.just("Success")
        when(matchingClient.deleteCv(2)).thenReturn(Mono.empty());

        // Act
        cvService.deleteFiles(idsToDelete);

        // Assert
        // Verificăm că findById a fost apelat pentru fiecare ID
        verify(cvRepository, times(1)).findById(1);
        verify(cvRepository, times(1)).findById(2);

        // Verificăm că matchingClient.deleteCv a fost apelat pentru fiecare ID
        verify(matchingClient, times(1)).deleteCv(1);
        verify(matchingClient, times(1)).deleteCv(2);

        // Verificăm că metoda deleteAll a fost apelată cu entitățile corecte
        verify(cvRepository, times(1)).deleteAll(cvListCaptor.capture());
        List<Cv> capturedCvList = cvListCaptor.getValue();
        assertNotNull(capturedCvList);
        assertEquals(2, capturedCvList.size());
        assertTrue(capturedCvList.containsAll(Arrays.asList(cv1, cv2)), "Lista pentru deleteAll ar trebui să conțină cv1 și cv2");

        // Verificăm că nu mai sunt alte interacțiuni
        verifyNoMoreInteractions(cvRepository);
        verifyNoMoreInteractions(matchingClient);
    }



    @Test
    void testDeleteFiles_HandlesEmptyList() {
        // Arrange
        List<Integer> idsToDelete = Collections.emptyList();

        // Act
        cvService.deleteFiles(idsToDelete);

        // Assert
        verify(cvRepository, never()).findById(anyInt());
        verify(cvRepository, times(1)).deleteAll(Collections.emptyList());
        verifyNoInteractions(matchingClient);
        verifyNoMoreInteractions(cvRepository);
    }

    @Test
    void testGetCvByPath_ReturnsCvDTOWhenFound() throws PathException {
        // Arrange
        String path = cv1.getPathName();
        when(cvRepository.findCvByPathName(path)).thenReturn(Optional.of(cv1));

        // Act
        CvDTO result = cvService.getCvByPath(path);

        // Assert
        assertNotNull(result);
        // Compară DTO-ul rezultat cu cel așteptat (sau câmpurile relevante)
        assertEquals(cvDTO1.getId(), result.getId());
        assertEquals(cvDTO1.getTechnicalSkills(), result.getTechnicalSkills());
        assertEquals(cvDTO1.getForeignLanguages(), result.getForeignLanguages());
        // Adaugă și alte aserțiuni necesare

        verify(cvRepository, times(1)).findCvByPathName(path);
        verifyNoInteractions(matchingClient);
    }

    @Test
    void testGetCvByPath_ThrowsExceptionWhenNotFound() {
        // Arrange
        String path = "non/existent/path.pdf";
        when(cvRepository.findCvByPathName(path)).thenReturn(Optional.empty());

        // Act & Assert
        PathException exception = assertThrows(PathException.class, () -> {
            cvService.getCvByPath(path);
        }, "Ar trebui să arunce PathException când CV-ul nu este găsit după cale");

        assertEquals("Wrong path", exception.getMessage()); // Verifică mesajul specific

        verify(cvRepository, times(1)).findCvByPathName(path);
        verifyNoInteractions(matchingClient);
    }

    @Test
    void testGetCvById_ReturnsViewDTOWhenFound() throws InputException {
        // Arrange
        Integer id = 1;
        when(cvRepository.findById(id)).thenReturn(Optional.of(cv1));

        // Act
        CvViewDTO result = cvService.getCvById(id);

        // Assert
        assertNotNull(result);
        assertEquals(cvViewDTO1, result); // Compară cu DTO-ul așteptat

        verify(cvRepository, times(1)).findById(id);
        verifyNoInteractions(matchingClient);
    }

    @Test
    void testGetCvById_ThrowsExceptionWhenNotFound() {
        // Arrange
        Integer id = 99;
        when(cvRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        InputException exception = assertThrows(InputException.class, () -> {
            cvService.getCvById(id);
        }, "Ar trebui să arunce InputException când CV-ul nu este găsit după ID");

        assertEquals("The cv with that id is not saved in the database", exception.getMessage()); // Verifică mesajul specific

        verify(cvRepository, times(1)).findById(id);
        verifyNoInteractions(matchingClient);
    }
}