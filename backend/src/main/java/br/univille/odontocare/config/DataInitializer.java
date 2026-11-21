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

/**
 * Na primeira subida:
 * 1. cria o usuário administrador (a migração não cria usuário: a senha precisa ser BCrypt);
 * 2. se o banco estiver vazio e app.seed-demo=true, carrega os dados de exemplo.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

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
        AppProperties.Admin admin = props.admin();
        if (admin != null && admin.email() != null && !usuarios.existsByEmailIgnoreCase(admin.email())) {
            Usuario u = new Usuario();
            u.setEmail(admin.email().trim().toLowerCase());
            u.setNome(admin.nome() == null || admin.nome().isBlank() ? "Administrador" : admin.nome());
            u.setSenhaHash(encoder.encode(admin.senha()));
            u.setCargo("ADMIN");
            usuarios.save(u);
            log.info("Usuário administrador criado: {}", u.getEmail());
        }

        if (props.seedDemo() && backup.bancoVazio()) {
            backup.restaurarExemplo();
            log.info("Banco vazio: dados de exemplo carregados (desligue com SEED_DEMO=false)");
        }
    }
}
