package br.univille.odontocare.web;

import br.univille.odontocare.dto.AgendamentoDto;
import br.univille.odontocare.dto.AgendamentoRequest;
import br.univille.odontocare.exception.ApiException;
import br.univille.odontocare.service.AgendamentoService;
import br.univille.odontocare.service.AgendamentoService.Filtro;
import br.univille.odontocare.service.AgendamentoService.Horario;
import br.univille.odontocare.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    private final AgendamentoService service;
    private final AuthService auth;

    public AgendamentoController(AgendamentoService service, AuthService auth) {
        this.service = service;
        this.auth = auth;
    }

    /** GET /api/agendamentos?inicio=2026-11-01&fim=2026-11-30&dentistaId=1&pacienteId=2&status=agendado */
    @GetMapping
    public List<AgendamentoDto> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Long dentistaId,
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) String status) {
        return service.listar(new Filtro(inicio, fim, dentistaId, pacienteId, status));
    }

    /** GET /api/agendamentos/horarios-livres?data=2026-11-23&dentistaId=1&procedimentoId=3[&pacienteId=&ignorarId=] */
    @GetMapping("/horarios-livres")
    public List<Horario> horariosLivres(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam Long dentistaId,
            @RequestParam(required = false) Long procedimentoId,
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) Long ignorarId) {
        return service.horariosLivres(data, dentistaId, procedimentoId, pacienteId, ignorarId);
    }

    @GetMapping("/{id}")
    public AgendamentoDto buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    /** 409 Conflict se o dentista ou o paciente já tiver consulta no horário. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgendamentoDto criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AgendamentoRequest req) {
        return service.criar(req, auth.idDoUsuario(jwt.getSubject()));
    }

    @PutMapping("/{id}")
    public AgendamentoDto atualizar(@PathVariable Long id, @Valid @RequestBody AgendamentoRequest req) {
        return service.atualizar(id, req);
    }

    /** Corpo: {"status": "confirmado"} — agendado, confirmado, concluido, cancelado, faltou. */
    @PatchMapping("/{id}/status")
    public AgendamentoDto status(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body == null ? null : body.get("status");
        if (status == null) throw ApiException.campo("status", "Informe o status");
        return service.alterarStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
