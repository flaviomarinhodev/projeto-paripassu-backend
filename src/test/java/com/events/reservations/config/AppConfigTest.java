package com.events.reservations.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AppConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowLoopbackOriginForFrontendRequests() throws Exception {
        mockMvc.perform(options("/api/events")
//                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:8080")
                        .header(HttpHeaders.ORIGIN, "https://projeto-paripassu-frontend-deploy.onrender.com/")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
//                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:8080"));
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://projeto-paripassu-frontend-deploy.onrender.com/"));

    }
}
