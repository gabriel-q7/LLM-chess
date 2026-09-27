package com.example.chess.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * HTTP adapter for laya-serve. Laya is a typed-decision model, not a chat model: it exposes no
 * OpenAI- or Ollama-compatible endpoint, so no Spring AI model integration applies to it.
 */
public class LayaClient {

    private static final Logger log = LoggerFactory.getLogger(LayaClient.class);

    private final RestClient restClient;

    public LayaClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public LayaModels.Response decide(LayaModels.Request request) {
        log.debug("Laya state:\n{}", request.state());
        try {
            LayaModels.Response response = restClient.post()
                    .uri("/v1/systemone")
                    .body(request)
                    .retrieve()
                    .body(LayaModels.Response.class);
            if (response == null || response.answers() == null) {
                throw new AiServiceUnavailableException("Laya returned an empty response", null);
            }
            return response;
        } catch (RestClientException e) {
            throw new AiServiceUnavailableException("Laya request failed", e);
        }
    }
}
