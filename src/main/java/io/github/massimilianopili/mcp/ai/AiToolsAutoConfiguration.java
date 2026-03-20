package io.github.massimilianopili.mcp.ai;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnProperty(name = "mcp.ai.enabled", havingValue = "true")
@EnableConfigurationProperties(AiProperties.class)
@Import({AiConfig.class, OllamaClient.class, AiTextTools.class})
public class AiToolsAutoConfiguration {
}
