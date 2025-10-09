package com.abernathy.medilaboui.controller;

import com.abernathy.medilaboui.model.Patient;
import com.abernathy.medilaboui.model.MedicalHistoryNote;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/patients")
public class PatientController {

    private final RestTemplate restTemplate;

    public PatientController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private final String backendBaseUrl = "http://localhost:8081/api/patients";
    private final String medicalNotesBaseUrl = "http://localhost:8083/api/patients";

    // -----------------------
    // List all patients
    // -----------------------
    @GetMapping("/all")
    public String listPatients(Model model) {
        String backendUrl = "http://localhost:8081/api/patients/all"; // use /all endpoint
        ResponseEntity<Patient[]> response =
                restTemplate.getForEntity(backendUrl, Patient[].class); // use backendUrl here

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
    public String addPatient(@ModelAttribute Patient patient) {
        restTemplate.postForEntity(backendBaseUrl, patient, Patient.class);
        return "redirect:/patients/all";
    }

    // -----------------------
    // Show edit patient form + notes
    // -----------------------
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        // Fetch patient
        Patient patient = restTemplate.getForObject(backendBaseUrl + "?id=" + id, Patient.class);
        if (patient == null) {
            model.addAttribute("errorMessage", "Patient not found");
            return "error";
        }
        model.addAttribute("patient", patient);

        // Fetch notes
        MedicalHistoryNote[] notesArray = restTemplate.getForObject(
                medicalNotesBaseUrl + "/history?patientId=" + id, MedicalHistoryNote[].class);
        List<MedicalHistoryNote> notes = notesArray != null ? Arrays.asList(notesArray) : Collections.emptyList();
        model.addAttribute("notes", notes);

        // Prepare empty note object for the form
        model.addAttribute("newNote", new MedicalHistoryNote());

        return "edit-patient"; // Thymeleaf template
    }



    // Handle patient update
    @PostMapping("/edit/{id}")
    public String updatePatientAndAddNote(
            @PathVariable Long id,
            @ModelAttribute Patient patient,
            @RequestParam(required = false) String physician,
            @RequestParam(required = false) String note) {

        // 1️⃣ Update patient info
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Patient> requestPatient = new HttpEntity<>(patient, headers);
        restTemplate.exchange(
                "http://localhost:8081/api/patients?id=" + id,
                HttpMethod.PUT, requestPatient, Patient.class
        );

        // 2️⃣ Add medical note if provided
        if (physician != null && !physician.isEmpty() && note != null && !note.isEmpty()) {
            // Create note object
            MedicalHistoryNote newNote = new MedicalHistoryNote();
            newNote.setPatientId(id);
            newNote.setPhysician(physician);
            newNote.setNote(note);
            newNote.setPatientName(patient.getFirstName());

            HttpEntity<MedicalHistoryNote> noteRequest = new HttpEntity<>(newNote, headers);

            // POST JSON body to backend
            restTemplate.postForEntity(
                    "http://localhost:8083/api/patients/history",
                    noteRequest,
                    MedicalHistoryNote.class
            );
        }
        // 3️⃣ Stay on the same edit page
        return "redirect:/patients/edit/" + id;
    }




}

//import com.abernathy.medilaboui.model.Patient;
//import org.springframework.http.*;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.ui.Model;
//
//
//@Controller
//@RequestMapping("/patients")
//public class PatientController {
//    private final RestTemplate restTemplate;
//
//    public PatientController(RestTemplate restTemplate) {
//        this.restTemplate = restTemplate;
//    }
//
//    private final String backendUrl = "http://localhost:8081/api/patients";
//
//    @GetMapping("/all")
//    public String listPatients(Model model) {
//         String backendUrl = "http://localhost:8081/api/patients/all";
//        ResponseEntity<Patient[]> response = restTemplate.getForEntity(backendUrl, Patient[].class);
//        Patient[] patients = response.getBody();
//        model.addAttribute("patients", patients);
//        return "list-patients"; // Thymeleaf template
//    }
//
//    @GetMapping("/add")
//    public String showAddForm(Model model) {
//        model.addAttribute("patient", new Patient());
//        return "add-patient";
//    }
//
//    // Handle submit
//    @PostMapping("/add")
//    public String addPatient(@ModelAttribute Patient patient) {
//        restTemplate.postForEntity(backendUrl, patient, Patient.class);
//        return "redirect:/patients/all"; // Redirect to list after adding
//    }
//
//    // Show edit form
//    @GetMapping("/edit/{id}")
//    public String showEditForm(@PathVariable Long id, Model model) {
//        Patient patient = restTemplate.getForObject(backendUrl + "?id=" + id, Patient.class);
//        model.addAttribute("patient", patient);
//        return "edit-patient"; // Thymeleaf template
//    }
//
//    // Handle form submission
//    @PostMapping("/edit/{id}")
//    public String updatePatient(@PathVariable("id") Long id, @ModelAttribute Patient patient) {
//        // Prepare headers for JSON
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        // Wrap patient in HttpEntity
//        HttpEntity<Patient> request = new HttpEntity<>(patient, headers);
//
//        // Include id as request param
//        String urlWithParam = backendUrl + "?id=" + id;
//
//        restTemplate.exchange(urlWithParam, HttpMethod.PUT, request, Patient.class);
//
//        return "redirect:/patients/all";
//    }
////    @DeleteMapping("/patients/{id}")
////    public String deletePatient(@PathVariable Long id) {
////        // Include id as request param
////        String urlWithParam = backendUrl + "?id=" + id;
////
////        restTemplate.exchange(urlWithParam, HttpMethod.PUT, request, Patient.class);
////
////        return "redirect:/patients"; // after delete, go back to patient list
////    }
//}
