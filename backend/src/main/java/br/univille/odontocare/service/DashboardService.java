package br.univille.odontocare.service;

import br.univille.odontocare.config.AppProperties;
import br.univille.odontocare.config.UsuarioAtual;
import br.univille.odontocare.domain.Agendamento;
import br.univille.odontocare.domain.Dentista;
import br.univille.odontocare.domain.StatusAgendamento;
import br.univille.odontocare.repository.AgendamentoRepository;
import br.univille.odontocare.repository.DentistaRepository;
import br.univille.odontocare.repository.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.IntStream;

/** Indicadores do Dashboard (mesmas contas que o front faz). */
@Service
public class DashboardService {

    private final AgendamentoRepository agendamentos;
    private final PacienteRepository pacientes;
    private final DentistaRepository dentistas;
    private final AppProperties props;
    private final UsuarioAtual atual;

    public DashboardService(AgendamentoRepository agendamentos, PacienteRepository pacientes,
                            DentistaRepository dentistas, AppProperties props, UsuarioAtual atual) {
        this.atual = atual;
        this.agendamentos = agendamentos;
        this.pacientes = pacientes;
        this.dentistas = dentistas;
        this.props = props;
    }

    public record Dia(LocalDate data, long total) {}

    public record PorDentista(Long dentistaId, String nome, String cor, long total) {}

    public record Resumo(String mes, long consultasHoje, long pacientes, BigDecimal faturamentoMes,
                         double comparecimento, Map<String, Long> porStatus, List<Dia> semana,
                         List<PorDentista> porDentista) {}

    @Transactional(readOnly = true)
    public Resumo resumo(YearMonth mes) {
        LocalDate hoje = props.hoje();
        YearMonth ym = mes == null ? YearMonth.from(hoje) : mes;

        Long dono = atual.filtroDono();
        List<Agendamento> doMes = entre(dono, ym.atDay(1), ym.atEndOfMonth());
        LocalDate segunda = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<Agendamento> daSemana = entre(dono, segunda, segunda.plusDays(5));

        long hojeCount = entre(dono, hoje, hoje).stream().filter(this::ativo).count();

        BigDecimal faturamento = doMes.stream()
                .filter(a -> StatusAgendamento.CONCLUIDO.equals(a.getStatus()))
                .map(a -> a.getValor() != null ? a.getValor() : a.getProcedimento().getValor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long concluidos = doMes.stream().filter(a -> StatusAgendamento.CONCLUIDO.equals(a.getStatus())).count();
        long faltas = doMes.stream().filter(a -> StatusAgendamento.FALTOU.equals(a.getStatus())).count();
        double comparecimento = concluidos + faltas == 0 ? 100.0 : concluidos * 100.0 / (concluidos + faltas);

        Map<String, Long> porStatus = new LinkedHashMap<>();
        StatusAgendamento.TODOS.stream().sorted().forEach(s -> porStatus.put(s, 0L));
        doMes.forEach(a -> porStatus.merge(a.getStatus(), 1L, Long::sum));

        List<Dia> semana = IntStream.range(0, 6).mapToObj(i -> {
            LocalDate d = segunda.plusDays(i);
            return new Dia(d, daSemana.stream().filter(a -> a.getData().equals(d) && ativo(a)).count());
        }).toList();

        List<PorDentista> porDentista = new ArrayList<>();
        List<Dentista> meusDentistas = dono == null ? dentistas.findAllByOrderByIdAsc() : dentistas.findAllByDonoIdOrderByIdAsc(dono);
        for (Dentista d : meusDentistas) {
            long n = doMes.stream().filter(a -> a.getDentista().getId().equals(d.getId()) && ativo(a)).count();
            porDentista.add(new PorDentista(d.getId(), d.getNome(), d.getCor(), n));
        }

        return new Resumo(ym.toString(), hojeCount, dono == null ? pacientes.count() : pacientes.countByDonoId(dono), faturamento,
                Math.round(comparecimento * 10) / 10.0, porStatus, semana, porDentista);
    }

    private List<Agendamento> entre(Long dono, LocalDate ini, LocalDate fim) {
        return dono == null ? agendamentos.findByDataBetween(ini, fim) : agendamentos.findByDonoIdAndDataBetween(dono, ini, fim);
    }

    private boolean ativo(Agendamento a) {
        return !StatusAgendamento.CANCELADO.equals(a.getStatus());
    }
}
