package com.example.newcotizador;

import com.example.newcotizador.infrastructure.adapter.out.persistence.entity.*;
import com.example.newcotizador.infrastructure.adapter.out.persistence.repository.*;
import com.example.newcotizador.domain.port.out.EmpleadoRepositoryPort;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class RolesWorkflowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired SolicitudCreditoRepository solicitudes;
    @Autowired ClienteRepository clientes;
    @Autowired ProductoHipotecarioRepository productos;
    @Autowired EmpleadoRepository empleados;
    @Autowired EmpleadoRepositoryPort empleadoPort;
    @Autowired PasswordEncoder encoder;
    int clientId, productId;

    @BeforeEach void seed() {
        solicitudes.deleteAll(); clientes.deleteAll(); productos.deleteAll(); empleados.deleteAll();
        account("ejecutivo","EJECUTIVO_COMERCIAL");
        account("otro","EJECUTIVO_COMERCIAL");
        account("aprobador","GESTOR_RIESGOS");
        account("gestor2","GESTOR_RIESGOS");
        clientId = clientes.saveAndFlush(ClienteJpaEntity.builder().tipoDocumento("DNI").numeroDocumento("70000001")
            .nombres("Cliente").apellidos("Prueba").ingresoMensualNeto(new BigDecimal("9000")).build()).getClienteId();
        productId = productos.saveAndFlush(ProductoHipotecarioJpaEntity.builder().nombreProducto("Hipotecario")
            .tasaInteresReferencial(new BigDecimal("9")).build()).getProductoId();
    }
    private void account(String user,String role) {
        empleados.saveAndFlush(EmpleadoJpaEntity.builder().codigoMatricula(user).nombres(user).apellidos("Prueba")
            .rolPrincipal(role).passwordHash(encoder.encode("Only-for-test-123!")).build());
    }
    private String body() {
        return """
            {"productoId":%d,"montoSolicitado":240000,"plazoMeses":240,
             "participantes":[{"clienteId":%d,"tipoParticipacion":"TITULAR"}],
             "inmuebles":[{"tipoInmueble":"CASA","direccion":"Direccion de prueba","valorComercial":300000,"valorTasacion":290000}]}
            """.formatted(productId,clientId);
    }
    private JsonNode create() throws Exception {
        String response = mvc.perform(post("/api/solicitudes/registrar").with(user("ejecutivo").roles("EJECUTIVO_COMERCIAL"))
            .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body()))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }
    private MockHttpSession login(String name) throws Exception {
        return (MockHttpSession)mvc.perform(post("/api/login").with(csrf())
            .param("username",name).param("password","Only-for-test-123!"))
            .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }
    @Test void realLoginPersistsBothRolesAndDoesNotExposeHash() throws Exception {
        for (String name : new String[]{"ejecutivo","aprobador"}) {
            MockHttpSession session=login(name);
            String body=mvc.perform(get("/api/session").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoMatricula").value(name)).andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
            assertThat(body).doesNotContain("$2a$");
            mvc.perform(post("/api/logout").session(session).with(csrf())).andExpect(status().isNoContent());
            assertThat(session.isInvalid()).isTrue();
        }
        assertThat(encoder.matches("Only-for-test-123!", empleadoPort.findByCodigoMatricula("ejecutivo").orElseThrow().getPasswordHash())).isTrue();
    }
    @Test void anonymousWrongCredentialsAndCsrfAreRejected() throws Exception {
        mvc.perform(get("/api/solicitudes")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/login").param("username","ejecutivo").param("password","Only-for-test-123!")).andExpect(status().isForbidden());
        mvc.perform(post("/api/login").with(csrf()).param("username","ejecutivo").param("password","wrong")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/solicitudes/registrar").with(user("ejecutivo").roles("EJECUTIVO_COMERCIAL"))
            .contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isForbidden());
    }
    @Test void roundTripRetainsGuaranteesAuditAndFinancialSnapshots() throws Exception {
        JsonNode created=create(); int id=created.get("solicitudId").asInt();
        assertThat(created.get("inmuebles").get(0).get("tipoInmueble").asText()).isEqualTo("CASA");
        assertThat(created.get("cuotaMensual").decimalValue()).isEqualByComparingTo("2105.43");
        assertThat(created.has("passwordHash")).isFalse();
        var product=productos.findById((short)productId).orElseThrow();
        product.setTasaInteresReferencial(new BigDecimal("20"));productos.saveAndFlush(product);
        var client=clientes.findById(clientId).orElseThrow();client.setIngresoMensualNeto(new BigDecimal("100"));clientes.saveAndFlush(client);
        String detail=mvc.perform(get("/api/solicitudes/"+id).with(user("ejecutivo").roles("EJECUTIVO_COMERCIAL")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.producto.tasaInteresReferencial").value(9))
            .andExpect(jsonPath("$.participantes[0].cliente.ingresoMensualNeto").value(9000))
            .andExpect(jsonPath("$.ejecutivo.passwordHash").doesNotExist()).andReturn().getResponse().getContentAsString();
        assertThat(detail).doesNotContain("$2a$");
        mvc.perform(post("/api/solicitudes/"+id+"/evaluar").with(user("aprobador").roles("GESTOR_RIESGOS"))
            .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.historial.length()").value(2));
        mvc.perform(post("/api/solicitudes/"+id+"/aprobar").with(user("aprobador").roles("GESTOR_RIESGOS"))
            .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"version\":1,\"comentario\":\"Ingreso & respaldo verificados\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.estadoActual").value("APROBADO"))
            .andExpect(jsonPath("$.historial.length()").value(3)).andExpect(jsonPath("$.version").value(2));
        mvc.perform(get("/api/solicitudes").with(user("ejecutivo").roles("EJECUTIVO_COMERCIAL")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }
    @Test void roleAndOwnershipCannotBeOverriddenByRequestParameters() throws Exception {
        int id=create().get("solicitudId").asInt();
        mvc.perform(get("/api/solicitudes/"+id).with(user("otro").roles("EJECUTIVO_COMERCIAL"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/solicitudes").with(user("otro").roles("EJECUTIVO_COMERCIAL")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(post("/api/solicitudes/"+id+"/evaluar?gestorId=3").with(user("ejecutivo").roles("EJECUTIVO_COMERCIAL"))
            .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/solicitudes/registrar?empleadoId=1").with(user("aprobador").roles("GESTOR_RIESGOS"))
            .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isForbidden());
        mvc.perform(get("/api/clientes").with(user("aprobador").roles("GESTOR_RIESGOS"))).andExpect(status().isForbidden());
    }
    @Test void staleVersionsAndOtherManagersCannotDecide() throws Exception {
        int id=create().get("solicitudId").asInt();
        mvc.perform(post("/api/solicitudes/"+id+"/evaluar").with(user("aprobador").roles("GESTOR_RIESGOS"))
            .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}")).andExpect(status().isOk());
        mvc.perform(post("/api/solicitudes/"+id+"/evaluar").with(user("gestor2").roles("GESTOR_RIESGOS"))
            .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}")).andExpect(status().isConflict());
        mvc.perform(post("/api/solicitudes/"+id+"/aprobar").with(user("gestor2").roles("GESTOR_RIESGOS"))
            .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"version\":1,\"comentario\":\"Intento ajeno\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/solicitudes/"+id+"/rechazar").with(user("aprobador").roles("GESTOR_RIESGOS"))
            .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"version\":1,\"comentario\":\"Documentacion insuficiente\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.estadoActual").value("RECHAZADO"));
    }
    @Test void validatesAmountsReferencesAndMassAssignment() throws Exception {
        for (String invalid : new String[]{body().replace("240000","-1"),body().replace("\"plazoMeses\":240","\"plazoMeses\":0"),
            body().replace("\"productoId\":"+productId,"\"productoId\":32767"),body().replace("\"clienteId\":"+clientId,"\"clienteId\":2147483647"),
            body().replace("\"montoSolicitado\":240000","\"estadoActual\":\"APROBADO\",\"montoSolicitado\":240000")}) {
            mvc.perform(post("/api/solicitudes/registrar").with(user("ejecutivo").roles("EJECUTIVO_COMERCIAL"))
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
        }
        assertThat(solicitudes.count()).isZero();
    }
    @Test void concurrentAssignmentHasExactlyOneWinner() throws Exception {
        int id=create().get("solicitudId").asInt();
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch start=new CountDownLatch(1);
        try {
            var futures=java.util.stream.Stream.of("aprobador","gestor2").map(name -> pool.submit(() -> {
                start.await();
                return mvc.perform(post("/api/solicitudes/"+id+"/evaluar").with(user(name).roles("GESTOR_RIESGOS"))
                    .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                    .andReturn().getResponse().getStatus();
            })).toList();
            start.countDown();
            assertThat(java.util.List.of(futures.get(0).get(15,TimeUnit.SECONDS),futures.get(1).get(15,TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(200,409);
        } finally { pool.shutdownNow(); }
    }
}
