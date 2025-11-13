package com.example.grazy_back.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidationResponse
{
    private boolean valid;
    private String message;
    private String username;

    public ValidationResponse(boolean valid, String message, String username)
    {
        this.valid = valid;
        this.message = message;
        this.username = username;
    }
}