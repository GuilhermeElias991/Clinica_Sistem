package br.univille.odontocare.service;

import br.univille.odontocare.config.AppProperties;
import br.univille.odontocare.domain.*;
import br.univille.odontocare.dto.*;
import br.univille.odontocare.exception.ApiException;
import br.univille.odontocare.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Backup (exportar/importar JSON) e "Restaurar dados de exemplo" da tela de Configurações.
 * O formato é o mesmo do backup antigo do front (localStorage), então backups feitos
 * antes do back-end existir também podem ser importados.
 */
@Service
public class BackupService {

    private final PacienteRepository pacientes;
    private final DentistaRepository dentistas;
    private final ProcedimentoRepository procedimentos;
    private final AgendamentoRepository agendamentos;
    private final ConfiguracaoService configuracao;
    private final AppProperties props;

    public BackupService(PacienteRepository pacientes, DentistaRepository dentistas, ProcedimentoRepository procedimentos,
                         AgendamentoRepository agendamentos, ConfiguracaoService configuracao, AppProperties props) {
        this.pacientes = pacientes;
        this.dentistas = dentistas;
        this.procedimentos = procedimentos;
        this.agendamentos = agendamentos;
        this.configuracao = configuracao;
        this.props = props;
    }

    // ------------------------------------------------------------------ exportar

