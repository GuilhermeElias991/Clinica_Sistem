package br.univille.odontocare.service;

import br.univille.odontocare.config.AppProperties;
import br.univille.odontocare.config.UsuarioAtual;
import br.univille.odontocare.config.UsuarioAtual;
import br.univille.odontocare.domain.*;
import br.univille.odontocare.dto.AgendamentoDto;
import br.univille.odontocare.dto.AgendamentoRequest;
import br.univille.odontocare.exception.ApiException;
import br.univille.odontocare.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Regras de agendamento. Tudo que o front valida também é validado aqui,
 * porque duas recepcionistas podem agendar ao mesmo tempo.
 */
@Service
public class AgendamentoService {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final AgendamentoRepository agendamentos;
    private final PacienteRepository pacientes;
    private final DentistaRepository dentistas;
    private final ProcedimentoRepository procedimentos;
    private final UsuarioRepository usuarios;
    private final ConfiguracaoService configuracao;
    private final AppProperties props;
    private final UsuarioAtual atual;

    public AgendamentoService(AgendamentoRepository agendamentos, PacienteRepository pacientes,
                              DentistaRepository dentistas, ProcedimentoRepository procedimentos,
                              UsuarioRepository usuarios, ConfiguracaoService configuracao, AppProperties props,
                              UsuarioAtual atual) {
        this.atual = atual;
        this.agendamentos = agendamentos;
        this.pacientes = pacientes;
        this.dentistas = dentistas;
        this.procedimentos = procedimentos;
        this.usuarios = usuarios;
        this.configuracao = configuracao;
        this.props = props;
    }

    public record Filtro(LocalDate inicio, LocalDate fim, Long dentistaId, Long pacienteId, String status) {}

