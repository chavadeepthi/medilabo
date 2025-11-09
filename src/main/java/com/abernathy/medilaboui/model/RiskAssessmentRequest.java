package com.abernathy.medilaboui.model;

import java.util.List;

public class RiskAssessmentRequest {

    private Patient patient;
    private List<String> notes;

    // Constructors
    public RiskAssessmentRequest() {}

    public RiskAssessmentRequest(Patient patient, List<String> notes) {
        this.patient = patient;
        this.notes = notes;
    }

    // Getters and Setters
    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(List<String> notes) {
        this.notes = notes;
    }
}

