package com.lwe.api;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * TDD (P25-T06): Welten kommen ohne gameSystemId aus. Alte Clients, die das
 * Feld noch mitsenden, dürfen nicht mit 500 scheitern (Fallback: ignorieren).
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class WorldCreateWithoutSystemIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String tokenFor(String tag) throws Exception {
        var email = "wcs-" + tag + "-" + System.nanoTime() + "@test.de";
        var res = mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("email", email, "username", "wcs" + tag, "password", "Test123!x"))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(res).path("accessToken").asText();
    }

    @Test
    void createWorldWithoutSystem() throws Exception {
        var token = tokenFor("plain");
        mvc.perform(post("/api/v1/worlds")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"NoSys\"}"))
            .andExpect(status().isCreated())
            .andExpect(content().string(not(containsString("gameSystemId"))));
    }

    @Test
    void createWorldIgnoresLegacySystemParam() throws Exception {
        var token = tokenFor("legacy");
        mvc.perform(post("/api/v1/worlds")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Legacy\",\"gameSystemId\":\"" + UUID.randomUUID() + "\"}"))
            .andExpect(status().isCreated());
    }
}
