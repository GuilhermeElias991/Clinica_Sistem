package br.univille.odontocare.dto;

import br.univille.odontocare.domain.Procedimento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Procedimento no formato do front: duracao = minutos. */
public record ProcedimentoDto(
        Long id,
        @NotBlank(message = "Informe o nome") @Size(max = 120) String nome,
        @NotNull(message = "Mínimo 10 min") @Min(value = 10, message = "Mínimo 10 min") @Max(value = 600, message = "Máximo 600 min") Integer duracao,
        @NotNull(message = "Valor inválido") @DecimalMin(value = "0", message = "Valor inválido") @Digits(integer = 8, fraction = 2, message = "Valor inválido") BigDecimal valor,
        Boolean ativo) {

    public static ProcedimentoDto from(Procedimento p) {
        return new ProcedimentoDto(p.getId(), p.getNome(), p.getDuracaoMin(), p.getValor(), p.isAtivo());
    }
}
