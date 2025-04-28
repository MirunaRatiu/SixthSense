package com.cv_jd_matching.HR;

import com.cv_jd_matching.HR.dto.JobDescriptionDTO;
import com.cv_jd_matching.HR.dto.JobDescriptionViewDTO;
import com.cv_jd_matching.HR.entity.JobDescription;
import com.cv_jd_matching.HR.error.InputException; // Import corect
import com.cv_jd_matching.HR.error.PathException;  // Import corect
import com.cv_jd_matching.HR.mapper.JobDescriptionMapper;
import com.cv_jd_matching.HR.repository.IJobDescriptionRepository;
import com.cv_jd_matching.HR.service.JobDescriptionServiceImpl;
import com.cv_jd_matching.HR.service.MatchingClient; // Import necesar
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient; // Păstrat dacă e folosit altundeva
import reactor.core.publisher.Mono;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList; // Asigură-te că e importat
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobDescriptionServiceTest {

    @Mock
    private IJobDescriptionRepository jobDescriptionRepository;

    @Mock
    private MatchingClient matchingClient; // Mock adăugat

    // Mock-urile pentru WebClient sunt păstrate în caz că sunt necesare pentru alte teste
    // Dacă nu sunt folosite deloc în această clasă, pot fi eliminate.
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpecMock;
    @Mock
    private WebClient.RequestBodySpec requestBodySpecMock;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpecMock;
    @Mock
    private WebClient.ResponseSpec responseSpecMock;

    @InjectMocks
    private JobDescriptionServiceImpl jobDescriptionService;

    @Captor
    private ArgumentCaptor<List<JobDescription>> jdListCaptor;
    // Captor pentru WebClient ID nu mai este necesar dacă nu testăm direct interacțiunea WebClient aici
    // @Captor
    // private ArgumentCaptor<Integer> webClientIdCaptor;

    private JobDescription jd1;
    private JobDescription jd2;
    private JobDescriptionViewDTO jdViewDTO1;
    private JobDescriptionViewDTO jdViewDTO2;
    private JobDescriptionDTO jdDTO1;


    @BeforeEach
    void setUp() {
        jd1 = new JobDescription();
        jd1.setId(1);
        jd1.setJobTitle("Java Developer");
        jd1.setPathName("path/to/jd1.pdf");
        // Simulăm date JSON valide ca string-uri pentru teste
        jd1.setRequiredQualifications("[{\"original_statement\":\"Experienta cu Java.\"}, {\"original_statement\":\"Cunostinte Spring Boot.\"}]");
        jd1.setKeyResponsibilities("Develop applications");
        jd1.setCompanyOverview("A great company");
        jd1.setPreferredSkills("[\"SQL\"]");
        jd1.setMessage("Some message");
        // Setează și data, dacă e relevantă pentru teste
        // jd1.setCreatedAt(new java.sql.Date(System.currentTimeMillis()));

        jd2 = new JobDescription();
        jd2.setId(2);
        jd2.setJobTitle("Python Developer");
        jd2.setPathName("path/to/jd2.docx");
        jd2.setRequiredQualifications("[{\"original_statement\":\"Experienta cu Python.\"}]");
        jd2.setKeyResponsibilities("Automate tasks");
        jd2.setCompanyOverview("Another great company");
        jd2.setPreferredSkills("[\"AWS\"]");
        jd2.setMessage("Another message");
        // jd2.setCreatedAt(new java.sql.Date(System.currentTimeMillis()));


        // Cream ViewDTO-urile de test folosind maparea reală pentru consistență
        jdViewDTO1 = JobDescriptionMapper.mapEntityToViewDTO(jd1);
        jdViewDTO2 = JobDescriptionMapper.mapEntityToViewDTO(jd2);

        // Cream DTO-ul de test folosind maparea reală
        jdDTO1 = JobDescriptionMapper.mapEntityToDTO(jd1);

    }

    @Test
    void testGetJobDescriptions_ReturnsListOfViewDTO() {
        // Arrange
        List<JobDescription> jdList = Arrays.asList(jd1, jd2);
        when(jobDescriptionRepository.findAll()).thenReturn(jdList);

        // Act
        List<JobDescriptionViewDTO> result = jobDescriptionService.getJobDescriptions();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        // Comparăm DTO-urile generate în test cu cele așteptate (generate în setUp)
        // Asigură-te că equals este implementat corect în DTO sau compară câmp cu câmp
        assertEquals(jdViewDTO1.getId(), result.get(0).getId());
        assertEquals(jdViewDTO1.getJobTitle(), result.get(0).getJobTitle());
        assertEquals(jdViewDTO1.getAccessLink(), result.get(0).getAccessLink());
        assertEquals(jdViewDTO1.getRequiredQualifications(), result.get(0).getRequiredQualifications()); // Comparăm și calificările parsate

        assertEquals(jdViewDTO2.getId(), result.get(1).getId());
        assertEquals(jdViewDTO2.getJobTitle(), result.get(1).getJobTitle());
        assertEquals(jdViewDTO2.getAccessLink(), result.get(1).getAccessLink());
        assertEquals(jdViewDTO2.getRequiredQualifications(), result.get(1).getRequiredQualifications()); // Comparăm și calificările parsate


        verify(jobDescriptionRepository, times(1)).findAll();
        verifyNoMoreInteractions(jobDescriptionRepository);
        verifyNoInteractions(matchingClient); // Verificăm că matchingClient nu e folosit
        // Verificam că WebClient NU a fost folosit (dacă e cazul)
        // verifyNoInteractions(webClient, requestBodyUriSpecMock, requestBodySpecMock, requestHeadersSpecMock, responseSpecMock);
    }

    @Test
    void testGetJobDescriptions_ReturnsEmptyListWhenNoJds() {
        // Arrange
        when(jobDescriptionRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<JobDescriptionViewDTO> result = jobDescriptionService.getJobDescriptions();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(jobDescriptionRepository, times(1)).findAll();
        verifyNoInteractions(matchingClient); // Verificăm că matchingClient nu e folosit
        // Verificam ca WebClient NU a fost folosit (dacă e cazul)
        // verifyNoInteractions(webClient, requestBodyUriSpecMock, requestBodySpecMock, requestHeadersSpecMock, responseSpecMock);
    }


    @Test
    void testGetJobDescriptionById_ReturnsViewDTOWhenFound() throws InputException { // Adaugă throws InputException
        // Arrange
        Integer id = 1;
        when(jobDescriptionRepository.findById(id)).thenReturn(Optional.of(jd1));

        // Act
        JobDescriptionViewDTO result = jobDescriptionService.getJobDescriptionById(id); // Apelul poate arunca InputException

        // Assert
        assertNotNull(result);
        // Comparăm cu DTO-ul așteptat generat în setUp
        assertEquals(jdViewDTO1.getId(), result.getId());
        assertEquals(jdViewDTO1.getJobTitle(), result.getJobTitle());
        assertEquals(jdViewDTO1.getAccessLink(), result.getAccessLink());
        assertEquals(jdViewDTO1.getRequiredQualifications(), result.getRequiredQualifications());


        verify(jobDescriptionRepository, times(1)).findById(id);
        verifyNoInteractions(matchingClient);
        // verifyNoInteractions(webClient);
    }

    @Test
    void testGetJobDescriptionById_ThrowsExceptionWhenNotFound() {
        // Arrange
        Integer id = 99;
        when(jobDescriptionRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        // Așteaptă InputException
        InputException exception = assertThrows(InputException.class, () -> {
            jobDescriptionService.getJobDescriptionById(id);
        }, "Ar trebui să arunce InputException când ID-ul nu este găsit");

        // Opțional: Verifică mesajul excepției dacă e necesar
        assertEquals("Wrong id", exception.getMessage()); // Sau mesajul specific din InputException

        verify(jobDescriptionRepository, times(1)).findById(id);
        verifyNoInteractions(matchingClient);
        // verifyNoInteractions(webClient);
    }

    @Test
    void testGetJobDescriptionByPath_ReturnsDTOWhenFound() throws PathException { // Adaugă throws PathException
        // Arrange
        String path = jd1.getPathName(); // Folosește calea reală din setup
        when(jobDescriptionRepository.findJobDescriptionByPathName(path)).thenReturn(Optional.of(jd1));

        // Act
        JobDescriptionDTO result = jobDescriptionService.getJobDescriptionByPath(path); // Apelul poate arunca PathException

        // Assert
        assertNotNull(result);
        // Comparăm cu DTO-ul așteptat generat în setUp
        assertEquals(jdDTO1.getId(), result.getId());
        assertEquals(jdDTO1.getJobTitle(), result.getJobTitle());
        assertEquals(jdDTO1.getRequiredQualifications(), result.getRequiredQualifications()); // Comparăm string-ul JSON original
        assertEquals(jdDTO1.getKeyResponsibilities(), result.getKeyResponsibilities());
        assertEquals(jdDTO1.getCompanyOverview(), result.getCompanyOverview());
        assertEquals(jdDTO1.getPreferredSkills(), result.getPreferredSkills());
        assertEquals(jdDTO1.getMessage(), result.getMessage());


        verify(jobDescriptionRepository, times(1)).findJobDescriptionByPathName(path);
        verifyNoInteractions(matchingClient);
        // verifyNoInteractions(webClient);
    }

    @Test
    void testGetJobDescriptionByPath_ThrowsExceptionWhenNotFound() {
        // Arrange
        String path = "non/existent/path.pdf";
        when(jobDescriptionRepository.findJobDescriptionByPathName(path)).thenReturn(Optional.empty());

        // Act & Assert
        // Așteaptă PathException
        PathException exception = assertThrows(PathException.class, () -> {
            jobDescriptionService.getJobDescriptionByPath(path);
        }, "Ar trebui să arunce PathException când calea nu este găsită");

        // Opțional: Verifică mesajul excepției dacă e necesar
        assertEquals("Wrong url", exception.getMessage()); // Sau mesajul specific din PathException

        verify(jobDescriptionRepository, times(1)).findJobDescriptionByPathName(path);
        verifyNoInteractions(matchingClient);
        // verifyNoInteractions(webClient);
    }

    @Test
    void testDeleteFiles_SuccessfulDeletion() {
        // Arrange
        List<Integer> idsToDelete = Arrays.asList(1, 2);
        when(jobDescriptionRepository.findById(1)).thenReturn(Optional.of(jd1));
        when(jobDescriptionRepository.findById(2)).thenReturn(Optional.of(jd2));
        // Mock apelul către matchingClient pentru fiecare ID găsit
        // Presupunem că deleteJobDescription returnează Mono<Void> sau Mono<String>
        when(matchingClient.deleteJobDescription(1)).thenReturn(Mono.empty()); // Sau Mono.just("Success") etc.
        when(matchingClient.deleteJobDescription(2)).thenReturn(Mono.empty());

        // Act
        jobDescriptionService.deleteFiles(idsToDelete);

        // Assert
        // Verifică apelurile findById
        verify(jobDescriptionRepository, times(1)).findById(1);
        verify(jobDescriptionRepository, times(1)).findById(2);
        // Verifică apelurile către matchingClient
        verify(matchingClient, times(1)).deleteJobDescription(1);
        verify(matchingClient, times(1)).deleteJobDescription(2);
        // Verifică apelul deleteAll cu entitățile corecte
        verify(jobDescriptionRepository, times(1)).deleteAll(jdListCaptor.capture());
        List<JobDescription> capturedJdList = jdListCaptor.getValue();
        assertNotNull(capturedJdList);
        assertEquals(2, capturedJdList.size());
        assertTrue(capturedJdList.containsAll(Arrays.asList(jd1, jd2)), "Lista pentru deleteAll ar trebui să conțină jd1 și jd2");

        // Verifică că nu mai sunt alte interacțiuni
        verifyNoMoreInteractions(jobDescriptionRepository);
        verifyNoMoreInteractions(matchingClient);
        // verifyNoInteractions(webClient);
    }



    @Test
    void testDeleteFiles_HandlesEmptyList() {
        // Arrange
        List<Integer> idsToDelete = Collections.emptyList();

        // Act
        jobDescriptionService.deleteFiles(idsToDelete);

        // Assert
        // Verificăm că findById nu este apelat niciodată
        verify(jobDescriptionRepository, never()).findById(anyInt());
        // Verificăm că deleteAll este apelat cu o listă goală (comportament așteptat dacă lista de ID-uri e goală)
        verify(jobDescriptionRepository, times(1)).deleteAll(Collections.emptyList());
        // Verificăm că matchingClient nu este apelat
        verifyNoInteractions(matchingClient);
        // Asigură-te că nu mai sunt alte interacțiuni neașteptate
        verifyNoMoreInteractions(jobDescriptionRepository);
        // verifyNoInteractions(webClient);
    }
}
