package com.vulnuris.authservice.security;

import com.vulnuris.authservice.entity.User;
import com.vulnuris.authservice.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(user.getRoles().stream()
                        .map(role -> "ROLE_" + role.getName().name())
                        .toArray(String[]::new))
                .disabled(!user.isEnabled())
                .build();
    }
}
