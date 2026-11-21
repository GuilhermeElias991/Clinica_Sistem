package br.univille.odontocare.dto;

import br.univille.odontocare.domain.Dentista;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DentistaDto(
        Long id,
        @NotBlank(message = "Informe o nome") @Size(min = 3, max = 150, message = "Informe o nome") String nome,
        @NotBlank(message = "Informe o CRO") @Size(max = 30, message = "CRO muito longo") String cro,
        @NotBlank(message = "Informe a especialidade") @Size(max = 80) String especialidade,
        @Size(max = 20, message = "Telefone inválido") String telefone,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Cor inválida (use #RRGGBB)") String cor,
        Boolean ativo) {

    public static DentistaDto from(Dentista d) {
        return new DentistaDto(d.getId(), d.getNome(), d.getCro(), d.getEspecialidade(),
                d.getTelefone() == null ? "" : d.getTelefone(), d.getCor(), d.isAtivo());
    }
}
