package org.example.dto;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Data
public class CallbackRequest {
    private String name;
    private String phone;
    private String service;
    private String comment;
    private String email;
    private boolean sendDoc = true;
    private boolean sendExcel = true;


}

