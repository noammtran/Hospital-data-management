package com.usth.hospital.security;

import com.usth.hospital.model.AppUserEntity;
import com.usth.hospital.repository.AppUserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUserEntity user = appUserRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password"));

        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities(user))
                .disabled(!user.isActive())
                .build();
    }

    static List<GrantedAuthority> authorities(AppUserEntity user) {
        Set<AppPermission> permissions = effectivePermissions(user);
        List<GrantedAuthority> authorities = new ArrayList<>(permissions.stream()
                .map(Enum::name)
                .map(SimpleGrantedAuthority::new)
                .toList());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        return authorities;
    }

    static Set<AppPermission> effectivePermissions(AppUserEntity user) {
        if (user.isPasswordChangeRequired()) {
            return Set.of(AppPermission.CHANGE_OWN_PASSWORD);
        }
        return user.getRole().permissions();
    }
}
