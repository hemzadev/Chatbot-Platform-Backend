package com.hmzadev.interactivechatbot.services;

import com.hmzadev.interactivechatbot.configuration.JwtService;
import com.hmzadev.interactivechatbot.dao.*;
import com.hmzadev.interactivechatbot.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
        logger.info("Registering new user with username: {}", request.getUsername());

        var user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        userRepository.save(user);

        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user); // Generate refresh token

        logger.info("User registered successfully with username: {}", request.getUsername());
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken) // Include refresh token in response
                .build();
    }

    public AuthenticationResponse login(AuthenticationRequest request) {
        logger.info("Attempting login for username: {}", request.getUsername());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (Exception e) {
            logger.error("Authentication failed for username: {}", request.getUsername(), e);
            throw new RuntimeException("Invalid username or password");
        }

        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    logger.error("User not found for username: {}", request.getUsername());
                    return new RuntimeException("User not found");
                });

        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        logger.info("User logged in successfully with username: {}", request.getUsername());
        return new AuthenticationResponse(jwtToken, refreshToken);
    }

    public AuthenticationResponse refreshToken(String refreshToken) {
        logger.info("Attempting to refresh token");

        // Validate the refresh token
        if (jwtService.isRefreshTokenValid(refreshToken)) {
            // Extract the username from the refresh token
            String username = jwtService.extractUsername(refreshToken);

            // Load user details
            var user = userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        logger.error("User not found for username: {}", username);
                        return new RuntimeException("User not found");
                    });

            // Generate new access token
            String newAccessToken = jwtService.generateToken(user);

            // Optionally generate a new refresh token (for rolling refresh tokens)
            String newRefreshToken = jwtService.generateRefreshToken(user);

            logger.info("Successfully refreshed tokens for user: {}", username);

            // Return the new tokens
            return AuthenticationResponse.builder()
                    .token(newAccessToken)
                    .refreshToken(newRefreshToken) // Include the new refresh token if needed
                    .build();
        } else {
            logger.error("Invalid refresh token");
            throw new RuntimeException("Invalid refresh token");
        }
    }


}
