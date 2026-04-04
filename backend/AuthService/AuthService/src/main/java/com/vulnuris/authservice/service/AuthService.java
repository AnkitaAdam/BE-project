package com.vulnuris.authservice.service;

import com.vulnuris.authservice.dto.request.LoginRequest;
import com.vulnuris.authservice.dto.request.RefreshTokenRequest;
import com.vulnuris.authservice.dto.request.RegisterRequest;
import com.vulnuris.authservice.dto.response.AuthTokensResponse;
import com.vulnuris.authservice.dto.response.UserResponse;

public interface AuthService {

    AuthTokensResponse register(RegisterRequest request);

    AuthTokensResponse login(LoginRequest request);

    AuthTokensResponse refresh(RefreshTokenRequest request);

    UserResponse me(String username);
}
