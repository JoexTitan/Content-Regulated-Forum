package com.springboot.blog.jwt;

import com.springboot.blog.exception.BlogAPIException;
import com.springboot.blog.utils.AppEnums.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {
    @Value("${app.jwt-secret}")
    private String jwtSecret;
    @Value("${app.jwt-expiration-milliseconds}")
    private String jwtExpiration;

    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Instant currentDate = Instant.now();
        Instant expireDate = currentDate.plusMillis(Long.parseLong(jwtExpiration));

        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(Date.from(currentDate))
                .setExpiration(Date.from(expireDate))
                .signWith(secretKey())
                .compact();

        return token;
    }

    private Key secretKey() {
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(jwtSecret)
        );
    }

    public String extractUsername(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        String username = claims.getSubject();
        return username;
    }

    public boolean validateToken(String token) {
        try {
            Jwts
                    .parser()
                    .setSigningKey(secretKey())
                    .build()
                    .parse(token);

            return true;

        } catch (ExpiredJwtException e) {
            // Handle token expiration
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The token is expired", ErrorCode.INVALID_JWT_TOKEN);
        } catch (UnsupportedJwtException e) {
            // Handle token Unsupported Jwt Exception
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The token is Unsupported", ErrorCode.INVALID_JWT_TOKEN);
        } catch (MalformedJwtException e) {
            // Handle malformed JWT
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The token is malformed", ErrorCode.INVALID_JWT_TOKEN);
        } catch (SignatureException e) {
            // Handle signature-related issues
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The token is not signed", ErrorCode.INVALID_JWT_TOKEN);
        } catch (IllegalArgumentException e) {
            // Handle illegal argument
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The token is illegal", ErrorCode.INVALID_JWT_TOKEN);
        }
    }
    public String getTokenFromHeader(HttpServletRequest request) {
        String BearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(BearerToken) && BearerToken.startsWith("Bearer ")) {
            String tokenVal = BearerToken.substring(7,BearerToken.length());
            return tokenVal;
        }
        return null;
    }
}
