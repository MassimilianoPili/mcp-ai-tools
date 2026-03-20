package io.github.massimilianopili.mcp.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "mcp.ai.enabled", havingValue = "true")
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean(name = "aiWebClient")
    public WebClient aiWebClient(AiProperties props) {
        log.info("AI WebClient: url={}, model={}, timeout={}s",
                props.getLlmUrl(), props.getLlmModel(), props.getTimeoutSeconds());

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(props.getTimeoutSeconds()));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Content-Type", "application/json")
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                        .build())
                .build();
    }
}
