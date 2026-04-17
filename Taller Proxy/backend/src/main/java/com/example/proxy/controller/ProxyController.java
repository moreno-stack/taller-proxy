package com.example.proxy.controller;

import com.example.proxy.model.ChatRequest;
import com.example.proxy.model.ChatResponse;
import com.example.proxy.model.QuotaStatus;
import com.example.proxy.model.UserState;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
public class ProxyController {
    private final Map<String, UserState> users = new ConcurrentHashMap<>();

    private static final Map<String, PlanInfo> PLANS = Map.of(
            "FREE", new PlanInfo("FREE", 1000, 5, 7),
            "PRO", new PlanInfo("PRO", 5000, 20, 30),
            "ENTERPRISE", new PlanInfo("ENTERPRISE", 20000, 60, 60)
    );

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                             @RequestBody ChatRequest request) {
        userId = Optional.ofNullable(userId).filter(id -> !id.isBlank()).orElse("default-user");
        UserState state = users.computeIfAbsent(userId, this::createUserState);
        synchronized (state) {
            resetIfNeeded(state);
            int estimatedTokens = estimateTokens(request.getPrompt());
            if (state.isMonthlyQuotaExceeded(estimatedTokens)) {
                return ResponseEntity.badRequest().body(new ChatResponse(
                        "Cuota mensual agotada. Por favor actualiza el plan.", 0, state.getMonthlyUsedTokens(), state.getMonthlyQuota(), state.getRateLimitRemaining(), state.getSecondsUntilRateLimitReset(), true));
            }
            if (!state.canSendRequest()) {
                return ResponseEntity.status(429).body(new ChatResponse(
                        "Límite de requests por minuto alcanzado. Espera unos segundos.", 0, state.getMonthlyUsedTokens(), state.getMonthlyQuota(), state.getRateLimitRemaining(), state.getSecondsUntilRateLimitReset(), true));
            }
            state.consumeRequest(estimatedTokens);
            String answer = generateResponse(request.getPrompt());
            updateDailyUsage(state, estimatedTokens);
            return ResponseEntity.ok(new ChatResponse(answer, estimatedTokens, state.getMonthlyUsedTokens(), state.getMonthlyQuota(), state.getRateLimitRemaining(), state.getSecondsUntilRateLimitReset(), false));
        }
    }

    @GetMapping("/usage")
    public QuotaStatus usage(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        userId = Optional.ofNullable(userId).filter(id -> !id.isBlank()).orElse("default-user");
        UserState state = users.computeIfAbsent(userId, this::createUserState);
        synchronized (state) {
            resetIfNeeded(state);
            return QuotaStatus.fromState(state);
        }
    }

    private UserState createUserState(String userId) {
        PlanInfo planInfo = PLANS.get("FREE");
        return new UserState(userId, planInfo.name, planInfo.monthlyQuota, planInfo.requestsPerMinute, planInfo.dailyHistorySize);
    }

    private void resetIfNeeded(UserState state) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (!today.equals(state.getLastMonthlyReset())) {
            state.resetMonthlyUsage(today);
        }
        state.resetRateLimitIfNeeded();
    }

    private int estimateTokens(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return 1;
        }
        int tokens = Math.max(1, prompt.trim().length() / 4);
        return Math.min(tokens, 200);
    }

    private String generateResponse(String prompt) {
        String base = prompt == null ? "" : prompt.trim();
        if (base.isBlank()) {
            return "Hola, ¿en qué puedo ayudarte hoy?";
        }

        String lower = base.toLowerCase();
        if (lower.contains("tarea") || lower.contains("trabajo") || lower.contains("ensayo") || lower.contains("proyecto")) {
            return "Aquí tienes un plan paso a paso para completar esa tarea:\n\n"
                    + "1. Define el objetivo principal y los requisitos.\n"
                    + "2. Escribe un esquema con las secciones o entregables clave.\n"
                    + "3. Investiga ejemplos o fuentes relevantes para apoyar tu contenido.\n"
                    + "4. Redacta cada parte con claridad, utilizando introducción, desarrollo y conclusión.\n"
                    + "5. Revisa el trabajo, corrige ortografía y ajusta el formato según las instrucciones.\n\n"
                    + "Si quieres, dime el tema específico y te preparo un esquema detallado.";
        }

        if (lower.contains("idea") || lower.contains("ideas") || lower.contains("crear") || lower.contains("propuesta")) {
            return "Te comparto algunas ideas creativas que puedes usar:\n\n"
                    + "• Idea 1: Para un trabajo escolar, crea una guía paso a paso con ejemplos y conclusiones.\n"
                    + "• Idea 2: Para una presentación, diseña diapositivas con estructura clara: problema, solución y resultados.\n"
                    + "• Idea 3: Para un proyecto creativo, elabora un plan con tareas, recursos y plazos.\n\n"
                    + "Dime si quieres que convierta una de estas ideas en un esquema completo o contenido listo para entregar.";
        }

        if (lower.contains("explica") || lower.contains("qué es") || lower.contains("como funciona") || lower.contains("define")) {
            return "Te explico el concepto de forma clara y práctica:\n\n"
                    + "1. Primero, describe qué es y para qué sirve.\n"
                    + "2. Luego, muestra cómo se utiliza con un ejemplo concreto.\n"
                    + "3. Finalmente, enumera los beneficios y los pasos principales.\n\n"
                    + "Por ejemplo, puedo ayudarte a explicar cómo funciona Spring Boot, qué se necesita para un ensayo o cómo desarrollar un proyecto escolar.\n"
                    + "Dime el tema concreto y te preparo una explicación completa con ejemplos.";
        }

        if (lower.contains("plan") || lower.contains("estrategia") || lower.contains("organizar")) {
            return "Vamos a estructurar una estrategia clara para tu proyecto:\n\n"
                    + "• Paso 1: Identifica el objetivo específico.\n"
                    + "• Paso 2: Divide el trabajo en tareas pequeñas y ordenadas.\n"
                    + "• Paso 3: Establece prioridades y tiempos para cada parte.\n"
                    + "• Paso 4: Revisa el progreso y ajusta según lo necesario.\n\n"
                    + "Puedo ayudarte también a crear un calendario o una lista de tareas detallada.";
        }

        return "Aquí tienes una respuesta más útil y práctica:\n\n"
                + "• Describe claramente el problema o la necesidad que tienes.\n"
                + "• Ofrece opciones o ideas que puedas aplicar hoy mismo.\n"
                + "• Si quieres, dime tu tema exacto y te preparo un contenido personalizado, paso a paso.";
    }

    private void updateDailyUsage(UserState state, int tokens) {
        state.addDailyUsage(tokens);
    }

    private static final class PlanInfo {
        final String name;
        final int monthlyQuota;
        final int requestsPerMinute;
        final int dailyHistorySize;

        PlanInfo(String name, int monthlyQuota, int requestsPerMinute, int dailyHistorySize) {
            this.name = name;
            this.monthlyQuota = monthlyQuota;
            this.requestsPerMinute = requestsPerMinute;
            this.dailyHistorySize = dailyHistorySize;
        }
    }
}
