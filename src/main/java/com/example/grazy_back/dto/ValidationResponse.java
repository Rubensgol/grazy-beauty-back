package com.example.grazy_back.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResponse
{
    private boolean valid;
    private String message;
    private String username;
    private String role;
    private Long tenantId;

    public ValidationResponse(boolean valid, String message, String username)
    {
        this.valid = valid;
        this.message = message;
        this.username = username;
    }
}