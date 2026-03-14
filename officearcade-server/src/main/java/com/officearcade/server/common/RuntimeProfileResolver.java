package com.officearcade.server.common;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class RuntimeProfileResolver {

    private final Environment environment;

    public RuntimeProfileResolver(Environment environment) {
        this.environment = environment;
    }

    public String resolveCurrentProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return "local";
        }
        return String.join(",", activeProfiles);
    }
}
