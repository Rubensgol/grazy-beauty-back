package com.example.grazy_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GrazyBackApplication 
{
	public static void main(String[] args)
	{
		SpringApplication.run(GrazyBackApplication.class, args);
	}
}
