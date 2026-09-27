package com.example.chess.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LayaClientTest {

    private MockRestServiceServer server;
    private LayaClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://laya:8000");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new LayaClient(builder.build());
    }

    @Test
    void postsTypedQuestionsAndReadsAnswers() {
        server.expect(requestTo("http://laya:8000/v1/systemone"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("english"))
                .andExpect(jsonPath("$.state").value("some state"))
                .andExpect(jsonPath("$.questions.intent.type").value("choice"))
                .andExpect(jsonPath("$.questions.intent.criteria.best_move").value("hint"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess("""
                        {"model":"laya-rl-agent","answers":{"intent":{"type":"choice","choice":"best_move",
                         "probabilities":{"best_move":0.9,"other":0.1},"confidence":0.5,
                         "action":{"act_probability":1.0}}},"usage":{"input_tokens":10,"output_tokens":0}}
                        """, MediaType.APPLICATION_JSON));

        LayaModels.Response response = client.decide(new LayaModels.Request("english", "some state",
                Map.of("intent", LayaModels.Question.choice("What?", Map.of("best_move", "hint", "other", "else")))));

        assertThat(response.answer("intent").choice()).isEqualTo("best_move");
        assertThat(response.answer("intent").choiceProbability()).isEqualTo(0.9);
        server.verify();
    }

    @Test
    void wrapsServerErrors() {
        server.expect(requestTo("http://laya:8000/v1/systemone")).andRespond(withServerError());

        assertThatThrownBy(() -> client.decide(new LayaModels.Request("english", "s", Map.of())))
                .isInstanceOf(AiServiceUnavailableException.class);
    }
}
