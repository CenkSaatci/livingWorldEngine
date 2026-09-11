package com.lwe.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.not;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * TDD: öffentliche Auth-Endpunkte müssen ohne Token erreichbar sein
 * (kein 403), während change-password geschützt bleiben muss.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class PublicAuthEndpointsIT {

    @Autowired
    private MockMvc mvc;

    @Test
    void forgotPasswordIsPublic() throws Exception {
        mvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody-" + System.nanoTime() + "@test.de\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void verifyEmailIsPublic() throws Exception {
        // ungültiges Token -> 400, aber NIEMALS 403 (endpoint ist öffentlich)
        mvc.perform(post("/api/v1/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"bogus\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void resendVerificationIsPublic() throws Exception {
        mvc.perform(post("/api/v1/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody@test.de\"}"))
            .andExpect(status().is(not(403)));
    }

    @Test
    void resetPasswordIsPublic() throws Exception {
        mvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"bogus\",\"password\":\"Test123!x\"}"))
            .andExpect(status().is(not(403)));
    }

    @Test
    void changePasswordStaysProtected() throws Exception {        mvc.perform(post("/api/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"x\",\"newPassword\":\"Test123!x\"}"))
            .andExpect(status().isForbidden());
    }
}
