package com.usth.hospital.security;

import com.usth.hospital.model.AppUserEntity;
import com.usth.hospital.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import java.util.Objects;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DatabaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AppUserRepository appUserRepository;

    @Override
    @Transactional(readOnly = true)
    public AbstractAuthenticationToken convert(Jwt jwt) {
        AppUserEntity user = appUserRepository.findByUsernameIgnoreCase(jwt.getSubject())
                .filter(AppUserEntity::isActive)
                .orElseThrow(() -> new BadCredentialsException("Account is unavailable"));
        Number issuedTokenVersion = jwt.getClaim("token_version");
        if (issuedTokenVersion == null
                || !Objects.equals(user.getTokenVersion(), issuedTokenVersion.longValue())) {
            throw new BadCredentialsException("Token is no longer valid");
        }
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                jwt,
                DatabaseUserDetailsService.authorities(user),
                user.getUsername()
        );
        authentication.setDetails(new CurrentUserSnapshot(
                user.getUsername(),
                user.getFullName(),
                user.getRole().name()
        ));
        return authentication;
    }
}
