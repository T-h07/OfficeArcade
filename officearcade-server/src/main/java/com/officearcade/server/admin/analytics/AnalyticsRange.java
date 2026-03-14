package com.officearcade.server.admin.analytics;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

public enum AnalyticsRange {
    TODAY("today", "Today", 1),
    LAST_7_DAYS("7d", "Last 7 Days", 7),
    LAST_30_DAYS("30d", "Last 30 Days", 30),
    ALL("all", "All Time", null);

    private final String queryValue;
    private final String label;
    private final Integer boundedDays;

    AnalyticsRange(String queryValue, String label, Integer boundedDays) {
        this.queryValue = queryValue;
        this.label = label;
        this.boundedDays = boundedDays;
    }

    public String queryValue() {
        return queryValue;
    }

    public String label() {
        return label;
    }

    public Integer boundedDays() {
        return boundedDays;
    }

    public Instant resolveRangeStart(Instant now, ZoneId zoneId) {
        LocalDate today = LocalDate.ofInstant(now, zoneId);
        return switch (this) {
            case TODAY -> today.atStartOfDay(zoneId).toInstant();
            case LAST_7_DAYS -> today.minusDays(6).atStartOfDay(zoneId).toInstant();
            case LAST_30_DAYS -> today.minusDays(29).atStartOfDay(zoneId).toInstant();
            case ALL -> null;
        };
    }

    public LocalDate resolveTrendStart(LocalDate today) {
        return switch (this) {
            case TODAY -> today;
            case LAST_7_DAYS -> today.minusDays(6);
            case LAST_30_DAYS, ALL -> today.minusDays(29);
        };
    }

    public static AnalyticsRange fromQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return LAST_7_DAYS;
        }

        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "today", "1d", "24h" -> TODAY;
            case "7d", "last7d", "last_7_days", "week" -> LAST_7_DAYS;
            case "30d", "last30d", "last_30_days", "month" -> LAST_30_DAYS;
            case "all", "alltime", "all_time" -> ALL;
            default -> LAST_7_DAYS;
        };
    }
}
