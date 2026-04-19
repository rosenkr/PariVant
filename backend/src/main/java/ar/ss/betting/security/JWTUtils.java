package ar.ss.betting.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JWTUtils {

    private static final Date EXPIRATION_TIME = new Date(60 * 1000 * 60); // 1 hour
    String secret = "shouldBeInRailway";
    SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

    public static String generateToken() {
        return Jwts.builder().setSubject(username).setIssuedAt(new Date()).setExpiration(EXPIRATION_TIME).signWith(key,signatureAlgorithm).compact();
    }

    public static boolean validateToken() {

    }

    public static String extractUsername(String part) {
        Claims = Jwts.parser().

    }
}
