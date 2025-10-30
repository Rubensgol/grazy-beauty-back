package com.example.grazy_back.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.grazy_back.model.Agendamento;
import com.example.grazy_back.enums.StatusAgendamentoEnum;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> 
{

	long countByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);
	List<Agendamento> findByDataHoraBetweenOrderByDataHoraAsc(LocalDateTime inicio, LocalDateTime fim);
	List<Agendamento> findByStatusAndDataHoraBetweenOrderByDataHoraAsc(StatusAgendamentoEnum status, LocalDateTime inicio, LocalDateTime fim);
	List<Agendamento> findByStatusAndNotificadoFalseAndDataHoraBetweenOrderByDataHoraAsc(StatusAgendamentoEnum status, LocalDateTime inicio, LocalDateTime fim);

    boolean existsByServicoId(Long servicoId);
}
