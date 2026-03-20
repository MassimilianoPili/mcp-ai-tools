package io.github.massimilianopili.mcp.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mcp.ai")
public class AiProperties {

    private boolean enabled = false;
    private String llmUrl;
    private String llmModel;
    private int maxInputLength = 32000;
    private int timeoutSeconds = 120;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getLlmUrl() { return llmUrl; }
    public void setLlmUrl(String llmUrl) { this.llmUrl = llmUrl; }

    public String getLlmModel() { return llmModel; }
    public void setLlmModel(String llmModel) { this.llmModel = llmModel; }

    public int getMaxInputLength() { return maxInputLength; }
    public void setMaxInputLength(int maxInputLength) { this.maxInputLength = maxInputLength; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public String getGenerateEndpoint() {
        String base = llmUrl;
        if (base != null && base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/api/generate";
    }
}
