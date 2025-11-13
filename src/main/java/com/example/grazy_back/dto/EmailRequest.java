package com.example.grazy_back.dto;

import java.util.List;

import lombok.Data;

@Data
public class EmailRequest 
{
    private List<String> to;
    private String subject;
    private String body;
    private List<String> cc;
    private List<String> bcc;
    private boolean html;
}