package com.freehost.common.security;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/** Limite les tentatives échouées par adresse IP : trop d'échecs = blocage temporaire. */
public final class RateLimiter {
    private final int maxFails;
    private final long windowMs;
    private final long blockMs;
    private final Map<String, Deque<Long>> fails = new HashMap<>();
    private final Map<String, Long> blockedUntil = new HashMap<>();

    public RateLimiter(int maxFails, long windowMs, long blockMs) {
        this.maxFails = maxFails;
        this.windowMs = windowMs;
        this.blockMs = blockMs;
    }

    public synchronized boolean isBlocked(String key) {
        Long until = blockedUntil.get(key);
        if (until == null) return false;
        if (System.currentTimeMillis() >= until) {
            blockedUntil.remove(key);
            return false;
        }
        return true;
    }

    public synchronized void fail(String key) {
        long now = System.currentTimeMillis();
        Deque<Long> q = fails.computeIfAbsent(key, k -> new ArrayDeque<>());
        q.addLast(now);
        while (!q.isEmpty() && now - q.peekFirst() > windowMs) q.pollFirst();
        if (q.size() >= maxFails) {
            blockedUntil.put(key, now + blockMs);
            q.clear();
        }
    }
}
