package br.univille.odontocare.web;

import br.univille.odontocare.config.UsuarioAtual;
import br.univille.odontocare.dto.ConfiguracaoDto;
import br.univille.odontocare.service.BackupService;
import br.univille.odontocare.service.ConfiguracaoService;
import br.univille.odontocare.service.DashboardService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.Map;

/** Configurações, Dashboard e Backup. */
@RestController
@RequestMapping("/api")
public class SistemaController {

    private final ConfiguracaoService configuracao;
    private final DashboardService dashboard;
    private final BackupService backup;
    private final UsuarioAtual atual;

    public SistemaController(ConfiguracaoService configuracao, DashboardService dashboard, BackupService backup,
                             UsuarioAtual atual) {
        this.atual = atual;
        this.configuracao = configuracao;
        this.dashboard = dashboard;
        this.backup = backup;
    }

    @GetMapping("/configuracoes")
    public ConfiguracaoDto obterConfig() {
        return configuracao.obter();
    }

    @PutMapping("/configuracoes")
    public ConfiguracaoDto salvarConfig(@Valid @RequestBody ConfiguracaoDto dto) {
        atual.exigirAdmin();
        return configuracao.atualizar(dto);
    }

    /** GET /api/dashboard?mes=2026-11 */
    @GetMapping("/dashboard")
    public DashboardService.Resumo dashboard(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return dashboard.resumo(mes);
    }

    @GetMapping("/backup")
    public Map<String, Object> exportar() {
        atual.exigirAdmin();
        return backup.exportar();
    }

    /** Substitui todos os dados pelo conteúdo do arquivo (mesmo formato do GET /api/backup). */
    @PostMapping("/backup")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void importar(@RequestBody JsonNode json) {
        atual.exigirAdmin();
        backup.importar(json, atual.entidade());
    }

    /** Apaga tudo e carrega os dados de exemplo. */
    @PostMapping("/backup/restaurar-exemplo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restaurarExemplo() {
        atual.exigirAdmin();
        backup.restaurarExemplo(atual.entidade());
    }
}
