package br.univille.odontocare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:odonto-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "app.seed-demo=true"
})
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    String token;

    @BeforeEach
    void login() throws Exception {
        mvc.perform(post("/api/backup/restaurar-exemplo").header("Authorization", "Bearer " + obterToken()))
                .andExpect(status().isNoContent());
    }

    private String obterToken() throws Exception {
        if (token != null) return token;
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@odonto.com\",\"senha\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.cargo").value("Administrador"))
                .andReturn();
        token = json.readTree(r.getResponse().getContentAsString()).get("token").asText();
        return token;
    }

    private String auth() throws Exception {
        return "Bearer " + obterToken();
    }

    /** Uma segunda-feira daqui a 2 semanas (sempre no futuro e dia útil). */
    private static LocalDate segundaFutura() {
        return LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusWeeks(2).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private JsonNode getJson(String url) throws Exception {
        return json.readTree(mvc.perform(get(url).header("Authorization", auth()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @Test
    void loginErradoDevolve401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@odonto.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos"));
    }

    @Test
    void semTokenDevolve401() throws Exception {
        mvc.perform(get("/api/pacientes")).andExpect(status().isUnauthorized());
    }

    @Test
    void dadosDeExemploCarregados() throws Exception {
        assertThat(getJson("/api/pacientes")).hasSize(6);
        assertThat(getJson("/api/dentistas")).hasSize(3);
        assertThat(getJson("/api/procedimentos")).hasSize(7);
        assertThat(getJson("/api/agendamentos")).hasSize(13);
        assertThat(getJson("/api/configuracoes").get("abertura").asText()).isEqualTo("08:00");
    }

    @Test
    void cpfInvalidoEDuplicado() throws Exception {
        String base = "{\"nome\":\"Fulano de Tal\",\"cpf\":\"%s\",\"telefone\":\"(47) 99999-0000\",\"email\":\"\",\"nascimento\":\"\"}";
        mvc.perform(post("/api/pacientes").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content(base.formatted("123.456.789-00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.cpf").value("CPF inválido"));
        mvc.perform(post("/api/pacientes").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content(base.formatted("52998224725")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.cpf").value("CPF já cadastrado"));
        mvc.perform(post("/api/pacientes").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content(base.formatted("93541134780")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpf").value("935.411.347-80"));
    }

    @Test
    void agendamentoComConflitoDevolve409() throws Exception {
        long paciente = getJson("/api/pacientes").get(0).get("id").asLong();
        long outroPaciente = getJson("/api/pacientes").get(1).get("id").asLong();
        long dentista = getJson("/api/dentistas").get(0).get("id").asLong();
        long proc = getJson("/api/procedimentos").get(2).get("id").asLong(); // Restauração, 60 min
        String data = segundaFutura().toString();
        String body = "{\"pacienteId\":%d,\"dentistaId\":%d,\"procedimentoId\":%d,\"data\":\"%s\",\"hora\":\"%s\",\"obs\":\"\"}";

        MvcResult criado = mvc.perform(post("/api/agendamentos").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(paciente, dentista, proc, data, "10:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.horaFim").value("11:00"))
                .andExpect(jsonPath("$.status").value("agendado"))
                .andReturn();
        long id = json.readTree(criado.getResponse().getContentAsString()).get("id").asLong();

        // mesmo dentista, horário sobreposto
        mvc.perform(post("/api/agendamentos").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(outroPaciente, dentista, proc, data, "10:30")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Conflito: dentista ocupado às 10:00")));

        // encostado no fim não conflita
        mvc.perform(post("/api/agendamentos").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(outroPaciente, dentista, proc, data, "11:00")))
                .andExpect(status().isCreated());

        // cancelar libera o horário
        mvc.perform(patch("/api/agendamentos/" + id + "/status").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"cancelado\"}"))
                .andExpect(status().isOk());
        JsonNode slots = getJson("/api/agendamentos/horarios-livres?data=" + data + "&dentistaId=" + dentista + "&procedimentoId=" + proc);
        assertThat(slots.get(4).get("hora").asText()).isEqualTo("10:00");
        assertThat(slots.get(4).get("livre").asBoolean()).isTrue();
    }

    @Test
    void foraDoExpedienteEDomingo() throws Exception {
        long paciente = getJson("/api/pacientes").get(0).get("id").asLong();
        long dentista = getJson("/api/dentistas").get(0).get("id").asLong();
        long proc = getJson("/api/procedimentos").get(3).get("id").asLong(); // Canal, 90 min
        String body = "{\"pacienteId\":%d,\"dentistaId\":%d,\"procedimentoId\":%d,\"data\":\"%s\",\"hora\":\"%s\"}";

        mvc.perform(post("/api/agendamentos").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(paciente, dentista, proc, segundaFutura(), "17:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.hora").value("Fora do horário de funcionamento (08:00–18:00)"));
        mvc.perform(post("/api/agendamentos").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(paciente, dentista, proc, segundaFutura().minusDays(1), "09:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.data").value("A clínica não abre aos domingos"));
    }

    @Test
    void naoExcluiProcedimentoEmUsoEExcluiPacienteEmCascata() throws Exception {
        long procEmUso = getJson("/api/agendamentos").get(0).get("procedimentoId").asLong();
        mvc.perform(delete("/api/procedimentos/" + procEmUso).header("Authorization", auth()))
                .andExpect(status().isConflict());

        long paciente = getJson("/api/agendamentos").get(0).get("pacienteId").asLong();
        mvc.perform(delete("/api/pacientes/" + paciente).header("Authorization", auth()))
                .andExpect(status().isNoContent());
        for (JsonNode a : getJson("/api/agendamentos")) {
            assertThat(a.get("pacienteId").asLong()).isNotEqualTo(paciente);
        }
    }

    @Test
    void backupExportaEImporta() throws Exception {
        JsonNode backup = getJson("/api/backup");
        mvc.perform(post("/api/pacientes").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Paciente Extra\",\"cpf\":\"93541134780\",\"telefone\":\"47999990000\"}"))
                .andExpect(status().isCreated());
        assertThat(getJson("/api/pacientes")).hasSize(7);

        mvc.perform(post("/api/backup").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content(backup.toString()))
                .andExpect(status().isNoContent());
        assertThat(getJson("/api/pacientes")).hasSize(6);
        assertThat(getJson("/api/agendamentos")).hasSize(13);
    }

    @Test
    void dashboardEConfiguracoes() throws Exception {
        JsonNode d = getJson("/api/dashboard");
        assertThat(d.get("pacientes").asLong()).isEqualTo(6);
        assertThat(d.get("semana")).hasSize(6);

        mvc.perform(put("/api/configuracoes").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clinica\":\"Clínica X\",\"abertura\":\"18:00\",\"fechamento\":\"08:00\",\"intervalo\":30,\"sabado\":true}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/configuracoes").header("Authorization", auth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clinica\":\"Clínica X\",\"abertura\":\"07:00\",\"fechamento\":\"19:00\",\"intervalo\":15,\"sabado\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clinica").value("Clínica X"))
                .andExpect(jsonPath("$.intervalo").value(15));
    }
}
