package br.com.gestorfinanceiro.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    public String generateToken(String userID, String username, String email, String role, String estaAtivo) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("estaAtivo", estaAtivo);
        claims.put("role", role);
        claims.put("id", userID);
        claims.put("email", email);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUserRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractUserEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("id", String.class));
    }

    public boolean validateToken(String token, String email) {
        String extractUserEmail = extractUserEmail(token);
        boolean isExpired = isTokenExpired(token);

        return (extractUserEmail.equals(email) && !isExpired);
    }

    public boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }
}