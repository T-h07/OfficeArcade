package com.officearcade.server.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "officearcade.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
