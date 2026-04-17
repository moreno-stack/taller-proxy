package com.example.proxy.model;

public class ChatResponse {
    private String message;
    private int usedTokens;
    private int monthlyUsedTokens;
    private int monthlyQuota;
    private int rateLimitRemaining;
    private long rateLimitResetSeconds;
    private boolean blocked;

    public ChatResponse() {
    }

    public ChatResponse(String message, int usedTokens, int monthlyUsedTokens, int monthlyQuota, int rateLimitRemaining, long rateLimitResetSeconds, boolean blocked) {
        this.message = message;
        this.usedTokens = usedTokens;
        this.monthlyUsedTokens = monthlyUsedTokens;
        this.monthlyQuota = monthlyQuota;
        this.rateLimitRemaining = rateLimitRemaining;
        this.rateLimitResetSeconds = rateLimitResetSeconds;
        this.blocked = blocked;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getUsedTokens() {
        return usedTokens;
    }

    public void setUsedTokens(int usedTokens) {
        this.usedTokens = usedTokens;
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

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}
