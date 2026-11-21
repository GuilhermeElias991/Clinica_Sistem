package br.univille.odontocare.dto;

import br.univille.odontocare.domain.Agendamento;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Agendamento como o front espera: hora = início (HH:mm), horaFim calculada pelo back. */
public record AgendamentoDto(
        Long id,
        Long pacienteId,
        Long dentistaId,
        Long procedimentoId,
        LocalDate data,
        @JsonFormat(pattern = "HH:mm") LocalTime hora,
        @JsonFormat(pattern = "HH:mm") LocalTime horaFim,
        String status,
        BigDecimal valor,
        String obs,
        LocalDateTime criadoEm) {

    public static AgendamentoDto from(Agendamento a) {
        return new AgendamentoDto(a.getId(), a.getPaciente().getId(), a.getDentista().getId(),
                a.getProcedimento().getId(), a.getData(), a.getHoraInicio(), a.getHoraFim(),
                a.getStatus(), a.getValor(), a.getObservacoes() == null ? "" : a.getObservacoes(),
                a.getCriadoEm());
    }
}
