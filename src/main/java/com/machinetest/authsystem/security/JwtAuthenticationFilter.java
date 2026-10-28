package com.machinetest.authsystem.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.machinetest.authsystem.user.InMemoryUserRepository;
import com.machinetest.authsystem.user.User;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTRIBUTE = "authError";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final TokenBlacklist tokenBlacklist;
    private final InMemoryUserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, TokenBlacklist tokenBlacklist,
                                   InMemoryUserRepository userRepository) {
        this.jwtService = jwtService;
        this.tokenBlacklist = tokenBlacklist;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            authenticate(request, header.substring(BEARER_PREFIX.length()).trim());
        }
        chain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        Optional<AuthenticatedUser> parsed = jwtService.parse(token);
        if (parsed.isEmpty()) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Token is invalid or expired");
            return;
        }
        AuthenticatedUser principal = parsed.get();
        if (tokenBlacklist.isRevoked(principal.tokenId())) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Token has been logged out");
            return;
        }
        Optional<User> user = userRepository.findByUsername(principal.username());
        if (user.isEmpty() || !user.get().isVerified() || user.get().userType() != principal.userType()) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Token is not valid for any active user");
            return;
        }
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority(principal.userType().getAuthority())));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}
