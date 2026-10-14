package com.medvision.analysis;


import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AnalysisOptionsHttpTest {

    @Autowired MockMvc mvc;
    @Autowired JsonMapper mapper;


    @Test
    void listsAllAlgorithms() throws Exception {
        mvc.perform(get("/api/analysis-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plannedAlgorithms.length()").value(3))
                .andExpect(jsonPath("$.plannedAlgorithms[0]").value("OTSU"))
                .andExpect(jsonPath("$.plannedAlgorithms[1]").value("MANUAL"))
                .andExpect(jsonPath("$.plannedAlgorithms[2]").value("ADAPTIVE"));
    }

    @Test
    void acceptsValidAlgorithms() throws Exception {
        validate(Map.of(
                "name", "Otsu test",
                "algorithm", "OTSU",
                "blurKernelSize", 5,
                "invert", false
        ), 200);

        validate(Map.of(
                "name", "Manual test",
                "algorithm", "MANUAL",
                "blurKernelSize", 5,
                "invert", false,
                "threshold", 128
        ), 200);

        validate(Map.of(
                "name", "Adaptive test",
                "algorithm", "ADAPTIVE",
                "blurKernelSize", 5,
                "invert", false,
                "blockSize", 11,
                "c", 2.0
        ), 200);
    }

    @Test
    void rejectsInvalidParameters() throws Exception {
        validate(Map.of(
                "name", "Bad manual",
                "algorithm", "MANUAL",
                "blurKernelSize", 5,
                "invert", false,
                "threshold", 300
        ), 400);

        validate(Map.of(
                "name", "Bad adaptive",
                "algorithm", "ADAPTIVE",
                "blurKernelSize", 5,
                "invert", false,
                "blockSize", 10,
                "c", 2.0
        ), 400);

        validate(Map.of(
                "name", "Missing manual threshold",
                "algorithm", "MANUAL",
                "blurKernelSize", 5,
                "invert", false
        ), 400);
    }

    private void validate(Map<String, Object> body, int expected)
            throws Exception {
        mvc.perform(post("/api/analysis-options/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(body)))
                .andExpect(status().is(expected));
    }
}