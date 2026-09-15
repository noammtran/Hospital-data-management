package com.usth.hospital.security;

import com.usth.hospital.config.JwtProperties;
import com.usth.hospital.dto.response.AuthTokenResponse;
import com.usth.hospital.model.AppUserEntity;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private static final String TOKEN_TYPE = "Bearer";
    public static final String ISSUER = "hospital-api";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public AuthTokenResponse issue(AppUserEntity user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwtProperties.accessTokenTtl());
        List<String> permissions = DatabaseUserDetailsService.effectivePermissions(user).stream()
                .map(Enum::name)
                .sorted()
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.getUsername())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("token_version", user.getTokenVersion())
                .claim("role", user.getRole().name())
                .claim("permissions", permissions)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new AuthTokenResponse(token, TOKEN_TYPE, jwtProperties.accessTokenTtl().toSeconds());
    }
}
