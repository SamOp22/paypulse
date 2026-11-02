
package com.paypulse.payment.auth.service;

import com.paypulse.payment.merchant.entity.Merchant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final Duration accessTokenLifetime;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.access-token-minutes}") long minutes) {

        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.accessTokenLifetime = Duration.ofMinutes(minutes);
    }

    public String generateAccessToken(Merchant merchant) {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(merchant.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenLifetime))
                .claim("role", merchant.getRole().name())
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }

    public long getAccessTokenLifetimeSeconds() {
        return accessTokenLifetime.toSeconds();
    }
}
