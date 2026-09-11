package com.lwe.security;

import com.lwe.core.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        var token = authHeader.substring(7);
        try {
            var userId = jwtService.extractUserId(token);
            var role = jwtService.extractRole(token);
            if (role == null || role.isBlank()) {
                throw new JwtService.TokenInvalidException("Missing role claim", null);
            }

            var userOpt = userRepository.findById(java.util.UUID.fromString(userId));
            if (userOpt.isEmpty()) {
                chain.doFilter(request, response);
                return;
            }

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            var auth = new UsernamePasswordAuthenticationToken(userOpt.get(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtService.TokenExpiredException e) {
            writeUnauthorized(response, "AUTH_TOKEN_EXPIRED");
            return;
        } catch (JwtService.TokenInvalidException | IllegalArgumentException e) {
            writeUnauthorized(response, "AUTH_TOKEN_INVALID");
            return;
        }

        chain.doFilter(request, response);
    }

    private static void writeUnauthorized(HttpServletResponse response, String code) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("""
            {"error":{"code":"%s","message":"Authentication failed"}}
            """.formatted(code));
    }
}
