package com.abernathy.medilaboui.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class Patient {

    private Long patientId;
    private String firstName;
    private String lastName;
    @DateTimeFormat(pattern = "yyyy-MM-dd")   // for binding form -> object
    @JsonFormat(pattern = "yyyy-MM-dd")       // for JSON -> object
    private LocalDate dob;
    private String address;
    private String phone;
    private String gender;
}
