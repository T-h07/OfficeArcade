package com.officearcade.server.health;

public record HealthResponse(
        String status,
        String application,
        String profile,
        String timestamp
) {
}
