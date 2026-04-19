package ar.ss.betting.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader();
        String jwtToken = header.substring(7);

        // validate for security
        String username = JWTUtils.extractUsername(jwtToken);
        customUserDetailsService.loadUserByUsername(username);


        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken();


        SecurityContextHolder.getContext().setAuthentication(authToken))

        // Advance the chain
        filterChain.doFilter(request,response);
    }
}
