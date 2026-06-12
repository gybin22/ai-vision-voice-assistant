package com.example.assistant.service;

import com.example.assistant.config.AssistantProperties;
import com.example.assistant.dto.SessionUsageDTO;
import com.example.assistant.exception.CostLimitExceededException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CostControlService {
    private final AssistantProperties properties;
    private final Map<String, SessionStats> sessionStats = new ConcurrentHashMap<>();
    private final Map<String, IpStats> ipStats = new ConcurrentHashMap<>();

    public CostControlService(AssistantProperties properties) {
        this.properties = properties;
    }

    public void validateRequest(String sessionId, String clientIp, String question, int frameCount, long totalImageBytes, int maxOutputTokens) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new CostLimitExceededException("SESSION_REQUIRED", "缺少 sessionId");
        }
        if (question == null || question.isBlank()) {
            throw new CostLimitExceededException("QUESTION_REQUIRED", "问题不能为空");
        }
        if (question.length() > properties.getCost().getMaxQuestionLength()) {
            throw new CostLimitExceededException("QUESTION_TOO_LONG", "问题过长，请控制在 " + properties.getCost().getMaxQuestionLength() + " 字以内。");
        }
        if (frameCount <= 0) {
            throw new CostLimitExceededException("IMAGE_REQUIRED", "至少需要上传 1 张关键帧。");
        }
        if (frameCount > properties.getCost().getMaxFrameCount()) {
            throw new CostLimitExceededException("TOO_MANY_FRAMES", "关键帧数量过多，最多允许 " + properties.getCost().getMaxFrameCount() + " 张。");
        }
        if (totalImageBytes > properties.getCost().getMaxTotalImageBytes()) {
            throw new CostLimitExceededException("IMAGE_SEQUENCE_TOO_LARGE", "关键帧总大小过大，请减少关键帧数量或降低截图质量。");
        }
        if (maxOutputTokens > properties.getCost().getMaxOutputTokens()) {
            throw new CostLimitExceededException("MAX_OUTPUT_TOO_LARGE", "输出长度超过系统限制。");
        }

        LocalDate today = LocalDate.now();
        SessionStats session = sessionStats.computeIfAbsent(sessionId, key -> new SessionStats(today));
        session.resetIfNewDay(today);

        long now = System.currentTimeMillis();
        if (now - session.lastRequestAt < properties.getCost().getMinSessionIntervalMs()) {
            throw new CostLimitExceededException("RATE_LIMITED", "请求过于频繁，请稍后再试。");
        }
        if (session.requestCount.get() >= properties.getCost().getMaxSessionRequestsPerDay()) {
            throw new CostLimitExceededException("SESSION_DAILY_LIMIT", "当前会话今日请求次数已达上限。");
        }

        String ip = clientIp == null ? "unknown" : clientIp;
        IpStats ipStat = ipStats.computeIfAbsent(ip, key -> new IpStats(today));
        ipStat.resetIfNewDay(today);
        if (ipStat.requestCount.get() >= properties.getCost().getMaxIpRequestsPerDay()) {
            throw new CostLimitExceededException("IP_DAILY_LIMIT", "当前 IP 今日请求次数已达上限。");
        }

        session.lastRequestAt = now;
    }

    public void recordUsage(String sessionId, String clientIp, double estimatedCost) {
        LocalDate today = LocalDate.now();
        SessionStats session = sessionStats.computeIfAbsent(sessionId, key -> new SessionStats(today));
        session.resetIfNewDay(today);
        session.requestCount.incrementAndGet();
        session.addEstimatedCost(estimatedCost);

        String ip = clientIp == null ? "unknown" : clientIp;
        IpStats ipStat = ipStats.computeIfAbsent(ip, key -> new IpStats(today));
        ipStat.resetIfNewDay(today);
        ipStat.requestCount.incrementAndGet();
    }

    public SessionUsageDTO getSessionUsage(String sessionId) {
        LocalDate today = LocalDate.now();
        SessionStats session = sessionStats.computeIfAbsent(sessionId, key -> new SessionStats(today));
        session.resetIfNewDay(today);
        int limit = properties.getCost().getMaxSessionRequestsPerDay();
        int count = session.requestCount.get();
        return new SessionUsageDTO(sessionId, count, limit,session.getEstimatedCost(), Math.max(0, limit - count));
    }

    private static class SessionStats {
        private LocalDate date;
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private volatile long lastRequestAt = 0;
        private double estimatedCost = 0.0;

        private SessionStats(LocalDate date) {
            this.date = date;
        }

        private synchronized void resetIfNewDay(LocalDate today) {
            if (!today.equals(date)) {
                date = today;
                requestCount.set(0);
                lastRequestAt = 0;
                estimatedCost = 0.0;
            }
        }

        private synchronized void addEstimatedCost(double value) {
            estimatedCost += value;
        }

        private synchronized double getEstimatedCost() {
            return estimatedCost;
        }
    }

    private static class IpStats {
        private LocalDate date;
        private final AtomicInteger requestCount = new AtomicInteger(0);

        private IpStats(LocalDate date) {
            this.date = date;
        }

        private synchronized void resetIfNewDay(LocalDate today) {
            if (!today.equals(date)) {
                date = today;
                requestCount.set(0);
            }
        }
    }
}
