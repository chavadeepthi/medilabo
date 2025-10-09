package com.abernathy.medilaboui.model;


import lombok.Data;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class MedicalHistoryNote {
    private String id;          // MongoDB _id
    private Long patientId;     // reference to Patient
    private String physician;
    private String note;
    private LocalDateTime createdAt;
    private String patientName;

    public String getCreatedAtFormatted() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return createdAt != null ? createdAt.format(formatter) : "";
    }
}
