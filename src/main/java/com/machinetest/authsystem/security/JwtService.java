package com.machinetest.authsystem.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.machinetest.authsystem.config.AppProperties;
import com.machinetest.authsystem.user.User;
import com.machinetest.authsystem.user.UserType;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final String USER_TYPE_CLAIM = "userType";
    private static final String EMAIL_CLAIM = "email";

    private final SecretKey signingKey;
    private final AppProperties.Jwt properties;
    private final Clock clock;
    private final JwtParser parser;

    public JwtService(AppProperties appProperties, Clock clock) {
        this.properties = appProperties.jwt();
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.parser = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    public IssuedToken generateToken(User user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.expiration());
        String tokenId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(tokenId)
                .issuer(properties.issuer())
                .subject(user.username())
                .claim(USER_TYPE_CLAIM, user.userType().name())
                .claim(EMAIL_CLAIM, user.email())
                .issuedAt(Date.from(issuedAt))
                .notBefore(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
        return new IssuedToken(token, tokenId, issuedAt, expiresAt);
    }

    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            UserType userType = UserType.fromValue(claims.get(USER_TYPE_CLAIM, String.class));
            if (claims.getSubject() == null || claims.getId() == null || userType == null || claims.getExpiration() == null) {
                return Optional.empty();
            }
            return Optional.of(new AuthenticatedUser(claims.getSubject(), userType, claims.getId(),
                    claims.getExpiration().toInstant()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public record IssuedToken(String token, String tokenId, Instant issuedAt, Instant expiresAt) {
    }
}
