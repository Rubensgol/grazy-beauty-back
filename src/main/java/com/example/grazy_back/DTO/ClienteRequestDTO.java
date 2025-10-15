package com.example.grazy_back.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClienteRequestDTO 
{
    private String nome;
    private String telefone;
    private String email;
    private String obs;
}
