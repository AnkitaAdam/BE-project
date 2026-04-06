package com.vulnuris.authservice.service.impl;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.vulnuris.authservice.dto.request.LoginRequest;
import com.vulnuris.authservice.dto.request.RefreshTokenRequest;
import com.vulnuris.authservice.dto.request.RegisterRequest;
import com.vulnuris.authservice.dto.response.AuthTokensResponse;
import com.vulnuris.authservice.dto.response.UserResponse;
import com.vulnuris.authservice.entity.Role;
import com.vulnuris.authservice.entity.RoleName;
import com.vulnuris.authservice.entity.User;
import com.vulnuris.authservice.exception.BadRequestException;
import com.vulnuris.authservice.exception.ConflictException;
import com.vulnuris.authservice.exception.InvalidTokenException;
import com.vulnuris.authservice.exception.ResourceNotFoundException;
import com.vulnuris.authservice.repository.RoleRepository;
import com.vulnuris.authservice.repository.UserRepository;
import com.vulnuris.authservice.security.JwtProperties;
import com.vulnuris.authservice.security.JwtTokenProvider;
import com.vulnuris.authservice.service.AuthService;
import com.vulnuris.authservice.service.model.TokenPair;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @Override
    public AuthTokensResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        String email = normalizeEmail(request.email());

        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username is already in use");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already in use");
        }

        Role defaultRole = roleRepository.findByName(RoleName.VIEWER)
                .orElseThrow(() -> new IllegalStateException("Role VIEWER is not configured"));

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .enabled(true)
                .roles(Set.of(defaultRole))
                .build();

        User savedUser = userRepository.save(user);
        return toAuthResponse(savedUser);
    }

    @Override
    public AuthTokensResponse login(LoginRequest request) {
        String username = normalizeUsername(request.username());

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, request.password()));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.isEnabled()) {
            throw new DisabledException("User account is disabled");
        }

        return toAuthResponse(user);
    }

    @Override
    public AuthTokensResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken().trim();

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException("Provided token is not a valid refresh token");
        }

        String username = jwtTokenProvider.getUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidTokenException("Refresh token subject does not exist"));

        if (!user.isEnabled()) {
            throw new DisabledException("User account is disabled");
        }

        return toAuthResponse(user);
    }

    @Override
    @Transactional
    public UserResponse me(String username) {
        String normalizedUsername = normalizeUsername(username);
        User user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found"));

        return toUserResponse(user);
    }

    @Override
    public UserResponse updateUserRoles(Long userId, Set<String> roles) {
        if (userId == null || userId <= 0) {
            throw new BadRequestException("User ID must be a positive number");
        }

        Set<RoleName> requestedRoles = parseRoleNames(roles);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setRoles(resolveRoles(requestedRoles));
        User updatedUser = userRepository.save(user);
        return toUserResponse(updatedUser);
    }

    private AuthTokensResponse toAuthResponse(User user) {
        TokenPair tokenPair = new TokenPair(
                jwtTokenProvider.generateAccessToken(user),
                jwtTokenProvider.generateRefreshToken(user)
        );

        return new AuthTokensResponse(
                "Bearer",
                tokenPair.accessToken(),
                jwtProperties.getAccessTokenValiditySeconds(),
                tokenPair.refreshToken(),
                jwtProperties.getRefreshTokenValiditySeconds(),
                toUserResponse(user)
        );
    }

    private UserResponse toUserResponse(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roles,
                user.isEnabled(),
                user.getCreatedAt()
        );
    }

    private Set<RoleName> parseRoleNames(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new BadRequestException("At least one role is required");
        }

        Set<RoleName> parsedRoles = new LinkedHashSet<>();
        for (String role : roles) {
            if (role == null || role.isBlank()) {
                throw new BadRequestException("Role values must not be blank");
            }

            String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
            try {
                parsedRoles.add(RoleName.valueOf(normalizedRole));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Unsupported role: " + normalizedRole
                        + ". Allowed roles: " + allowedRolesDisplay());
            }
        }

        return parsedRoles;
    }

    private Set<Role> resolveRoles(Set<RoleName> roleNames) {
        return roleNames.stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new IllegalStateException("Role " + roleName + " is not configured")))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String allowedRolesDisplay() {
        return Arrays.stream(RoleName.values())
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private String normalizeUsername(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
