package com.abernathy.medilaboui.controller;

import com.abernathy.medilaboui.model.Patient;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.ui.Model;


@Controller
@RequestMapping("/patients")
public class PatientController {
    private final RestTemplate restTemplate;

    public PatientController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private final String backendUrl = "http://localhost:8081/api/patients";

    @GetMapping("/all")
    public String listPatients(Model model) {
         String backendUrl = "http://localhost:8081/api/patients/all";
        ResponseEntity<Patient[]> response = restTemplate.getForEntity(backendUrl, Patient[].class);
        Patient[] patients = response.getBody();
        model.addAttribute("patients", patients);
        return "list-patients"; // Thymeleaf template
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("patient", new Patient());
        return "add-patient";
    }

    // Handle submit
    @PostMapping("/add")
    public String addPatient(@ModelAttribute Patient patient) {
        restTemplate.postForEntity(backendUrl, patient, Patient.class);
        return "redirect:/patients/all"; // Redirect to list after adding
    }

    // Show edit form
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Patient patient = restTemplate.getForObject(backendUrl + "?id=" + id, Patient.class);
        model.addAttribute("patient", patient);
        return "edit-patient"; // Thymeleaf template
    }

    // Handle form submission
    @PostMapping("/edit/{id}")
    public String updatePatient(@PathVariable("id") Long id, @ModelAttribute Patient patient) {
        // Prepare headers for JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Wrap patient in HttpEntity
        HttpEntity<Patient> request = new HttpEntity<>(patient, headers);

        // Include id as request param
        String urlWithParam = backendUrl + "?id=" + id;

        restTemplate.exchange(urlWithParam, HttpMethod.PUT, request, Patient.class);

        return "redirect:/patients/all";
    }
//    @DeleteMapping("/patients/{id}")
//    public String deletePatient(@PathVariable Long id) {
//        // Include id as request param
//        String urlWithParam = backendUrl + "?id=" + id;
//
//        restTemplate.exchange(urlWithParam, HttpMethod.PUT, request, Patient.class);
//
//        return "redirect:/patients"; // after delete, go back to patient list
//    }
}
