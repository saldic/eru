package app.security;

import app.entities.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class JwtUtil {

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final String issuer;
    private final long expiryMinutes;
    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    public JwtUtil(String secret) {
        this(secret, "eru-api", 60);
    }

    public JwtUtil(String secret, String issuer, long expiryMinutes) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.expiryMinutes = expiryMinutes;
        this.verifier = JWT.require(algorithm).withIssuer(issuer).build();
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiryMinutes, ChronoUnit.MINUTES);
        Set<String> roles = new LinkedHashSet<>(user.getRolesAsStrings().stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet()));

        return JWT.create()
                .withIssuer(issuer)
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .withSubject(user.getUsername())
                .withClaim("userId", user.getId())
                .withArrayClaim("roles", roles.toArray(new String[0]))
                .sign(algorithm);
    }

    public DecodedJWT verifyToken(String token) {
        cleanupRevokedTokens();
        DecodedJWT decodedJWT = verifier.verify(token);
        Instant revokedUntil = revokedTokens.get(token);
        if (revokedUntil != null && revokedUntil.isAfter(Instant.now())) {
            throw new JWTVerificationException("Token has been revoked");
        }
        return decodedJWT;
    }

    public void revokeToken(String token) {
        cleanupRevokedTokens();
        DecodedJWT decodedJWT = verifier.verify(token);
        Instant expiresAt = decodedJWT.getExpiresAtAsInstant();
        if (expiresAt != null && expiresAt.isAfter(Instant.now())) {
            revokedTokens.put(token, expiresAt);
        }
    }

    private void cleanupRevokedTokens() {
        Instant now = Instant.now();
        revokedTokens.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
    }
}
