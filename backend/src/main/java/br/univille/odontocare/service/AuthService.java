package br.univille.odontocare.service;

import br.univille.odontocare.config.AppProperties;
import br.univille.odontocare.domain.Usuario;
import br.univille.odontocare.dto.AuthDtos.LoginResponse;
import br.univille.odontocare.dto.AuthDtos.UsuarioDto;
import br.univille.odontocare.exception.ApiException;
import br.univille.odontocare.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class AuthService {

    /** Hash calculado uma vez para gastar o mesmo tempo quando o e-mail não existe. */
    private final String hashFalso;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;
    private final AppProperties props;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtEncoder jwtEncoder, AppProperties props) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwtEncoder = jwtEncoder;
        this.props = props;
        this.hashFalso = encoder.encode("senha-que-nao-existe");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String email, String senha) {
        Usuario u = usuarios.findByEmailIgnoreCase(email.trim()).orElse(null);
        boolean senhaOk = encoder.matches(senha, u == null ? hashFalso : u.getSenhaHash());
        if (u == null || !senhaOk || !u.isAtivo()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
        }
        Duration validade = Duration.ofHours(Math.max(1, props.jwtExpirationHours()));
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("odontocare-api")
                .subject(u.getEmail())
                .issuedAt(agora)
                .expiresAt(agora.plus(validade))
                .claim("uid", u.getId())
                .claim("nome", u.getNome())
                .claim("cargo", u.getCargo())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResponse(token, validade.toSeconds(), UsuarioDto.from(u));
    }

    @Transactional(readOnly = true)
    public UsuarioDto perfil(String email) {
        return UsuarioDto.from(obter(email));
    }

    @Transactional
    public UsuarioDto atualizarNome(String email, String nome) {
        Usuario u = obter(email);
        u.setNome(nome.trim());
        return UsuarioDto.from(u);
    }

    @Transactional
    public void alterarSenha(String email, String senhaAtual, String novaSenha) {
        Usuario u = obter(email);
        if (!encoder.matches(senhaAtual, u.getSenhaHash())) {
            throw ApiException.campo("senhaAtual", "Senha atual incorreta");
        }
        if (senhaAtual.equals(novaSenha)) {
            throw ApiException.campo("novaSenha", "A nova senha deve ser diferente da atual");
        }
        u.setSenhaHash(encoder.encode(novaSenha));
    }

    @Transactional(readOnly = true)
    public Long idDoUsuario(String email) {
        return usuarios.findByEmailIgnoreCase(email).map(Usuario::getId).orElse(null);
    }

    private Usuario obter(String email) {
        return usuarios.findByEmailIgnoreCase(email)
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sessão inválida"));
    }
}
