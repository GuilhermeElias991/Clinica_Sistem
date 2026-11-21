package br.univille.odontocare.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/** Corpo de POST/PUT /api/agendamentos. A hora de fim é calculada pela duração do procedimento. */
public record AgendamentoRequest(
        @NotNull(message = "Selecione o paciente") Long pacienteId,
        @NotNull(message = "Selecione o dentista") Long dentistaId,
        @NotNull(message = "Selecione o procedimento") Long procedimentoId,
        @NotNull(message = "Informe a data") LocalDate data,
        @NotNull(message = "Selecione um horário") @JsonFormat(pattern = "HH:mm") LocalTime hora,
        String status,
        String obs) {
}
