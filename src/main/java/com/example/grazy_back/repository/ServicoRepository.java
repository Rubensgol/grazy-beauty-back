package com.example.grazy_back.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.grazy_back.model.Servico;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, Long> {
}