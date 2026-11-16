package com.tecsup.reservas.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecsup.reservas.BaseServicioTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Quién puede entrar a qué: estudiante, administrador, maqueta (token) y errores en formato JSON. */
@AutoConfigureMockMvc
class SeguridadApiTest extends BaseServicioTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private MvcResult login(String identificador, String clave) throws Exception {
        String cuerpo = json.writeValueAsString(Map.of("identificador", identificador, "clave", clave));
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn();
    }

    private String token(String identificador, String clave) throws Exception {
        MvcResult r = login(identificador, clave);
        JsonNode raiz = json.readTree(r.getResponse().getContentAsString());
        return "Bearer " + raiz.get("token").asText();
    }

    @Test
    void sinTokenNoSeEntra() throws Exception {
        mvc.perform(get("/api/cubiculos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/panel")).andExpect(status().isUnauthorized());
    }

    @Test
    void elEstudianteEntraSoloConSuCodigoOSuCorreo() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"e1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("ESTUDIANTE"))
                .andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"E2@Tecsup.edu.pe\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/cubiculos").header("Authorization", token("E1", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cubiculos.length()").value(4));
    }

    @Test
    void uncodigoQueNoEstaEnLaListaNoEntra() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"NOEXISTE\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES"));
    }

    @Test
    void elAdministradorSiempreNecesitaSuContrasena() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"ADMIN\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"ADMIN\",\"clave\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"ADMIN\",\"clave\":\"" + CLAVE_ADMIN + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("ADMIN"));
    }

    @Test
    void cadaRolSoloEntraASuZona() throws Exception {
        String estudiante = token("E1", "");
        String administrador = token("ADMIN", CLAVE_ADMIN);
        mvc.perform(get("/api/admin/panel").header("Authorization", estudiante)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/panel").header("Authorization", administrador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cubiculos.length()").value(4));
        mvc.perform(get("/api/reservas/mias").header("Authorization", administrador)).andExpect(status().isForbidden());
        mvc.perform(get("/api/reservas/mias").header("Authorization", estudiante)).andExpect(status().isOk());
    }

    @Test
    void laMaquetaEntraSoloConSuToken() throws Exception {
        mvc.perform(post("/api/sensores/1/presencia")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/sensores/1/presencia").header("X-Sensor-Token", "otro-token"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/sensores/1/presencia").header("X-Sensor-Token", "token-de-pruebas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cubiculo").value(1))
                .andExpect(jsonPath("$.estado").value("LIBRE"))
                .andExpect(jsonPath("$.luz").value("APAGADA"));
        mvc.perform(get("/api/sensores/9/estado").header("X-Sensor-Token", "token-de-pruebas"))
                .andExpect(status().isNotFound());
        // un estudiante con su JWT no puede hacerse pasar por la maqueta
        mvc.perform(post("/api/sensores/1/presencia").header("Authorization", token("E1", "")))
                .andExpect(status().isForbidden());
    }

    @Test
    void reservarPorApiDevuelve201YLosErroresTienenFormato() throws Exception {
        String estudiante = token("E1", "");
        Long cubiculo = cub(1).getId();
        String cuerpo = "{\"cubiculoId\":" + cubiculo + ",\"horaInicio\":\"10:00\",\"integrantes\":[]}";
        mvc.perform(post("/api/reservas").header("Authorization", estudiante)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("RESERVADA"))
                .andExpect(jsonPath("$.horaFin").value("11:00"));
        mvc.perform(post("/api/reservas").header("Authorization", estudiante)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CUOTA_AGOTADA"));
        mvc.perform(post("/api/reservas").header("Authorization", token("E2", ""))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cubiculoId\":" + cubiculo + ",\"horaInicio\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
    }
}
