package br.univille.odontocare.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String jwtSecret,
        int jwtExpirationHours,
        Admin admin,
        boolean seedDemo,
        String timezone,
        List<String> corsOrigins,
        String medicasSenha) {

    public record Admin(String email, String senha, String nome) {}

    public ZoneId zone() {
        return ZoneId.of(timezone == null || timezone.isBlank() ? "America/Sao_Paulo" : timezone);
    }

    public LocalDate hoje() {
        return LocalDate.now(Clock.system(zone()));
    }
}
