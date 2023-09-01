package spring.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "art-center-api")
public class ArtAiCenterApiProperties {
    private String host;
    private String pullRequest;
}
