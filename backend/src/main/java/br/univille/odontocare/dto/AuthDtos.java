package br.univille.odontocare.dto;

import br.univille.odontocare.domain.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Tipos usados em /api/auth. */
public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank(message = "Informe o e-mail") String email,
            @NotBlank(message = "Informe a senha") String senha) {}

    public record UsuarioDto(Long id, String nome, String email, String cargo) {
        public static UsuarioDto from(Usuario u) {
            return new UsuarioDto(u.getId(), u.getNome(), u.getEmail(), cargoLegivel(u.getCargo()));
        }

        private static String cargoLegivel(String cargo) {
            if (cargo == null) return "";
            return switch (cargo) {
                case "ADMIN" -> "Administrador";
                case "RECEPCAO" -> "Recepção";
                case "DENTISTA" -> "Dentista";
                default -> cargo;
            };
        }
    }

    public record LoginResponse(String token, long expiraEmSegundos, UsuarioDto usuario) {}

    public record AlterarSenhaRequest(
            @NotBlank(message = "Informe a senha atual") String senhaAtual,
            @NotBlank(message = "Informe a nova senha") @Size(min = 8, max = 72, message = "A nova senha precisa ter de 8 a 72 caracteres") String novaSenha) {}

    public record AtualizarPerfilRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 120, message = "Nome muito longo") String nome) {}
}
