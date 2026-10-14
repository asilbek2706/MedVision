package com.medvision.analysis;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AnalysisEvaluationControllerTest {

    @Test
    void acceptsMultipartEvaluationRequest() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID analysisId = UUID.randomUUID();

        var service = mock(AnalysisEvaluationExecutionService.class);
        var controller = new AnalysisEvaluationController(service);

        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();

        var file = new MockMultipartFile(
                "file",
                "ground-truth.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        ownerId.toString(),
                        null
                );

        var response = mock(AnalysisResponse.class);

        when(service.evaluate(
                eq(ownerId),
                eq(analysisId),
                any()
        )).thenReturn(response);

        mvc.perform(
                multipart("/api/analyses/{id}/evaluate", analysisId)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .principal(authentication)
        ).andExpect(status().isOk());

        verify(service).evaluate(
                eq(ownerId),
                eq(analysisId),
                argThat(upload ->
                        upload != null
                        && "ground-truth.png".equals(
                                upload.getOriginalFilename()
                        )
                        && "image/png".equals(
                                upload.getContentType()
                        )
                )
        );
    }
}