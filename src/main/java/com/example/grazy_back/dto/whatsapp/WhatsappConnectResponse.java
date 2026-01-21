package com.example.grazy_back.dto.whatsapp;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WhatsappConnectResponse {
    
    private boolean success;
    private String qrCode; // base64 data URL
    private String pairingCode; // código para parear (alternativa ao QR)
    private String message;
    private String instanceName;
    
    public WhatsappConnectResponse() {}
    
    public static WhatsappConnectResponse withQrCode(String qrCode, String instanceName) {
        WhatsappConnectResponse response = new WhatsappConnectResponse();
        response.setSuccess(true);
        response.setQrCode(qrCode);
        response.setInstanceName(instanceName);
        return response;
    }
    
    public static WhatsappConnectResponse alreadyConnected(String message) {
        WhatsappConnectResponse response = new WhatsappConnectResponse();
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }
    
    public static WhatsappConnectResponse error(String message) {
        WhatsappConnectResponse response = new WhatsappConnectResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
    
    // Getters e Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getQrCode() {
        return qrCode;
    }
    
    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }
    
    public String getPairingCode() {
        return pairingCode;
    }
    
    public void setPairingCode(String pairingCode) {
        this.pairingCode = pairingCode;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getInstanceName() {
        return instanceName;
    }
    
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }
}