    public record Horario(@com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm") LocalTime hora, boolean livre) {}

    // ------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public List<AgendamentoDto> listar(Filtro f) {
        Specification<Agendamento> spec = (root, q, cb) -> cb.conjunction();
        Long dono = atual.filtroDono();
        if (dono != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("dono").get("id"), dono));
        if (f.inicio() != null) spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("data"), f.inicio()));
        if (f.fim() != null) spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("data"), f.fim()));
        if (f.dentistaId() != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("dentista").get("id"), f.dentistaId()));
        if (f.pacienteId() != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("paciente").get("id"), f.pacienteId()));
        if (f.status() != null && !f.status().isBlank()) {
            if (!StatusAgendamento.valido(f.status())) throw ApiException.badRequest("Status inválido: " + f.status());
            spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), f.status()));
        }
        return agendamentos.findAll(spec, Sort.by("data", "horaInicio", "id")).stream().map(AgendamentoDto::from).toList();
    }

    @Transactional(readOnly = true)
    public AgendamentoDto buscar(Long id) {
        return AgendamentoDto.from(obter(id));
    }

    /** Botões de horário do formulário: cada slot do expediente e se está livre. */
    @Transactional(readOnly = true)
    public List<Horario> horariosLivres(LocalDate data, Long dentistaId, Long procedimentoId, Long pacienteId, Long ignorarId) {
        atual.permitir(dentistas.findById(dentistaId).orElse(null), "Dentista não encontrado");
        if (pacienteId != null) atual.permitir(pacientes.findById(pacienteId).orElse(null), "Paciente não encontrado");
        Configuracao cfg = configuracao.atual();
        int passo = cfg.getIntervaloMin();
        int duracao = procedimentoId == null ? passo : procedimentos.findById(procedimentoId)
                .orElseThrow(() -> ApiException.notFound("Procedimento não encontrado")).getDuracaoMin();
        long ignorar = ignorarId == null ? -1L : ignorarId;
        long paciente = pacienteId == null ? -1L : pacienteId;

        List<Horario> lista = new ArrayList<>();
        int ini = cfg.getAbertura().toSecondOfDay() / 60;
        int fimExpediente = cfg.getFechamento().toSecondOfDay() / 60;
        for (int m = ini; m + duracao <= fimExpediente; m += passo) {
            LocalTime h = LocalTime.of(m / 60, m % 60);
            boolean livre = agendamentos.findConflitos(data, h, h.plusMinutes(duracao), dentistaId, paciente, ignorar).isEmpty();
            lista.add(new Horario(h, livre));
        }
        return lista;
    }

    // ------------------------------------------------------------------ escrita

    @Transactional
    public AgendamentoDto criar(AgendamentoRequest req, Long usuarioId) {
        String status = statusOuPadrao(req.status());
        // trava paciente e dentista sempre nessa ordem (evita deadlock e reserva dupla)
        Paciente paciente = pacientes.findByIdForUpdate(req.pacienteId())
                .orElseThrow(() -> ApiException.campo("pacienteId", "Paciente não encontrado"));
        Dentista dentista = dentistas.findByIdForUpdate(req.dentistaId())
                .orElseThrow(() -> ApiException.campo("dentistaId", "Dentista não encontrado"));
        exigirAcesso(paciente, dentista);
        Procedimento proc = procedimentos.findById(req.procedimentoId())
                .orElseThrow(() -> ApiException.campo("procedimentoId", "Procedimento não encontrado"));

        if (!dentista.isAtivo()) throw ApiException.campo("dentistaId", "Dentista inativo");
        if (req.data().isBefore(props.hoje())) throw ApiException.campo("data", "Não é possível agendar no passado");

        LocalTime fim = validarHorario(req.data(), req.hora(), proc);
        if (!StatusAgendamento.CANCELADO.equals(status)) {
            verificarConflito(req.data(), req.hora(), fim, dentista.getId(), paciente.getId(), -1L);
        }

        Agendamento a = new Agendamento();
        a.setPaciente(paciente);
        a.setDentista(dentista);
        a.setProcedimento(proc);
        a.setData(req.data());
        a.setHoraInicio(req.hora());
        a.setHoraFim(fim);
        a.setStatus(status);
        a.setValor(proc.getValor());
        a.setObservacoes(Documentos.vazioParaNull(req.obs()));
        a.setDono(paciente.getDono() != null ? paciente.getDono() : atual.entidade());
        if (usuarioId != null) usuarios.findById(usuarioId).ifPresent(a::setCriadoPor);
        return AgendamentoDto.from(agendamentos.save(a));
    }

    @Transactional
    public AgendamentoDto atualizar(Long id, AgendamentoRequest req) {
        Agendamento a = obter(id);
        String status = req.status() == null || req.status().isBlank() ? a.getStatus() : statusOuPadrao(req.status());

        Paciente paciente = pacientes.findByIdForUpdate(req.pacienteId())
                .orElseThrow(() -> ApiException.campo("pacienteId", "Paciente não encontrado"));
        Dentista dentista = dentistas.findByIdForUpdate(req.dentistaId())
                .orElseThrow(() -> ApiException.campo("dentistaId", "Dentista não encontrado"));
        exigirAcesso(paciente, dentista);
        Procedimento proc = procedimentos.findById(req.procedimentoId())
                .orElseThrow(() -> ApiException.campo("procedimentoId", "Procedimento não encontrado"));

        boolean trocouDentista = !Objects.equals(a.getDentista().getId(), dentista.getId());
        if (trocouDentista && !dentista.isAtivo()) throw ApiException.campo("dentistaId", "Dentista inativo");
        boolean trocouData = !req.data().equals(a.getData());
        if (trocouData && req.data().isBefore(props.hoje())) {
            throw ApiException.campo("data", "Não é possível agendar no passado");
        }

        LocalTime fim = validarHorario(req.data(), req.hora(), proc);
        if (!StatusAgendamento.CANCELADO.equals(status)) {
            verificarConflito(req.data(), req.hora(), fim, dentista.getId(), paciente.getId(), a.getId());
        }

        if (!Objects.equals(a.getProcedimento().getId(), proc.getId())) a.setValor(proc.getValor());
        a.setPaciente(paciente);
        a.setDentista(dentista);
        a.setProcedimento(proc);
        a.setData(req.data());
        a.setHoraInicio(req.hora());
        a.setHoraFim(fim);
        a.setStatus(status);
        a.setObservacoes(Documentos.vazioParaNull(req.obs()));
        return AgendamentoDto.from(a);
    }

    /** Confirmar, concluir, faltou, cancelar. Reativar um cancelado verifica conflito de novo. */
    @Transactional
    public AgendamentoDto alterarStatus(Long id, String novoStatus) {
        if (!StatusAgendamento.valido(novoStatus)) throw ApiException.campo("status", "Status inválido");
        Agendamento a = obter(id);
        if (StatusAgendamento.CANCELADO.equals(a.getStatus()) && !StatusAgendamento.CANCELADO.equals(novoStatus)) {
            pacientes.findByIdForUpdate(a.getPaciente().getId());
            dentistas.findByIdForUpdate(a.getDentista().getId());
            verificarConflito(a.getData(), a.getHoraInicio(), a.getHoraFim(),
                    a.getDentista().getId(), a.getPaciente().getId(), a.getId());
        }
        a.setStatus(novoStatus);
        return AgendamentoDto.from(a);
    }

    @Transactional
    public void excluir(Long id) {
        agendamentos.delete(obter(id));
    }

    // ------------------------------------------------------------------ regras

    private Agendamento obter(Long id) {
        Agendamento a = agendamentos.findById(id).orElse(null);
        return atual.permitir(a, "Agendamento não encontrado");
    }

    /** Paciente e dentista precisam ser da médica logada (o administrador pode usar qualquer um). */
    private void exigirAcesso(Paciente paciente, Dentista dentista) {
        if (!atual.podeAcessar(paciente)) throw ApiException.campo("pacienteId", "Paciente não encontrado");
        if (!atual.podeAcessar(dentista)) throw ApiException.campo("dentistaId", "Dentista não encontrado");
    }

    private String statusOuPadrao(String status) {
        if (status == null || status.isBlank()) return StatusAgendamento.AGENDADO;
        if (!StatusAgendamento.valido(status)) throw ApiException.campo("status", "Status inválido");
        return status;
    }

    /** Dia de funcionamento + dentro do expediente. Devolve a hora de fim. */
    private LocalTime validarHorario(LocalDate data, LocalTime inicio, Procedimento proc) {
        Configuracao cfg = configuracao.atual();
        DayOfWeek dow = data.getDayOfWeek();
        if (dow == DayOfWeek.SUNDAY) throw ApiException.campo("data", "A clínica não abre aos domingos");
        if (dow == DayOfWeek.SATURDAY && !cfg.isAtendeSabado()) throw ApiException.campo("data", "A clínica não abre aos sábados");

        LocalTime fim = inicio.plusMinutes(proc.getDuracaoMin());
        boolean virouODia = !fim.isAfter(inicio);
        if (virouODia || inicio.isBefore(cfg.getAbertura()) || fim.isAfter(cfg.getFechamento())) {
            throw ApiException.campo("hora", "Fora do horário de funcionamento (%s–%s)"
                    .formatted(cfg.getAbertura().format(HH_MM), cfg.getFechamento().format(HH_MM)));
        }
        return fim;
    }

    private void verificarConflito(LocalDate data, LocalTime inicio, LocalTime fim, Long dentistaId, Long pacienteId, Long ignorarId) {
        List<Agendamento> conflitos = agendamentos.findConflitos(data, inicio, fim, dentistaId, pacienteId, ignorarId);
        if (conflitos.isEmpty()) return;
        Agendamento c = conflitos.get(0);
        String hora = c.getHoraInicio().format(HH_MM);
        String msg = Objects.equals(c.getDentista().getId(), dentistaId)
                ? "Conflito: dentista ocupado às %s (%s)".formatted(hora, c.getPaciente().getNome())
                : "Conflito: paciente já tem consulta às %s".formatted(hora);
        throw new ApiException(org.springframework.http.HttpStatus.CONFLICT, msg, java.util.Map.of("hora", msg));
    }
}
