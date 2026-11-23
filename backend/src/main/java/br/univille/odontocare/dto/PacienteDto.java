package br.univille.odontocare.dto;

import br.univille.odontocare.domain.Paciente;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Paciente no formato usado pelo front (nascimento, obs). */
public record PacienteDto(
        Long id,
        @NotBlank(message = "Informe o nome completo") @Size(min = 3, max = 150, message = "Informe o nome completo") String nome,
        @NotBlank(message = "CPF inválido") String cpf,
        @NotBlank(message = "Telefone inválido") @Size(max = 20, message = "Telefone inválido") String telefone,
        @Email(message = "E-mail inválido") @Size(max = 160, message = "E-mail muito longo") String email,
        @PastOrPresent(message = "Data futura") LocalDate nascimento,
        @Size(max = 60) String convenio,
        String obs,
        String responsavel) {

    public static PacienteDto from(Paciente p) {
        return new PacienteDto(p.getId(), p.getNome(), p.getCpf(), p.getTelefone(),
                p.getEmail() == null ? "" : p.getEmail(),
                p.getDataNascimento(), p.getConvenio(),
                p.getObservacoes() == null ? "" : p.getObservacoes(), p.nomeDoDono());
    }
}
