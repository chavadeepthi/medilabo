package com.abernathy.medilaboui.controller;


import com.abernathy.medilaboui.model.DiabetesAssessmentResult;
import com.abernathy.medilaboui.model.Patient;
import com.abernathy.medilaboui.model.MedicalHistoryNote;
import com.abernathy.medilaboui.model.RiskAssessmentRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

        RiskAssessmentRequest riskRequest = new RiskAssessmentRequest();
        riskRequest.setPatient(patient);
        riskRequest.setNotes(notes.stream()
                .map(MedicalHistoryNote::getNote) // assuming each note has getContent()
                .collect(Collectors.toList())
        );

        // --- Fetch risk assessment ---
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // preserve session/cookies if needed
            HttpSession session = request.getSession(false);
            if (session != null) {
                Object jwt = session.getAttribute("JWT");
                if (jwt instanceof String) {
                    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
                }
            }

            HttpEntity<RiskAssessmentRequest> requestEntity = new HttpEntity<>(riskRequest, headers);

            ResponseEntity<DiabetesAssessmentResult> riskResponse = restTemplate.exchange(
                    gatewayBaseUrl + "/api/proxy/risk/assess",
                    HttpMethod.POST,
                    requestEntity,
                    DiabetesAssessmentResult.class
            );

            model.addAttribute("riskResult", riskResponse.getBody());
        } catch (HttpClientErrorException ex) {
            log.error("Risk assessment failed: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            model.addAttribute("riskError", "Risk assessment failed: " + ex.getStatusCode());
        }

        model.addAttribute("gatewayBaseUrl", gatewayBaseUrl);

        return "edit-patient";
    }
//    @GetMapping("/risk/{id}")
//    public String assessRisk(@PathVariable Long id, Model model, HttpServletRequest request) {
//        HttpHeaders headers = new HttpHeaders();
//        if (request.getCookies() != null) {
//            Arrays.stream(request.getCookies())
//                    .filter(c -> "JSESSIONID".equals(c.getName()))
//                    .findFirst()
//                    .ifPresent(cookie -> headers.add(HttpHeaders.COOKIE, "JSESSIONID=" + cookie.getValue()));
//        }
//
//        String url = gatewayBaseUrl + "/api/proxy/risk/assessment?patientId=" + id;
//
//        log.info("UI: Calling risk endpoint URL {} with cookies {}", url,
//                Arrays.toString(request.getCookies()));
//
////        ResponseEntity<DiabetesAssessmentResult> response = restTemplate.exchange(
////                url,
////                HttpMethod.GET,
////                createEntityWithSession(request),
////                DiabetesAssessmentResult.class
////        );
//
//        try {
//            ResponseEntity<DiabetesAssessmentResult> response = restTemplate.exchange(
//                    url,
//                    HttpMethod.GET,
//                    createEntityWithSession(request),
//                    DiabetesAssessmentResult.class);
//            model.addAttribute("riskResult", response.getBody());
//        } catch(HttpClientErrorException ex) {
//            log.error("UI: Risk endpoint returned error status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
//            model.addAttribute("error", "Risk assessment failed: " + ex.getStatusCode());
//        }
//
//        // Load patient + notes for display
//        return showEditForm(id, model, request);
//    }


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
    private HttpHeaders createHeadersWithSession(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        if (request.getCookies() != null) {
            Arrays.stream(request.getCookies())
                    .filter(c -> "JSESSIONID".equals(c.getName()))
                    .findFirst()
                    .ifPresent(cookie -> headers.add(HttpHeaders.COOKIE, "JSESSIONID=" + cookie.getValue()));
        }
        return headers;
    }

    private HttpEntity<Void> createEntityWithSession(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        if (request.getCookies() != null) {
            Arrays.stream(request.getCookies())
                    .filter(c -> "JSESSIONID".equals(c.getName()))
                    .findFirst()
                    .ifPresent(cookie -> headers.add(HttpHeaders.COOKIE, "JSESSIONID=" + cookie.getValue()));
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