    @Transactional
    public Map<String, Object> exportar() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("versao", 1);
        out.put("exportadoEm", OffsetDateTime.now(props.zone()).toString());
        out.put("config", ConfiguracaoDto.from(configuracao.atual()));
        out.put("pacientes", pacientes.findAllByOrderByNomeAsc().stream().map(PacienteDto::from).toList());
        out.put("dentistas", dentistas.findAllByOrderByIdAsc().stream().map(DentistaDto::from).toList());
        out.put("procedimentos", procedimentos.findAllByOrderByIdAsc().stream().map(ProcedimentoDto::from).toList());
        out.put("agendamentos", agendamentos.findAll().stream().map(AgendamentoDto::from).toList());
        return out;
    }

    // ------------------------------------------------------------------ importar

    /** Substitui TODOS os dados (menos usuários). Se algo estiver errado, nada é alterado. */
    @Transactional
    public void importar(JsonNode json) {
        for (String col : List.of("pacientes", "dentistas", "procedimentos", "agendamentos")) {
            if (json == null || !json.path(col).isArray()) throw ApiException.badRequest("Arquivo inválido: falta a lista \"" + col + "\"");
        }
        limparTudo();
        Set<String> cpfs = new HashSet<>();
        Set<String> cros = new HashSet<>();

        Map<String, Procedimento> procs = new HashMap<>();
        for (JsonNode n : json.get("procedimentos")) {
            Procedimento p = new Procedimento();
            p.setNome(obrigatorio(n, "nome", "procedimento"));
            p.setDuracaoMin(Math.max(10, n.path("duracao").asInt(30)));
            p.setValor(decimal(n.path("valor"), BigDecimal.ZERO).max(BigDecimal.ZERO));
            p.setAtivo(n.path("ativo").asBoolean(true));
            procs.put(n.path("id").asText(), procedimentos.save(p));
        }

        Map<String, Dentista> dents = new HashMap<>();
        for (JsonNode n : json.get("dentistas")) {
            Dentista d = new Dentista();
            d.setNome(obrigatorio(n, "nome", "dentista"));
            d.setCro(obrigatorio(n, "cro", "dentista"));
            if (!cros.add(d.getCro().toLowerCase())) throw ApiException.badRequest("Arquivo inválido: CRO repetido (" + d.getCro() + ")");
            d.setEspecialidade(n.path("especialidade").asText("Clínico Geral"));
            d.setTelefone(Documentos.vazioParaNull(n.path("telefone").asText(null)));
            String cor = n.path("cor").asText("#0ea5e9");
            d.setCor(cor.matches("^#[0-9a-fA-F]{6}$") ? cor.toLowerCase() : "#0ea5e9");
            d.setAtivo(n.path("ativo").asBoolean(true));
            dents.put(n.path("id").asText(), dentistas.save(d));
        }

        Map<String, Paciente> pacs = new HashMap<>();
        for (JsonNode n : json.get("pacientes")) {
            String nome = obrigatorio(n, "nome", "paciente");
            String cpf = obrigatorio(n, "cpf", "paciente");
            if (!Documentos.cpfValido(cpf)) throw ApiException.badRequest("Arquivo inválido: CPF inválido para " + nome);
            if (!cpfs.add(Documentos.digitos(cpf))) throw ApiException.badRequest("Arquivo inválido: CPF repetido (" + nome + ")");
            Paciente p = new Paciente();
            p.setNome(nome);
            p.setCpf(Documentos.formatarCpf(cpf));
            p.setTelefone(Documentos.formatarTelefone(obrigatorio(n, "telefone", "paciente")));
            p.setEmail(Documentos.vazioParaNull(n.path("email").asText(null)));
            String nasc = Documentos.vazioParaNull(n.path("nascimento").asText(null));
            p.setDataNascimento(nasc == null ? null : LocalDate.parse(nasc));
            String conv = Documentos.vazioParaNull(n.path("convenio").asText(null));
            p.setConvenio(conv == null ? "Particular" : conv);
            p.setObservacoes(Documentos.vazioParaNull(n.path("obs").asText(null)));
            pacs.put(n.path("id").asText(), pacientes.save(p));
        }

        for (JsonNode n : json.get("agendamentos")) {
            Paciente p = pacs.get(n.path("pacienteId").asText());
            Dentista d = dents.get(n.path("dentistaId").asText());
            Procedimento pr = procs.get(n.path("procedimentoId").asText());
            if (p == null || d == null || pr == null) {
                throw ApiException.badRequest("Arquivo inválido: agendamento aponta para paciente/dentista/procedimento inexistente");
            }
            Agendamento a = new Agendamento();
            a.setPaciente(p);
            a.setDentista(d);
            a.setProcedimento(pr);
            a.setData(LocalDate.parse(obrigatorio(n, "data", "agendamento")));
            LocalTime ini = LocalTime.parse(obrigatorio(n, "hora", "agendamento"));
            a.setHoraInicio(ini);
            String fimTxt = Documentos.vazioParaNull(n.path("horaFim").asText(null));
            a.setHoraFim(fimTxt == null ? ini.plusMinutes(pr.getDuracaoMin()) : LocalTime.parse(fimTxt));
            if (!a.getHoraFim().isAfter(ini)) throw ApiException.badRequest("Arquivo inválido: horário de agendamento inválido");
            String st = n.path("status").asText(StatusAgendamento.AGENDADO);
            a.setStatus(StatusAgendamento.valido(st) ? st : StatusAgendamento.AGENDADO);
            a.setValor(decimal(n.path("valor"), pr.getValor()));
            a.setObservacoes(Documentos.vazioParaNull(n.path("obs").asText(null)));
            a.setCriadoEm(dataHora(n.path("criadoEm").asText(null)));
            agendamentos.save(a);
        }

        JsonNode c = json.path("config");
        if (c.isObject()) {
            Configuracao atual = configuracao.atual();
            configuracao.atualizar(new ConfiguracaoDto(
                    c.path("clinica").asText(atual.getNomeClinica()),
                    LocalTime.parse(c.path("abertura").asText(atual.getAbertura().toString())),
                    LocalTime.parse(c.path("fechamento").asText(atual.getFechamento().toString())),
                    c.path("intervalo").asInt(atual.getIntervaloMin()),
                    c.path("sabado").asBoolean(atual.isAtendeSabado())));
        }
    }

    // ------------------------------------------------------------------ dados de exemplo

    /** Apaga tudo (menos usuários) e carrega os mesmos dados de exemplo do front. */
    @Transactional
    public void restaurarExemplo() {
        limparTudo();
        Configuracao cfg = configuracao.atual();
        cfg.setNomeClinica("OdontoCare");
        cfg.setAbertura(LocalTime.of(8, 0));
        cfg.setFechamento(LocalTime.of(18, 0));
        cfg.setIntervaloMin(30);
        cfg.setAtendeSabado(false);

        Dentista d1 = dentista("Dra. Ana Souza", "CRO-SC 12345", "Ortodontia", "(47) 99911-2233", "#0ea5e9");
        Dentista d2 = dentista("Dr. Bruno Lima", "CRO-SC 23456", "Implantodontia", "(47) 99922-3344", "#8b5cf6");
        Dentista d3 = dentista("Dra. Carla Mendes", "CRO-SC 34567", "Clínico Geral", "(47) 99933-4455", "#10b981");

        Procedimento p1 = procedimento("Avaliação", 30, 120);
        Procedimento p2 = procedimento("Limpeza (Profilaxia)", 45, 180);
        Procedimento p3 = procedimento("Restauração", 60, 250);
        Procedimento p4 = procedimento("Canal (Endodontia)", 90, 900);
        Procedimento p5 = procedimento("Clareamento", 60, 700);
        Procedimento p6 = procedimento("Manutenção Aparelho", 30, 150);
        Procedimento p7 = procedimento("Extração", 60, 350);

        Paciente c1 = paciente("Marcos Pereira", "529.982.247-25", "(47) 98811-1111", "marcos@email.com", "1990-04-12", "Particular", null);
        Paciente c2 = paciente("Juliana Costa", "111.444.777-35", "(47) 98822-2222", "juliana@email.com", "1985-09-30", "OdontoPrev", "Alergia a penicilina");
        Paciente c3 = paciente("Rafael Oliveira", "390.533.447-05", "(47) 98833-3333", "rafael@email.com", "2001-01-20", "Amil Dental", null);
        Paciente c4 = paciente("Beatriz Santos", "853.513.468-93", "(47) 98844-4444", "bia@email.com", "1998-07-05", "Particular", "Usa aparelho fixo");
        Paciente c5 = paciente("Pedro Almeida", "275.484.389-23", "(47) 98855-5555", "pedro@email.com", "1975-12-15", "Bradesco Dental", null);
        Paciente c6 = paciente("Larissa Ferreira", "714.602.380-01", "(47) 98866-6666", "larissa@email.com", "1993-03-22", "Particular", null);

        LocalDate segunda = props.hoje().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        agendamento(segunda, 0, "08:00", c1, d1, p6, StatusAgendamento.CONCLUIDO);
        agendamento(segunda, 0, "09:00", c2, d3, p2, StatusAgendamento.CONCLUIDO);
        agendamento(segunda, 0, "14:00", c5, d2, p1, StatusAgendamento.FALTOU);
        agendamento(segunda, 1, "10:00", c3, d3, p3, StatusAgendamento.CONCLUIDO);
        agendamento(segunda, 1, "15:30", c4, d1, p6, StatusAgendamento.CONFIRMADO);
        agendamento(segunda, 2, "08:30", c6, d2, p4, StatusAgendamento.CONFIRMADO);
        agendamento(segunda, 2, "11:00", c1, d3, p2, StatusAgendamento.AGENDADO);
        agendamento(segunda, 3, "09:00", c2, d1, p1, StatusAgendamento.AGENDADO);
        agendamento(segunda, 3, "13:30", c5, d2, p7, StatusAgendamento.AGENDADO);
        agendamento(segunda, 4, "10:00", c3, d3, p5, StatusAgendamento.AGENDADO);
        agendamento(segunda, 4, "16:00", c6, d1, p6, StatusAgendamento.CANCELADO);
        agendamento(segunda, 7, "09:00", c4, d3, p2, StatusAgendamento.AGENDADO);
        agendamento(segunda, 8, "14:00", c1, d2, p3, StatusAgendamento.AGENDADO);
    }

    /** Banco "virgem": só os procedimentos padrão da migração V1, sem dentistas/pacientes/agendamentos. */
    @Transactional(readOnly = true)
    public boolean bancoVazio() {
        return pacientes.count() == 0 && dentistas.count() == 0 && agendamentos.count() == 0;
    }

    // ------------------------------------------------------------------ auxiliares

    private void limparTudo() {
        agendamentos.deleteAllInBatch();
        pacientes.deleteAllInBatch();
        dentistas.deleteAllInBatch();
        procedimentos.deleteAllInBatch();
    }

    private Dentista dentista(String nome, String cro, String esp, String tel, String cor) {
        Dentista d = new Dentista();
        d.setNome(nome);
        d.setCro(cro);
        d.setEspecialidade(esp);
        d.setTelefone(tel);
        d.setCor(cor);
        return dentistas.save(d);
    }

    private Procedimento procedimento(String nome, int duracao, int valor) {
        Procedimento p = new Procedimento();
        p.setNome(nome);
        p.setDuracaoMin(duracao);
        p.setValor(BigDecimal.valueOf(valor));
        return procedimentos.save(p);
    }

    private Paciente paciente(String nome, String cpf, String tel, String email, String nasc, String conv, String obs) {
        Paciente p = new Paciente();
        p.setNome(nome);
        p.setCpf(cpf);
        p.setTelefone(tel);
        p.setEmail(email);
        p.setDataNascimento(LocalDate.parse(nasc));
        p.setConvenio(conv);
        p.setObservacoes(obs);
        return pacientes.save(p);
    }

    private void agendamento(LocalDate segunda, int dias, String hora, Paciente c, Dentista d, Procedimento p, String status) {
        Agendamento a = new Agendamento();
        a.setPaciente(c);
        a.setDentista(d);
        a.setProcedimento(p);
        a.setData(segunda.plusDays(dias));
        a.setHoraInicio(LocalTime.parse(hora));
        a.setHoraFim(a.getHoraInicio().plusMinutes(p.getDuracaoMin()));
        a.setStatus(status);
        a.setValor(p.getValor());
        agendamentos.save(a);
    }

    private static String obrigatorio(JsonNode n, String campo, String oQue) {
        String v = Documentos.vazioParaNull(n.path(campo).asText(null));
        if (v == null) throw ApiException.badRequest("Arquivo inválido: " + oQue + " sem \"" + campo + "\"");
        return v;
    }

    private static BigDecimal decimal(JsonNode n, BigDecimal padrao) {
        if (n == null || n.isMissingNode() || n.isNull()) return padrao;
        try {
            return new BigDecimal(n.asText());
        } catch (NumberFormatException e) {
            return padrao;
        }
    }

    private LocalDateTime dataHora(String txt) {
        if (txt == null || txt.isBlank()) return null;
        try {
            return OffsetDateTime.parse(txt).atZoneSameInstant(props.zone()).toLocalDateTime();
        } catch (DateTimeException e) {
            try {
                return LocalDateTime.parse(txt);
            } catch (DateTimeException e2) {
                return null;
            }
        }
    }
}
