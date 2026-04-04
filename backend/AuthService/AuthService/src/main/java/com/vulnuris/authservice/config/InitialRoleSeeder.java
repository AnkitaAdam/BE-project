package com.vulnuris.authservice.config;

import com.vulnuris.authservice.entity.Role;
import com.vulnuris.authservice.entity.RoleName;
import com.vulnuris.authservice.entity.User;
import com.vulnuris.authservice.repository.RoleRepository;
import com.vulnuris.authservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InitialRoleSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${security.bootstrap-admin.enabled:false}")
    private boolean bootstrapAdminEnabled;

    @Value("${security.bootstrap-admin.username:admin}")
    private String bootstrapAdminUsername;

    @Value("${security.bootstrap-admin.email:admin@vulnuris.local}")
    private String bootstrapAdminEmail;

    @Value("${security.bootstrap-admin.password:ChangeThisAdminPassword123!}")
    private String bootstrapAdminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureRoles();
        ensureBootstrapAdmin();
    }

    private void ensureRoles() {
        Map<RoleName, String> roleDescriptions = Map.of(
                RoleName.ADMIN, "Platform administrator with full privileges",
                RoleName.ANALYST, "Security analyst with operational access",
                RoleName.VIEWER, "Read-only access to dashboards and reports"
        );

        roleDescriptions.forEach((name, description) -> roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(Role.builder().name(name).description(description).build())));
    }

    private void ensureBootstrapAdmin() {
        if (!bootstrapAdminEnabled) {
            return;
        }

        if (bootstrapAdminPassword == null || bootstrapAdminPassword.length() < 12) {
            log.warn("Bootstrap admin is enabled but password does not meet minimum length of 12 characters");
            return;
        }

        String username = normalize(bootstrapAdminUsername);
        String email = normalize(bootstrapAdminEmail);

        if (userRepository.existsByUsername(username)) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Role ADMIN is not configured"));

        User adminUser = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(bootstrapAdminPassword))
                .enabled(true)
                .roles(Set.of(adminRole))
                .build();

        userRepository.save(adminUser);
        log.info("Bootstrap admin user created with username={}", username);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
