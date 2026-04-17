package com.example.proxy.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserState {
    private final String userId;
    private final String plan;
    private final int monthlyQuota;
    private final int requestsPerMinute;
    private final int dailyHistorySize;
    private int monthlyUsedTokens;
    private LocalDate lastMonthlyReset;
    private int currentMinuteCount;
    private LocalDateTime rateLimitWindowStart;
    private LocalDate lastDailyUpdate;
    private final List<Integer> dailyUsage;

    public UserState(String userId, String plan, int monthlyQuota, int requestsPerMinute, int dailyHistorySize) {
        this.userId = userId;
        this.plan = plan;
        this.monthlyQuota = monthlyQuota;
        this.requestsPerMinute = requestsPerMinute;
        this.dailyHistorySize = dailyHistorySize;
        this.monthlyUsedTokens = 0;
        this.lastMonthlyReset = LocalDate.now(ZoneOffset.UTC);
        this.currentMinuteCount = 0;
        this.rateLimitWindowStart = LocalDateTime.now(ZoneOffset.UTC);
        this.lastDailyUpdate = LocalDate.now(ZoneOffset.UTC);
        this.dailyUsage = new ArrayList<>(Collections.nCopies(dailyHistorySize, 0));
    }

    public String getUserId() {
        return userId;
    }

    public String getPlan() {
        return plan;
    }

    public int getMonthlyQuota() {
        return monthlyQuota;
    }

    public int getMonthlyUsedTokens() {
        return monthlyUsedTokens;
    }

    public LocalDate getLastMonthlyReset() {
        return lastMonthlyReset;
    }

    public int getRateLimitRemaining() {
        return Math.max(0, requestsPerMinute - currentMinuteCount);
    }

    public long getSecondsUntilRateLimitReset() {
        long elapsed = Duration.between(rateLimitWindowStart, LocalDateTime.now(ZoneOffset.UTC)).getSeconds();
        return Math.max(0, 60 - elapsed);
    }

    public List<Integer> getDailyUsage() {
        return List.copyOf(dailyUsage);
    }

    public boolean canSendRequest() {
        resetRateLimitIfNeeded();
        return currentMinuteCount < requestsPerMinute;
    }

    public void consumeRequest(int tokens) {
        currentMinuteCount++;
        monthlyUsedTokens += tokens;
    }

    public boolean isMonthlyQuotaExceeded(int tokens) {
        return monthlyUsedTokens + tokens > monthlyQuota;
    }

    public void resetMonthlyUsage(LocalDate today) {
        monthlyUsedTokens = 0;
        lastMonthlyReset = today;
        lastDailyUpdate = today;
        dailyUsage.clear();
        for (int i = 0; i < dailyHistorySize; i++) {
            dailyUsage.add(0);
        }
    }

    public void resetRateLimitIfNeeded() {
        long seconds = Duration.between(rateLimitWindowStart, LocalDateTime.now(ZoneOffset.UTC)).getSeconds();
        if (seconds >= 60) {
            currentMinuteCount = 0;
            rateLimitWindowStart = LocalDateTime.now(ZoneOffset.UTC);
        }
    }

    public void addDailyUsage(int tokens) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        shiftDailyUsage(today);
        int index = dailyUsage.size() - 1;
        dailyUsage.set(index, dailyUsage.get(index) + tokens);
    }

    private void shiftDailyUsage(LocalDate today) {
        long days = ChronoUnit.DAYS.between(lastDailyUpdate, today);
        if (days <= 0) {
            return;
        }
        if (days >= dailyHistorySize) {
            dailyUsage.clear();
            for (int i = 0; i < dailyHistorySize; i++) {
                dailyUsage.add(0);
            }
        } else {
            for (int i = 0; i < days; i++) {
                dailyUsage.remove(0);
                dailyUsage.add(0);
            }
        }
        lastDailyUpdate = today;
    }
}
