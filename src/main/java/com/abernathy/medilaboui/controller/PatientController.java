package com.abernathy.medilaboui.controller;


import com.abernathy.medilaboui.model.DiabetesAssessmentResult;
import com.abernathy.medilaboui.model.Patient;
import com.abernathy.medilaboui.model.MedicalHistoryNote;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/patients")
public class PatientController {

    private final RestTemplate restTemplate;
    private final String gatewayBaseUrl;

    public PatientController(RestTemplate restTemplate,
                             @Value("${gateway.base-url}") String gatewayBaseUrl) {
        this.restTemplate = restTemplate;
        this.gatewayBaseUrl = gatewayBaseUrl;
        log.info("UI started with gatewayBaseUrl={}", gatewayBaseUrl);
    }

//    // -----------------------
//    // RestTemplate bean (can be inside controller)
//    // -----------------------
//    @Bean
//    public RestTemplate restTemplate(RestTemplateBuilder builder) {
//        return builder.rootUri(gatewayBaseUrl).build();
//    }

    // -----------------------
    // List all patients
    // -----------------------
    @GetMapping("/all")
    public String listPatients(Model model, HttpServletRequest request) {

        HttpEntity<Void> entity = createEntityWithSession(request);

        ResponseEntity<Patient[]> response = restTemplate.exchange(
                gatewayBaseUrl + "/api/proxy/patients/all",
                HttpMethod.GET,
                entity,
                Patient[].class
        );
        log.info("Using gateway URL: {}", gatewayBaseUrl);

        List<Patient> patients = Arrays.asList(response.getBody());
        model.addAttribute("patients", patients);
        return "list-patients";
    }

    // -----------------------
    // Show add patient form
    // -----------------------
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("patient", new Patient());
        return "add-patient";
    }

    @PostMapping("/add")
    public String addPatient(@ModelAttribute Patient patient, HttpServletRequest request) {

        HttpEntity<Patient> entity = new HttpEntity<>(patient, createJsonHeaders(request));

        restTemplate.postForEntity(
                gatewayBaseUrl + "/api/proxy/patients",
                entity,
                Patient.class
        );
        return "redirect:/patients/all";
    }



    // -----------------------
    // Show edit patient form + notes
    // -----------------------
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,

                               Model model, HttpServletRequest request) {

        HttpEntity<Void> entity = createEntityWithSession(request);

        // Fetch patient
        Patient patient = restTemplate.exchange(
                gatewayBaseUrl + "/api/proxy/patients?id=" + id,
                HttpMethod.GET,
                entity,
                Patient.class
        ).getBody();

        if (patient == null) {
            // create empty object to avoid Thymeleaf crash
            patient = new Patient();
            patient.setPatientId(id); // or .setId(id) if your field is `id`
        }

        model.addAttribute("patient", patient);
        log.info("Patient added to model: {}", patient);


        // Fetch medical notes
        MedicalHistoryNote[] notesArray = restTemplate.exchange(
                gatewayBaseUrl + "/api/proxy/notes/history?patientId=" + id,
                HttpMethod.GET,
                entity,
                MedicalHistoryNote[].class
        ).getBody();

        List<MedicalHistoryNote> notes = notesArray != null ? Arrays.asList(notesArray) : Collections.emptyList();
        model.addAttribute("notes", notes);

        // Prepare empty note object for the form
        model.addAttribute("newNote", new MedicalHistoryNote());
        model.addAttribute("gatewayBaseUrl", gatewayBaseUrl);

        return "edit-patient";
    }
    @GetMapping("/{id}/risk")
    public String assessRisk(@PathVariable Long id, Model model, HttpServletRequest request) {

        HttpEntity<Void> entity = createEntityWithSession(request);

        // Call the risk assessment service
        ResponseEntity<DiabetesAssessmentResult> response = restTemplate.exchange(
                gatewayBaseUrl + "/api/proxy/risk/" + id,
                HttpMethod.GET,
                entity,
                DiabetesAssessmentResult.class
        );

        DiabetesAssessmentResult riskResult = response.getBody();
        model.addAttribute("riskResult", riskResult);

        // Load patient + notes again to display on the same page
        return showEditForm(id, model, request);
    }

    // -----------------------
    // Handle patient update + add note
    // -----------------------
    @PostMapping("/edit/{id}")
    public String updatePatientAndAddNote(
            @PathVariable Long id,
            @ModelAttribute Patient patient,
            @RequestParam(required = false) String physician,
            @RequestParam(required = false) String note,
            HttpServletRequest request) {

        // --- 1️⃣ Prepare headers with session and JSON content type ---
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Propagate session cookie so backend sees authenticated user
        String cookie = request.getHeader("Cookie");
        if (cookie != null) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }

        HttpEntity<Patient> patientEntity = new HttpEntity<>(patient, createJsonHeaders(request));

        // 1️⃣ Update patient info
        restTemplate.exchange(
                gatewayBaseUrl + "/api/proxy/patients?id=" + id,
                HttpMethod.PUT,
                patientEntity,
                Patient.class
        );

        // 2️⃣ Add medical note if provided
        if (physician != null && !physician.isEmpty() && note != null && !note.isEmpty()) {
            MedicalHistoryNote newNote = new MedicalHistoryNote();
            newNote.setPatientId(id);
            newNote.setPhysician(physician);
            newNote.setNote(note);
            newNote.setPatientName(patient.getFirstName());

            HttpEntity<MedicalHistoryNote> noteEntity = new HttpEntity<>(newNote, createJsonHeaders(request));

            restTemplate.postForEntity(
                    gatewayBaseUrl + "/api/proxy/notes/history?patientId=" + id, // query param forwarded
                    noteEntity,
                    MedicalHistoryNote.class
            );
        }

        // Redirect to gateway patient list
        // Use relative path
        return "redirect:/patients/all";
    }

    // -----------------------
    // Helper methods
    // -----------------------
    private HttpEntity<Void> createEntityWithSession(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        String jsessionId = Arrays.stream(request.getCookies() != null ? request.getCookies() : new Cookie[0])
                .filter(c -> "JSESSIONID".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        if (jsessionId != null) {
            headers.add(HttpHeaders.COOKIE, "JSESSIONID=" + jsessionId);
        }
        return new HttpEntity<>(headers);
    }

    private HttpHeaders createJsonHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String jsessionId = Arrays.stream(request.getCookies() != null ? request.getCookies() : new Cookie[0])
                .filter(c -> "JSESSIONID".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        if (jsessionId != null) {
            headers.add(HttpHeaders.COOKIE, "JSESSIONID=" + jsessionId);
        }

        return headers;
    }

}

