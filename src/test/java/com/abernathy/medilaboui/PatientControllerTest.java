package com.abernathy.medilaboui;



import com.abernathy.medilaboui.controller.PatientController;
import com.abernathy.medilaboui.model.DiabetesAssessmentResult;
import com.abernathy.medilaboui.model.MedicalHistoryNote;
import com.abernathy.medilaboui.model.Patient;
import com.abernathy.medilaboui.model.RiskAssessmentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PatientControllerTest {

    @InjectMocks
    private PatientController patientController;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Model model;

    private MockMvc mockMvc;

    private final String gatewayBaseUrl = "http://localhost:8081";
    private final String gatewayExternalURL = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        patientController = new PatientController(restTemplate, gatewayBaseUrl);
        // inject gatewayExternalURL manually
        patientController.gatewayExternalURL = gatewayExternalURL;

        mockMvc = MockMvcBuilders.standaloneSetup(patientController).build();
    }

    @Test
    void testListPatients() throws Exception {
        Patient patient = new Patient();
        patient.setPatientId(1L);
        Patient[] patientsArray = {patient};

        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("JSESSIONID", "123")});
        when(restTemplate.exchange(
                eq(gatewayBaseUrl + "/api/proxy/patients/all"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Patient[].class)
        )).thenReturn(new ResponseEntity<>(patientsArray, HttpStatus.OK));

        String view = patientController.listPatients(model, request);
        assertEquals("list-patients", view);
        verify(model).addAttribute(eq("patients"), any());
    }

    @Test
    void testShowAddForm() {
        String view = patientController.showAddForm(model);
        assertEquals("add-patient", view);
        verify(model).addAttribute(eq("patient"), any(Patient.class));
    }

    @Test
    void testAddPatient() {
        Patient patient = new Patient();
        patient.setFirstName("John");

        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("JSESSIONID", "123")});
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Patient.class)))
                .thenReturn(new ResponseEntity<>(patient, HttpStatus.CREATED));

        String redirect = patientController.addPatient(patient, request);
        assertEquals("redirect:"+gatewayExternalURL+"/patients/all", redirect);
    }

    @Test
    void testShowEditForm() {
        Long patientId = 1L;
        Patient patient = new Patient();
        patient.setPatientId(patientId);

        MedicalHistoryNote note = new MedicalHistoryNote();
        note.setNote("History note");

        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("JSESSIONID", "123")});

        when(restTemplate.exchange(
                eq(gatewayBaseUrl + "/api/proxy/patients?id=" + patientId),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Patient.class)
        )).thenReturn(new ResponseEntity<>(patient, HttpStatus.OK));

        when(restTemplate.exchange(
                eq(gatewayBaseUrl + "/api/proxy/notes/history?patientId=" + patientId),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(MedicalHistoryNote[].class)
        )).thenReturn(new ResponseEntity<>(new MedicalHistoryNote[]{note}, HttpStatus.OK));

        String view = patientController.showEditForm(patientId, model, request);
        assertEquals("edit-patient", view);
        verify(model).addAttribute(eq("patient"), eq(patient));
        verify(model).addAttribute(eq("notes"), any());
    }

    @Test
    void testUpdatePatientAndAddNote() {
        Long patientId = 1L;
        Patient patient = new Patient();
        patient.setFirstName("John");

        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("JSESSIONID", "123")});

        String redirect = patientController.updatePatientAndAddNote(patientId, patient, "Dr. Smith", "Note content", request);
        assertEquals("redirect:"+gatewayExternalURL+"/patients/all", redirect);

        verify(restTemplate, times(1))
                .exchange(eq(gatewayBaseUrl + "/api/proxy/patients?id=" + patientId), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Patient.class));

        verify(restTemplate, times(1))
                .postForEntity(eq(gatewayBaseUrl + "/api/proxy/notes/history?patientId=" + patientId), any(HttpEntity.class), eq(MedicalHistoryNote.class));
    }
}
