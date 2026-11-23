package br.univille.odontocare.config;

import br.univille.odontocare.domain.Proprietario;
import br.univille.odontocare.domain.Usuario;
import br.univille.odontocare.exception.ApiException;
import br.univille.odontocare.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Quem está logado na requisição atual (lido do JWT).
 * Regra de acesso: o ADMIN enxerga tudo; os demais, só o que eles mesmos cadastraram.
 */
@Component
public class UsuarioAtual {

    private final UsuarioRepository usuarios;

    public UsuarioAtual(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    private Jwt jwt() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof Jwt jwt) return jwt;
        throw new ApiException(HttpStatus.UNAUTHORIZED, "Sessão inválida");
    }

    public Long id() {
        Object uid = jwt().getClaim("uid");
        if (uid instanceof Number n) return n.longValue();
        throw new ApiException(HttpStatus.UNAUTHORIZED, "Sessão inválida");
    }

    public boolean admin() {
        return "ADMIN".equals(jwt().getClaimAsString("cargo"));
    }

    public void exigirAdmin() {
        if (!admin()) throw new ApiException(HttpStatus.FORBIDDEN, "Apenas o administrador pode fazer isso");
    }

    /** Usuário logado como entidade (para gravar como dono de um registro). */
    public Usuario entidade() {
        return usuarios.getReferenceById(id());
    }

    /** Id para filtrar consultas: null = sem filtro (administrador). */
    public Long filtroDono() {
        return admin() ? null : id();
    }

    public boolean podeAcessar(Proprietario p) {
        if (admin()) return true;
        return p.getDono() != null && p.getDono().getId().equals(id());
    }

    /** Devolve o registro se o usuário puder vê-lo; senão age como se não existisse (404). */
    public <T extends Proprietario> T permitir(T registro, String naoEncontrado) {
        if (registro == null || !podeAcessar(registro)) throw ApiException.notFound(naoEncontrado);
        return registro;
    }
}
