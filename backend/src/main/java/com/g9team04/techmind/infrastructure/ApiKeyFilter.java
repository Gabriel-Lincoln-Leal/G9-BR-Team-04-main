package com.g9team04.techmind.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Autenticação simples para o MVP; a chave deve vir de uma variável de ambiente. */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {
    private final byte[] expectedKey;

    public ApiKeyFilter(@Value("${techmind.api-key}") String apiKey) {
        if (apiKey == null || apiKey.isBlank() || apiKey.length() < 32) {
            throw new IllegalArgumentException("TECHMIND_API_KEY deve ter pelo menos 32 caracteres");
        }
        this.expectedKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().equals("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ") ||
                !MessageDigest.isEqual(expectedKey,
                        authorization.substring(7).getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
