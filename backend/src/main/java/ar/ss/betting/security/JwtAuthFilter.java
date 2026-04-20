package ar.ss.betting.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Placed before default Spring UsernamePasswordAuthenticationFilter
// Uses JWTUtil for JWT operations for a client that has a token already
// Adds principal object to SecurityContextHolder directly,
// so that subsequent UsernamePasswordAuthenticationFilter sees the principal and is
// effectively skipped
public class JwtAuthFilter extends OncePerRequestFilter {

    private final CustomUserDetailsService customUserDetailsService;
    private final JWTUtil jwtUtil;

    public JwtAuthFilter(CustomUserDetailsService customUserDetailsService, JWTUtil jwtUtil) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = null;
        String username = null;

        // Extract username from token
        String authHeader = request.getHeader("Authorization");
        if(authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            username = jwtUtil.extractUsername(token);
        }

        // Extra verification, add principal to SecurityContext if valid
        if(username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails =customUserDetailsService.loadUserByUsername(username);
            if(jwtUtil.validateToken(username,userDetails,token)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Advance the chain
        filterChain.doFilter(request,response);
    }
}
