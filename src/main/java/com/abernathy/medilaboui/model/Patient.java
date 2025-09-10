package com.abernathy.medilaboui.model;


import lombok.Data;

import java.time.LocalDate;

@Data
public class Patient {

    private Long patientId;
    private String firstName;
    private String lastName;
    private LocalDate dob;
    private String address;
    private String phone;
}
