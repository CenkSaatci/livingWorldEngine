package com.lwe.config;

import jakarta.persistence.EntityManager;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Setzt {@code app.tenant_id} als PostgreSQL-Session-Variable vor jedem Request.
 * RLS-Policies nutzen diesen Wert zur Tenant-Isolation.
 *
 * @see <a href="../../docs/ADR/004-multitenancy-shared-schema.md">ADR-004</a>
 */
@Component
@Order(2)
public class TenantInterceptor implements Filter {

    private final EntityManager entityManager;

    public TenantInterceptor(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        var httpReq = (HttpServletRequest) request;
        var userId = httpReq.getUserPrincipal();
        if (userId != null) {
            // userId ist der Username (Email) — wir brauchen die UUID.
            // Die UUID wird aus der Authentication gesetzt (siehe JwtAuthFilter).
            // Fallback: aus dem SecurityContext ziehen.
            var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof com.lwe.core.domain.User user) {
                entityManager.createNativeQuery(
                    "SET LOCAL app.tenant_id = ?1"
                ).setParameter(1, user.getId().toString())
                 .executeUpdate();
            }
        }

        chain.doFilter(request, response);
    }
}
