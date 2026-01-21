package com.example.grazy_back.dto.whatsapp;

public class SendMessageRequest {
    
    private String phoneNumber;
    private String message;
    
    public SendMessageRequest() {}
    
    public SendMessageRequest(String phoneNumber, String message) {
        this.phoneNumber = phoneNumber;
        this.message = message;
    }
    
    // Getters e Setters
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
