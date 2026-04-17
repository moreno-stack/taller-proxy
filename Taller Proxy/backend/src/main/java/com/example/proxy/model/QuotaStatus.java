package com.example.proxy.model;

import java.util.List;

public class QuotaStatus {
    private String plan;
    private int monthlyUsedTokens;
    private int monthlyQuota;
    private int remainingTokens;
    private int rateLimitRemaining;
    private long rateLimitResetSeconds;
    private List<Integer> dailyUsage;

    public QuotaStatus() {
    }

    public static QuotaStatus fromState(UserState state) {
        QuotaStatus status = new QuotaStatus();
        status.plan = state.getPlan();
        status.monthlyUsedTokens = state.getMonthlyUsedTokens();
        status.monthlyQuota = state.getMonthlyQuota();
        status.remainingTokens = Math.max(0, state.getMonthlyQuota() - state.getMonthlyUsedTokens());
        status.rateLimitRemaining = state.getRateLimitRemaining();
        status.rateLimitResetSeconds = state.getSecondsUntilRateLimitReset();
        status.dailyUsage = state.getDailyUsage();
        return status;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public int getMonthlyUsedTokens() {
        return monthlyUsedTokens;
    }

    public void setMonthlyUsedTokens(int monthlyUsedTokens) {
        this.monthlyUsedTokens = monthlyUsedTokens;
    }

    public int getMonthlyQuota() {
        return monthlyQuota;
    }

    public void setMonthlyQuota(int monthlyQuota) {
        this.monthlyQuota = monthlyQuota;
    }

    public int getRemainingTokens() {
        return remainingTokens;
    }

    public void setRemainingTokens(int remainingTokens) {
        this.remainingTokens = remainingTokens;
    }

    public int getRateLimitRemaining() {
        return rateLimitRemaining;
    }

    public void setRateLimitRemaining(int rateLimitRemaining) {
        this.rateLimitRemaining = rateLimitRemaining;
    }

    public long getRateLimitResetSeconds() {
        return rateLimitResetSeconds;
    }

    public void setRateLimitResetSeconds(long rateLimitResetSeconds) {
        this.rateLimitResetSeconds = rateLimitResetSeconds;
    }

    public List<Integer> getDailyUsage() {
        return dailyUsage;
    }

    public void setDailyUsage(List<Integer> dailyUsage) {
        this.dailyUsage = dailyUsage;
    }
}
