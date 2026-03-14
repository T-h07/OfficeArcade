package com.officearcade.server.health;

import com.officearcade.server.common.RuntimeProfileResolver;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final RuntimeProfileResolver runtimeProfileResolver;

    @Value("${spring.application.name:officearcade-server}")
    private String applicationName;

    public HealthController(RuntimeProfileResolver runtimeProfileResolver) {
        this.runtimeProfileResolver = runtimeProfileResolver;
    }

    @GetMapping
    public HealthResponse getHealth() {
        return new HealthResponse(
                "UP",
                applicationName,
                runtimeProfileResolver.resolveCurrentProfile(),
                Instant.now().toString()
        );
    }
}
