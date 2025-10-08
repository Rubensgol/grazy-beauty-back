package com.example.grazy_back.dto;

public class LoginRequest
{
    private String usuario;
    private String senha;

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
