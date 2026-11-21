package br.univille.odontocare.dto;

import br.univille.odontocare.domain.Configuracao;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/** Configurações no formato do front: clinica, abertura, fechamento, intervalo, sabado. */
public record ConfiguracaoDto(
        @NotBlank(message = "Informe o nome da clínica") @Size(max = 120) String clinica,
        @NotNull(message = "Informe a abertura") @JsonFormat(pattern = "HH:mm") LocalTime abertura,
        @NotNull(message = "Informe o fechamento") @JsonFormat(pattern = "HH:mm") LocalTime fechamento,
        @NotNull(message = "Intervalo inválido") Integer intervalo,
        Boolean sabado) {

    public static ConfiguracaoDto from(Configuracao c) {
        return new ConfiguracaoDto(c.getNomeClinica(), c.getAbertura(), c.getFechamento(),
                c.getIntervaloMin(), c.isAtendeSabado());
    }
}
