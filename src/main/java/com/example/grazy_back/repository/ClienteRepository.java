package com.example.grazy_back.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.grazy_back.model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> 
{
}
