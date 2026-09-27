package com.example.newcotizador;

import com.example.newcotizador.entity.Usuario;
import com.example.newcotizador.repository.*;
import java.math.BigDecimal;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkflowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CotizacionRepository quotes;
    @Autowired ClienteRepository clients;
    @Autowired UsuarioRepository users;

    private static final String INPUT = """
        {"dni":"12345678","nombres":"Ana","apellidos":"Torres",
         "valorInmueble":300000,"cuotaInicial":60000,"plazoMeses":240,
         "ingresosMensuales":9000,"deudasMensuales":500,"scoreCrediticio":750}
        """;

    @BeforeEach void reset() {
        quotes.deleteAll(); clients.deleteAll(); users.deleteAll();
        account("ejecutivo", "EJECUTIVO"); account("otro", "EJECUTIVO"); account("aprobador", "APROBADOR");
    }

    private void account(String name, String role) {
        Usuario u = new Usuario(); u.setUsername(name); u.setRol(role);
        // Authentication is injected by Spring Security test support; hashes are never used.
        u.setPasswordHash("unused-test-hash"); users.save(u);
    }

    private JsonNode create(String input) throws Exception {
        return json.readTree(mvc.perform(post("/api/cotizaciones").with(user("ejecutivo").roles("EJECUTIVO"))
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(input))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private JsonNode requestRate(JsonNode quote) throws Exception {
        return json.readTree(mvc.perform(post("/api/cotizaciones/{id}/solicitud", quote.get("id").asInt())
                .with(user("ejecutivo").roles("EJECUTIVO")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"teaPreferencial\":7.5,\"version\":" + quote.get("version").asLong() + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PENDIENTE_APROBACION"))
                .andReturn().getResponse().getContentAsString());
    }

    @Test void securityEnforcesAuthenticationRolesAndCsrf() throws Exception {
        mvc.perform(get("/api/cotizaciones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/aprobaciones").with(user("ejecutivo").roles("EJECUTIVO"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/cotizaciones").with(user("ejecutivo").roles("EJECUTIVO"))
                .contentType(MediaType.APPLICATION_JSON).content(INPUT)).andExpect(status().isForbidden());
        mvc.perform(post("/api/cotizaciones").with(user("aprobador").roles("APROBADOR")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(INPUT)).andExpect(status().isForbidden());
    }

    @Test void approvedRateReducesPaymentPreservesOriginalRateAndRejectsStaleDecision() throws Exception {
        JsonNode original = create(INPUT);
        JsonNode pending = requestRate(original);
        String decision = "{\"aprobar\":true,\"comentario\":\"Perfil validado\",\"version\":" + pending.get("version").asLong() + "}";
        JsonNode approved = json.readTree(mvc.perform(post("/api/aprobaciones/{id}/decision", original.get("id").asInt())
                .with(user("aprobador").roles("APROBADOR")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(decision))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("APROBADA"))
                .andExpect(jsonPath("$.teaCalculada").value(9.0)).andExpect(jsonPath("$.aprobador").value("aprobador"))
                .andReturn().getResponse().getContentAsString());
        assertThat(new BigDecimal(approved.get("cuotaMensualEstimada").asText()))
                .isLessThan(new BigDecimal(original.get("cuotaMensualEstimada").asText()));
        mvc.perform(post("/api/aprobaciones/{id}/decision", original.get("id").asInt())
                .with(user("aprobador").roles("APROBADOR")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(decision))
                .andExpect(status().isConflict());
    }

    @Test void ownershipIsPrivateAndListDoesNotExposeOtherExecutives() throws Exception {
        JsonNode quote = create(INPUT);
        mvc.perform(post("/api/cotizaciones/{id}/solicitud", quote.get("id").asInt())
                .with(user("otro").roles("EJECUTIVO")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"teaPreferencial\":7.5,\"version\":0}"))
                .andExpect(status().isNotFound());
        String response = mvc.perform(get("/api/cotizaciones").with(user("otro").roles("EJECUTIVO")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("12345678", "Ana", "Torres");
    }

    @Test void poorScoreMaySaveButCannotRequestPreferentialRate() throws Exception {
        JsonNode quote = create(INPUT.replace("750", "650"));
        mvc.perform(post("/api/cotizaciones/{id}/solicitud", quote.get("id").asInt())
                .with(user("ejecutivo").roles("EJECUTIVO")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"teaPreferencial\":7.5,\"version\":0}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test void validationAndPaginationReturnSafeErrors() throws Exception {
        String response = mvc.perform(post("/api/cotizaciones").with(user("ejecutivo").roles("EJECUTIVO"))
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(INPUT.replace("12345678", "bad")))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("SELECT", "INSERT", "org.hibernate", "password_hash", "stackTrace");
        mvc.perform(get("/api/cotizaciones?size=51").with(user("ejecutivo").roles("EJECUTIVO")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/cotizaciones?page=-1").with(user("ejecutivo").roles("EJECUTIVO")))
                .andExpect(status().isBadRequest());
    }

    @Test void rejectionIsFinalAndLeavesOriginalInstallment() throws Exception {
        JsonNode quote = create(INPUT); JsonNode pending = requestRate(quote);
        mvc.perform(post("/api/aprobaciones/{id}/decision", quote.get("id").asInt())
                .with(user("aprobador").roles("APROBADOR")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"aprobar\":false,\"comentario\":\"Capacidad no confirmada\",\"version\":" + pending.get("version").asLong() + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.cuotaMensualEstimada").value(quote.get("cuotaMensualEstimada").asDouble()));
    }

    @Test void roundedDstiCannotMakeIneligibleRequestEligible() throws Exception {
        JsonNode quote = create(INPUT.replace("9000", "10000").replace("\"deudasMensuales\":500", "\"deudasMensuales\":1894.97"));
        assertThat(new BigDecimal(quote.get("dstiPorcentaje").asText())).isEqualByComparingTo("40.00");
        assertThat(quote.get("elegiblePreferencial").asBoolean()).isFalse();
        mvc.perform(post("/api/cotizaciones/{id}/solicitud", quote.get("id").asInt())
                .with(user("ejecutivo").roles("EJECUTIVO")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"teaPreferencial\":7.5,\"version\":0}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test void dstiOverflowIsRejectedBeforePersistence() throws Exception {
        String response = mvc.perform(post("/api/cotizaciones").with(user("ejecutivo").roles("EJECUTIVO"))
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(INPUT.replace("9000", "0.01")))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("INSERT", "org.hibernate", "JdbcSQL", "stackTrace");
        assertThat(quotes.count()).isZero();
        assertThat(clients.count()).isZero();
    }

    @Test void maximumMonetaryAmountWithCentsPassesApiAndDatabaseValidation() throws Exception {
        JsonNode quote = create(INPUT.replace("300000", "999999999.99")
                .replace("60000", "100000000.00").replace("9000", "999999999.99"));
        assertThat(new BigDecimal(quote.get("valorInmueble").asText())).isEqualByComparingTo("999999999.99");
        assertThat(new BigDecimal(quote.get("montoPrestamo").asText())).isEqualByComparingTo("899999999.99");
        mvc.perform(post("/api/simulaciones").with(user("ejecutivo").roles("EJECUTIVO")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"valorInmueble":"999999999.99","cuotaInicial":"100000000.00","plazoMeses":240,
                     "ingresosMensuales":"999999999.99","deudasMensuales":500,"scoreCrediticio":850}
                    """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.montoPrestamo").value(899999999.99));
    }

    @Test void simultaneousDecisionsHaveExactlyOneWinner() throws Exception {
        JsonNode pending = requestRate(create(INPUT));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Integer> decide = () -> {
                if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("Start timeout");
                return mvc.perform(post("/api/aprobaciones/{id}/decision", pending.get("id").asInt())
                        .with(user("aprobador").roles("APROBADOR")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aprobar\":true,\"comentario\":\"Revisión concurrente\",\"version\":" + pending.get("version").asLong() + "}"))
                        .andReturn().getResponse().getStatus();
            };
            Future<Integer> first = pool.submit(decide); Future<Integer> second = pool.submit(decide);
            start.countDown();
            assertThat(java.util.List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        } finally { start.countDown(); pool.shutdownNow(); }
    }
}
