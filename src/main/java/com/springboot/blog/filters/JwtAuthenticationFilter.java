package com.springboot.blog.filters;

import com.springboot.blog.jwt.MyUserDetailsService;
import com.springboot.blog.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@AllArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    final JwtTokenProvider jwtTokenProvider;
    final MyUserDetailsService myUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // extract token form header
        String token = getTokenFromHeader(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            // extract username from token
            String username = jwtTokenProvider.extractUsername(token);

            // load db object for authentication
            UserDetails foundUser = myUserDetailsService.loadUserByUsername(username);

            // verify that authorizations on the user
            Authentication authenticated = new UsernamePasswordAuthenticationToken(
                    foundUser, null, foundUser.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(authenticated);
        }

        // continue the filter chain
        filterChain.doFilter(request,response);
    }


    private String getTokenFromHeader(HttpServletRequest request) {
        String BearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(BearerToken) && BearerToken.startsWith("Bearer ")) {
            String tokenVal = BearerToken.substring(7,BearerToken.length());
            return tokenVal;
        }

        return null;
    }
}
