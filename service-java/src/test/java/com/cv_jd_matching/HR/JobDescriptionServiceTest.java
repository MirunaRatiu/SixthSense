package com.cv_jd_matching.HR;

import com.cv_jd_matching.HR.dto.JobDescriptionDTO;
import com.cv_jd_matching.HR.dto.JobDescriptionViewDTO;
import com.cv_jd_matching.HR.entity.JobDescription;
import com.cv_jd_matching.HR.mapper.JobDescriptionMapper;
import com.cv_jd_matching.HR.repository.IJobDescriptionRepository;
import com.cv_jd_matching.HR.service.JobDescriptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobDescriptionServiceTest {

    @Mock
    private IJobDescriptionRepository jobDescriptionRepository;

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
    @Captor
    private ArgumentCaptor<Integer> webClientIdCaptor;

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
        jd1.setRequiredQualifications("[\"Java\", \"Spring\"]");
        jd1.setKeyResponsibilities("Develop applications");
        jd1.setCompanyOverview("A great company");
        jd1.setPreferredSkills("[\"SQL\"]");
        jd1.setMessage("Some message");

        jd2 = new JobDescription();
        jd2.setId(2);
        jd2.setJobTitle("Python Developer");
        jd2.setPathName("path/to/jd2.docx");
        jd2.setRequiredQualifications("[\"Python\", \"Django\"]");
        jd2.setKeyResponsibilities("Automate tasks");
        jd2.setCompanyOverview("Another great company");
        jd2.setPreferredSkills("[\"AWS\"]");
        jd2.setMessage("Another message");

        // Cream ViewDTO-urile de test DOAR cu câmpurile mapate
        jdViewDTO1 = JobDescriptionViewDTO.builder()
                .id(jd1.getId())
                .jobTitle(jd1.getJobTitle())
                .accessLink(jd1.getPathName())
                .build();
        jdViewDTO2 = JobDescriptionViewDTO.builder()
                .id(jd2.getId())
                .jobTitle(jd2.getJobTitle())
                .accessLink(jd2.getPathName())
                .build();

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
        assertEquals(jdViewDTO1.getJobTitle(), result.get(0).getJobTitle());
        assertEquals(jdViewDTO1.getId(), result.get(0).getId());
        assertEquals(jdViewDTO1.getAccessLink(), result.get(0).getAccessLink());

        assertEquals(jdViewDTO2.getJobTitle(), result.get(1).getJobTitle());
        assertEquals(jdViewDTO2.getId(), result.get(1).getId());
        assertEquals(jdViewDTO2.getAccessLink(), result.get(1).getAccessLink());

        verify(jobDescriptionRepository, times(1)).findAll();
        verifyNoMoreInteractions(jobDescriptionRepository);
        // Verificam că WebClient NU a fost folosit
        verifyNoInteractions(webClient, requestBodyUriSpecMock, requestBodySpecMock, requestHeadersSpecMock, responseSpecMock);
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
        // Verificam ca WebClient NU a fost folosit
        verifyNoInteractions(webClient, requestBodyUriSpecMock, requestBodySpecMock, requestHeadersSpecMock, responseSpecMock);
    }


    @Test
    void testGetJobDescriptionById_ReturnsViewDTOWhenFound() {
        // Arrange
        Integer id = 1;
        when(jobDescriptionRepository.findById(id)).thenReturn(Optional.of(jd1));

        // Act
        JobDescriptionViewDTO result = jobDescriptionService.getJobDescriptionById(id);

        // Assert
        assertNotNull(result);
        assertEquals(jdViewDTO1.getId(), result.getId());
        assertEquals(jdViewDTO1.getJobTitle(), result.getJobTitle());
        assertEquals(jdViewDTO1.getAccessLink(), result.getAccessLink());

        verify(jobDescriptionRepository, times(1)).findById(id);
        // Verificam ca WebClient NU a fost folosit
        verifyNoInteractions(webClient, requestBodyUriSpecMock, requestBodySpecMock, requestHeadersSpecMock, responseSpecMock);
    }


    @Test
    void testGetJobDescriptionById_ThrowsExceptionWhenNotFound() {
        // Arrange
        Integer id = 99;
        when(jobDescriptionRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            jobDescriptionService.getJobDescriptionById(id);
        });

        assertEquals("Wrong id", exception.getMessage());
        verify(jobDescriptionRepository, times(1)).findById(id);
        // Verificam că WebClient NU a fost folosit
        verifyNoInteractions(webClient, requestBodyUriSpecMock, requestBodySpecMock, requestHeadersSpecMock, responseSpecMock);
    }


    @Test
    void testGetJobDescriptionByPath_ReturnsDTOWhenFound() {
        // Arrange
        String path = "path/to/jd1.pdf";
        when(jobDescriptionRepository.findJobDescriptionByPathName(path)).thenReturn(Optional.of(jd1));

        // Act
        JobDescriptionDTO result = jobDescriptionService.getJobDescriptionByPath(path);

        // Assert
        assertNotNull(result);
        assertEquals(jdDTO1.getId(), result.getId());
        assertEquals(jd1.getJobTitle(), result.getJobTitle());
        assertEquals(jd1.getCompanyOverview(), result.getCompanyOverview());
        assertEquals(jd1.getKeyResponsibilities(), result.getKeyResponsibilities());
        assertEquals(jd1.getPreferredSkills(), result.getPreferredSkills());
        assertEquals(jd1.getRequiredQualifications(), result.getRequiredQualifications());
        assertEquals(jd1.getMessage(), result.getMessage());

        verify(jobDescriptionRepository, times(1)).findJobDescriptionByPathName(path);
        // Verificăm că WebClient NU a fost folosit
        verifyNoInteractions(webClient, requestBodyUriSpecMock, requestBodySpecMock, requestHeadersSpecMock, responseSpecMock);
    }


    @Test
    void testGetJobDescriptionByPath_ThrowsExceptionWhenNotFound() {
        // Arrange
        String path = "non/existent/path.pdf";
        when(jobDescriptionRepository.findJobDescriptionByPathName(path)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            jobDescriptionService.getJobDescriptionByPath(path);
        });

        assertEquals("Wrong url", exception.getMessage());
        verify(jobDescriptionRepository, times(1)).findJobDescriptionByPathName(path);
        // Verificam că WebClient NU a fost folosit
        verifyNoInteractions(webClient, requestBodyUriSpecMock, requestBodySpecMock, requestHeadersSpecMock, responseSpecMock);
    }


    @Test
    void testDeleteFiles_CallsRepositoryDeleteAllAndWebClientForEachId() {
        // Arrange
        List<Integer> idsToDelete = Arrays.asList(1, 2);
        when(jobDescriptionRepository.findById(1)).thenReturn(Optional.of(jd1));
        when(jobDescriptionRepository.findById(2)).thenReturn(Optional.of(jd2));

        // Configurare mock WebClient DOAR pentru acest test
        when(webClient.method(HttpMethod.DELETE)).thenReturn(requestBodyUriSpecMock);
        when(requestBodyUriSpecMock.uri(anyString())).thenReturn(requestBodySpecMock);
        when(requestBodySpecMock.bodyValue(any())).thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);
        when(responseSpecMock.bodyToMono(String.class)).thenReturn(Mono.just("Deleted"));


        // Act
        jobDescriptionService.deleteFiles(idsToDelete);

        // Assert
        verify(jobDescriptionRepository, times(1)).findById(1);
        verify(jobDescriptionRepository, times(1)).findById(2);

        verify(jobDescriptionRepository, times(1)).deleteAll(jdListCaptor.capture());
        List<JobDescription> capturedJdList = jdListCaptor.getValue();
        assertNotNull(capturedJdList);
        assertEquals(2, capturedJdList.size());
        assertTrue(capturedJdList.containsAll(Arrays.asList(jd1, jd2)), "Lista pentru deleteAll ar trebui să conțină jd1 și jd2");

        // Verificam apelurile WebClient așa cum erau inainte
        verify(webClient, times(idsToDelete.size())).method(HttpMethod.DELETE);
        verify(requestBodyUriSpecMock, times(idsToDelete.size())).uri("/delete/jd");
        verify(requestBodySpecMock, times(idsToDelete.size())).bodyValue(webClientIdCaptor.capture());
        List<Integer> capturedWebClientIds = webClientIdCaptor.getAllValues();
        assertEquals(idsToDelete, capturedWebClientIds, "WebClient ar trebui apelat cu ID-urile 1 și 2");

        verify(requestHeadersSpecMock, times(idsToDelete.size())).retrieve();
        verify(responseSpecMock, times(idsToDelete.size())).bodyToMono(String.class);

        verifyNoMoreInteractions(jobDescriptionRepository);
        // Verificam că nu mai sunt alte interacțiuni cu WebClient după cele așteptate
        verifyNoMoreInteractions(webClient, requestBodyUriSpecMock, requestBodySpecMock, requestHeadersSpecMock, responseSpecMock);
    }

    @Test
    void testDeleteFiles_HandlesEmptyList() {
        // Arrange
        List<Integer> idsToDelete = Collections.emptyList();

        // Act
        jobDescriptionService.deleteFiles(idsToDelete);

        // Assert
        verify(jobDescriptionRepository, never()).findById(anyInt());
        verify(jobDescriptionRepository, times(1)).deleteAll(Collections.emptyList());
        // Verificam că WebClient NU a fost folosit
        verifyNoInteractions(webClient, requestBodyUriSpecMock, requestBodySpecMock, requestHeadersSpecMock, responseSpecMock);

        verifyNoMoreInteractions(jobDescriptionRepository);
    }


    @Test
    void testDeleteFiles_ThrowsExceptionWhenIdNotFound() {
        // Arrange
        List<Integer> idsToDelete = Arrays.asList(1, 99); // ID 99 nu exista
        when(jobDescriptionRepository.findById(1)).thenReturn(Optional.of(jd1));
        when(jobDescriptionRepository.findById(99)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> {
            jobDescriptionService.deleteFiles(idsToDelete);
        }, "Ar trebui să arunce NoSuchElementException când un ID nu este găsit");

        verify(jobDescriptionRepository, times(1)).findById(1);
        verify(jobDescriptionRepository, times(1)).findById(99);
        verify(jobDescriptionRepository, never()).deleteAll(anyList());
        // Verificam că WebClient NU a fost folosit, deoarece excepția apare înainte
        verifyNoInteractions(webClient, requestBodyUriSpecMock, requestBodySpecMock, requestHeadersSpecMock, responseSpecMock);
    }
}