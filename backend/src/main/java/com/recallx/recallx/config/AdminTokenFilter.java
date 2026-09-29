package com.recallx.recallx.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * /api/admin/** (memory sync, recall check, demo reset) needs the X-Admin-Token header. These endpoints spend
 * Hindsight credits or delete data, so they fail closed: with no token configured, every request is refused.
 */
public class AdminTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Admin-Token";

    private final byte[] expected;

    public AdminTokenFilter(String adminToken) {
        this.expected = adminToken == null ? new byte[0] : adminToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/admin/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String given = request.getHeader(HEADER);
        boolean allowed = expected.length > 0 && given != null
                && MessageDigest.isEqual(expected, given.getBytes(StandardCharsets.UTF_8));
        if (allowed) {
            chain.doFilter(request, response);
            return;
        }
        String detail = expected.length == 0
                ? "Admin endpoints are disabled: set RECALLX_ADMIN_TOKEN."
                : "Send the admin token in the " + HEADER + " header.";
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"forbidden\",\"detail\":\"" + detail + "\"}");
    }
}
