package com.example.grazy_back.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResposta<T> 
{
    private boolean success = true;
    private T data;
    private String message;
    private Instant timestamp = Instant.now();

    public static <T> ApiResposta<T> of(T data) 
    {
        return new ApiResposta<>(true, data, null, Instant.now());
    }

    public static <T> ApiResposta<T> of(T data, String message) 
    {
        return new ApiResposta<>(true, data, message, Instant.now());
    }

    public static <T> ApiResposta<T> error(String message) 
    {
        return new ApiResposta<>(false, null, message, Instant.now());
    }
}
