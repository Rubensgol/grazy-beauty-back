package com.example.grazy_back.dto.whatsapp;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WhatsappStatusResponse {
    
    private String status; // "open", "close", "connecting"
    private String phoneNumber;
    private String instanceName;
    private String qrCode;
    private String message;
    
    public WhatsappStatusResponse() {}
    
    public WhatsappStatusResponse(String status) {
        this.status = status;
    }
    
    public static WhatsappStatusResponse connected(String phoneNumber, String instanceName) {
        WhatsappStatusResponse response = new WhatsappStatusResponse("open");
        response.setPhoneNumber(phoneNumber);
        response.setInstanceName(instanceName);
        return response;
    }
    
    public static WhatsappStatusResponse disconnected() {
        return new WhatsappStatusResponse("close");
    }
    
    public static WhatsappStatusResponse connecting(String qrCode) {
        WhatsappStatusResponse response = new WhatsappStatusResponse("connecting");
        response.setQrCode(qrCode);
        return response;
    }
    
    public static WhatsappStatusResponse error(String message) {
        WhatsappStatusResponse response = new WhatsappStatusResponse("error");
        response.setMessage(message);
        return response;
    }
    
    // Getters e Setters
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getInstanceName() {
        return instanceName;
    }
    
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }
    
    public String getQrCode() {
        return qrCode;
    }
    
    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
