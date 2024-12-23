package com.hmzadev.interactivechatbot.web;

import com.hmzadev.interactivechatbot.dao.AuthenticationRequest;
import com.hmzadev.interactivechatbot.dao.AuthenticationResponse;
import com.hmzadev.interactivechatbot.dao.RegisterRequest;
import com.hmzadev.interactivechatbot.services.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            @RequestBody Map<String, String> refreshTokenRequest) {
        try {
            // Extract the refresh token from the request map
            String refreshToken = refreshTokenRequest.get("refreshToken");

            if (refreshToken == null || refreshToken.isEmpty()) {
                // If no refresh token provided, return a bad request response
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(null);
            }

            // Call the service to refresh the token
            AuthenticationResponse response = authenticationService.refreshToken(refreshToken);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log the exception and return a server error response
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
