package com.yawar.nextforgeai.security;

import com.yawar.nextforgeai.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;


@Component
public class JwtService {

    @Value("${jwt.secret-key}")
    private String SECRET_KEY;

    private SecretKey getSecretKey(){
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }


    public String generateAccessToken(Map<String, Object> extraClaims,
                                CustomUserDetail userDetails) {
        return Jwts.builder()
                .claims(extraClaims == null ? Map.of() : extraClaims)
                .subject(userDetails.getUser().getId())
                .claim("email",userDetails.getUser().getEmail())
                .claim("isActive",userDetails.getUser().isActive())
                .claim("isEmailVerified",userDetails.getUser().isEmailVerified())
                .claim("username",userDetails.getUser().getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(getSecretKey())
                .compact();
    }

    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUsername(String token){
        return extractClaim(token,claims -> claims.get("username",String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token,
                              Function<Claims, T> resolver) {
        final Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean isTokenValid(String token,
                                CustomUserDetail userDetails) {

        final String userId = extractUserId(token);

        return userId.equals(userDetails.getUser().getId())
                && !isTokenExpired(token);
    }

    public String getLoggedInUserId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetail userDetail)){
            throw new AuthenticationCredentialsNotFoundException("Jwt Not Found");
        }
        return userDetail.getUser().getId();
    }

    public String generateRefreshToken(User user){

        return Jwts.builder()
                .subject(user.getId())
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + 1000L * 60 * 60 * 24 * 30
                        )
                )
                .signWith(getSecretKey())
                .compact();
    }
}
