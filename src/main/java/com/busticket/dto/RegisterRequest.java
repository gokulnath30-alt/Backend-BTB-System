package com.busticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String name;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;
    private String phoneNumber;
    private String role;
}
