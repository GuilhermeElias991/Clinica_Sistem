package br.univille.odontocare.config;

import br.univille.odontocare.domain.Usuario;
import br.univille.odontocare.repository.UsuarioRepository;
import br.univille.odontocare.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Na primeira subida:
 * 1. cria o usuário administrador (a migração não cria usuário: a senha precisa ser BCrypt);
 * 2. cria as médicas (cada uma enxerga só os próprios pacientes; o administrador enxerga tudo);
 * 3. se o banco estiver vazio e app.seed-demo=true, carrega os dados de exemplo (que ficam com o administrador).
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private record Medica(String nome, String email) {}

    /** Contas das médicas. A senha inicial vem de app.medicas-senha (variável MEDICAS_SENHA). */
    private static final List<Medica> MEDICAS = List.of(
            new Medica("Dra. Ana Souza", "ana@odonto.com"),
            new Medica("Dra. Carla Mendes", "carla@odonto.com"),
            new Medica("Dra. Helena Ribeiro", "helena@odonto.com"),
            new Medica("Dra. Mariana Costa", "mariana@odonto.com"));

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final BackupService backup;
    private final AppProperties props;

    public DataInitializer(UsuarioRepository usuarios, PasswordEncoder encoder, BackupService backup, AppProperties props) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.backup = backup;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Usuario donoDoExemplo = null;
        AppProperties.Admin admin = props.admin();
        if (admin != null && admin.email() != null) {
            donoDoExemplo = usuarios.findByEmailIgnoreCase(admin.email().trim()).orElse(null);
            if (donoDoExemplo == null) {
                Usuario u = new Usuario();
                u.setEmail(admin.email().trim().toLowerCase());
                u.setNome(admin.nome() == null || admin.nome().isBlank() ? "Administrador" : admin.nome());
                u.setSenhaHash(encoder.encode(admin.senha()));
                u.setCargo("ADMIN");
                donoDoExemplo = usuarios.save(u);
                log.info("Usuário administrador criado: {}", u.getEmail());
            }
        }

        criarMedicas();

        if (donoDoExemplo != null && props.seedDemo() && backup.bancoVazio()) {
            backup.restaurarExemplo(donoDoExemplo);
            log.info("Banco vazio: dados de exemplo carregados (desligue com SEED_DEMO=false)");
        }
    }

    private void criarMedicas() {
        String senha = props.medicasSenha();
        if (senha == null || senha.isBlank()) {
            log.warn("MEDICAS_SENHA não definida: contas das médicas não foram criadas");
            return;
        }
        for (Medica m : MEDICAS) {
            if (usuarios.existsByEmailIgnoreCase(m.email())) continue;
            Usuario u = new Usuario();
            u.setNome(m.nome());
            u.setEmail(m.email());
            u.setSenhaHash(encoder.encode(senha));
            u.setCargo("DENTISTA");
            usuarios.save(u);
            log.info("Usuária criada: {}", m.email());
        }
    }
}
