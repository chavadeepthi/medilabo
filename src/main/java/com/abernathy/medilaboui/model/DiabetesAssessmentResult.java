package com.abernathy.medilaboui.model;

import lombok.Data;

@Data
public class DiabetesAssessmentResult {
    private Integer patientId;
    private String firstName;
    private String lastName;
    private Integer age;
    private String gender;
    private String riskLevel;
}
