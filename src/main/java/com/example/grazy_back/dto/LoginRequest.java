package com.example.grazy_back.dto;

import lombok.Data;

@Data
public class LoginRequest
{
    private String usuario;
    private String senha;
    private Long tenantId;

    public String getUsername() 
    { 
        return usuario; 
    }
    
    public void setUsuario(String usuario)
    { 
        this.usuario = usuario; 
    }

    public String getSenha() 
    {
         return senha; 
    }

    public void setSenha(String senha) 
    { 
        this.senha = senha; 
    }
}
