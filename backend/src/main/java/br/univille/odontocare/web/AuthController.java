package br.univille.odontocare.web;

import br.univille.odontocare.dto.AuthDtos.*;
import br.univille.odontocare.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    /** Público. Devolve o JWT e os dados do usuário. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req.email(), req.senha());
    }

    @GetMapping("/me")
    public UsuarioDto me(@AuthenticationPrincipal Jwt jwt) {
        return auth.perfil(jwt.getSubject());
    }

    /** Tela de Configurações → "Seu nome (usuário)". */
    @PutMapping("/me")
    public UsuarioDto atualizar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AtualizarPerfilRequest req) {
        return auth.atualizarNome(jwt.getSubject(), req.nome());
    }
}
