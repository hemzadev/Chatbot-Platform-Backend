package com.hmzadev.interactivechatbot.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret.key}")
    private String jwtSecret;

    @Value("${jwt.refresh.secret.key}")
    private String refreshSecretKey;

    // Consistent key decoding and retrieval for access tokens
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret); // Decode the secret key
        Key key = Keys.hmacShaKeyFor(keyBytes); // Use HMAC with SHA-256
        logKeyDetails("Access", jwtSecret, key);
        return key;
    }

    // Consistent key decoding and retrieval for refresh tokens
    private Key getRefreshSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(refreshSecretKey); // Decode the refresh secret key
        Key key = Keys.hmacShaKeyFor(keyBytes); // Use HMAC with SHA-256
        logKeyDetails("Refresh", refreshSecretKey, key);
        return key;
    }

    private void logKeyDetails(String type, String encodedKey, Key key) {
        System.out.println(type + " Secret Key (Base64 Encoded): " + encodedKey);
        System.out.println(type + " Signing Key: " + key);
    }

    // Extract username (subject) from the token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Helper method to extract username from the token in the request
    public String getUserUsernameFromToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request); // Extract token from request headers
        if (token != null && !token.isEmpty()) {
            return extractUsername(token); // Use the extractUsername method
        }
        return null;
    }

    // This method assumes you have a Bearer token in the Authorization header
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Remove "Bearer " prefix
        }
        return null;
    }

    // Extract a specific claim from the token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token, false); // Pass false for access tokens
        return claimsResolver.apply(claims);
    }

    // Generate Access Token with consistent signing key
    public String generateToken(UserDetails userDetails) {
        return generateToken(Map.of(), userDetails);
    }

    // Generate token with extra claims and user details
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        String token = Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername()) // The username is the subject
                .setIssuedAt(new Date(System.currentTimeMillis())) // Issue date
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 15)) // 15 minutes expiration
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // Sign with the access key
                .compact();
        logTokenDetails("Access", token);
        return token;
    }

    // Generate Refresh Token
    public String generateRefreshToken(UserDetails userDetails) {
        String token = Jwts.builder()
                .setSubject(userDetails.getUsername()) // The username is the subject
                .setIssuedAt(new Date(System.currentTimeMillis())) // Issue date
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7)) // 7 days expiration
                .signWith(getRefreshSigningKey(), SignatureAlgorithm.HS256) // Sign with the refresh key
                .compact();
        logTokenDetails("Refresh", token);
        return token;
    }

    private void logTokenDetails(String type, String token) {
        System.out.println(type + " Token: " + token);
    }

    // Validate the Access Token
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Key signingKey = getSigningKey();
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);
            String username = jws.getBody().getSubject();
            boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);
            logTokenValidation("Access", token, isValid);
            return isValid;
        } catch (Exception e) {
            logTokenValidationError("Access", token, e);
            return false;
        }
    }

    // Validate Refresh Token
    public boolean isRefreshTokenValid(String token) {
        try {
            Key signingKey = getRefreshSigningKey();
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            logTokenValidation("Refresh", token, true);
            return true;
        } catch (Exception e) {
            logTokenValidationError("Refresh", token, e);
            return false;
        }
    }

    private void logTokenValidation(String type, String token, boolean isValid) {
        System.out.println(type + " Token Validation: " + (isValid ? "Valid" : "Invalid"));
        System.out.println(type + " Token: " + token);
    }

    private void logTokenValidationError(String type, String token, Exception e) {
        System.out.println("Invalid " + type + " token: " + e.getMessage());
        System.out.println(type + " Token: " + token);
    }

    // Check if the token has expired
    private boolean isTokenExpired(String token) {
        boolean isExpired = extractExpiration(token).before(new Date());
        System.out.println("Token Expired: " + isExpired);
        return isExpired;
    }

    // Extract expiration date from the token
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Extract all claims from the token
    public Claims extractAllClaims(String token, boolean isRefreshToken) {
        Key signingKey = isRefreshToken ? getRefreshSigningKey() : getSigningKey();
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        logClaims("Access", claims); // For access tokens
        if (isRefreshToken) {
            logClaims("Refresh", claims); // For refresh tokens
        }
        return claims;
    }

    private void logClaims(String type, Claims claims) {
        System.out.println(type + " Token Claims: " + claims);
    }

    // Optional: Check for clock skew (if needed for strict time checks)
    public boolean checkClockSkew(String token) {
        long expirationTime = extractExpiration(token).getTime();
        long currentTime = System.currentTimeMillis();
        long skew = Math.abs(expirationTime - currentTime);
        boolean hasSkew = skew > 300000; // 5 minutes skew tolerance
        System.out.println("Clock Skew Detected: " + hasSkew);
        return hasSkew;
    }
}
