package dev.ak.ai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpClientTool {

    private final RestClient restClient = RestClient.create();

    @Tool(description = "Call an external API and return the response as a string")
    public String callApi(String url) {
        try {
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            return "Error calling API: " + e.getMessage();
        }
    }
}
