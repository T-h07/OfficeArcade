package com.officearcade.server.playlimits;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "officearcade.play-limits")
public class PlayLimitPolicyProperties {

    private int dailyGameLimit = 5;
    private long cooldownMinutes = 90;
    private String timezone = "Europe/Berlin";

    public int getDailyGameLimit() {
        return Math.max(dailyGameLimit, 1);
    }

    public void setDailyGameLimit(int dailyGameLimit) {
        this.dailyGameLimit = dailyGameLimit;
    }

    public long getCooldownMinutes() {
        return Math.max(cooldownMinutes, 0L);
    }

    public void setCooldownMinutes(long cooldownMinutes) {
        this.cooldownMinutes = cooldownMinutes;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public ZoneId zoneId() {
        try {
            return ZoneId.of(timezone);
        } catch (Exception ignored) {
            return ZoneId.of("Europe/Berlin");
        }
    }
}
