package com.yawar.nextforgeai.service.impl;

import com.yawar.nextforgeai.dto.*;
import com.yawar.nextforgeai.entity.*;
import com.yawar.nextforgeai.entity.enums.Provider;
import com.yawar.nextforgeai.entity.enums.SubscriptionStatus;
import com.yawar.nextforgeai.error.BadRequestException;
import com.yawar.nextforgeai.error.ResourceNotFoundException;
import com.yawar.nextforgeai.repository.*;
import com.yawar.nextforgeai.security.CustomUserDetail;
import com.yawar.nextforgeai.security.JwtService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import com.yawar.nextforgeai.service.*;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements com.yawar.nextforgeai.service.AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long PASSWORD_RESET_TOKEN_EXPIRY =
            Duration.ofMinutes(10).toMillis();
    private final SessionService sessionService;
    private final UserSessionRepository userSessionRepository;

    @Value("${frontend.url}")
    private String FRONTEND_URL;



    @Transactional
    @Override
    public RegisterResponse register(RegisterRequest request)  {

        log.info("Registration request received for email={}", request.getEmail());

        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());

        User user;

        if (optionalUser.isPresent()) {

            user = optionalUser.get();

            if (user.isActive() || user.isDeleted()) {
                log.warn("Registration failed. User already exists. email={}", request.getEmail());
                throw new BadRequestException("Already Register");
            }

            log.info("Inactive user found. Reusing existing account. userId={}", user.getId());

        } else {

            log.info("Creating new user. email={}", request.getEmail());

            String generatedUsername = generateUniqueUsername(request.getEmail());

            user = User.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .username(generatedUsername)
                    .password(passwordEncoder.encode(request.getPassword()))
                    .provider(Provider.LOCAL)
                    .isActive(false)
                    .isEmailVerified(false)
                    .isDeleted(false)
                    .build();

            user = userRepository.save(user);

            log.info("User created successfully. userId={}", user.getId());
        }

        log.debug("Generating OTP for userId={}", user.getId());

        String otp = generateRandomOtp();

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .email(user.getEmail())
                .otp(otp)
                .expiresAt(System.currentTimeMillis() + (1000 * 60 * 5))
                .build();

        tokenRepository.save(verificationToken);

        log.info("OTP generated and stored successfully. userId={}", user.getId());

        log.info("Sending OTP email to {}", user.getEmail());

        emailService.sendOtpEmail(
                user.getEmail(),
                otp
        );

        log.info("OTP email sent successfully to {}", user.getEmail());

        log.info("Registration completed successfully. userId={}", user.getId());

        return new RegisterResponse(user.getId());
    }

    private String generateUniqueUsername(String email) {

        String baseUsername = email.substring(0, email.indexOf("@"))
                .replaceAll("[^a-zA-Z0-9_]", "");

        String username = baseUsername;
        int counter = 1;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }

    private String generateRandomOtp() {
        return String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
    }

    @Transactional
    @Override
    public AuthResponse verifyOtpAndFilnalizeRegister(String userId, OtpRequest request, HttpServletRequest httpServletRequest)  {

        log.info("OTP verification started. userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.isDeleted()) {
            log.warn("OTP verification failed. Deleted user. userId={}", userId);
            throw new BadRequestException("Invalid User.");
        }

        if (user.isActive()) {
            log.warn("OTP verification skipped. User already verified. userId={}", userId);
            throw new BadRequestException("User already registered.");
        }

        EmailVerificationToken verificationToken = tokenRepository
                .findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(user.getEmail())
                .orElseThrow(() -> new BadRequestException("No active verification token found."));

        if (!verificationToken.getOtp().equals(request.getOtp())) {
            log.warn("Invalid OTP entered. userId={}", userId);
            throw new BadRequestException("Invalid OTP.");
        }

        if (System.currentTimeMillis() > verificationToken.getExpiresAt()) {
            log.warn("OTP expired. userId={}", userId);
            throw new BadRequestException("OTP has expired.");
        }

        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);

        log.info("OTP verified successfully. userId={}", userId);

        user.setActive(true);
        user.setEmailVerified(true);
        user = userRepository.save(user);

        log.info("User account activated. userId={}", userId);

        Plan freePlan = planRepository.findByName("FREE")
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "FREE"));

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(freePlan)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(Instant.now())
                .currentPeriodEnd(null)
                .stripeSubscriptionId(null)
                .build();

        subscriptionRepository.save(subscription);

        emailService.sendRegisterSuccessfulEmail(user.getEmail());

        log.info("Free subscription assigned. userId={}, plan={}",
                user.getId(),
                freePlan.getName());

        String accessToken = jwtService.generateAccessToken(null,new CustomUserDetail(user));

        String refreshToken =
                sessionService.createSession(
                        user,
                        httpServletRequest.getRemoteAddr(),
                        httpServletRequest.getHeader("User-Agent"),
                        null
                );

        emailService.sendMailToOwner(
                "New User Registered",
                "USER_REGISTERED",
                "A new user has successfully registered on NextForge AI.",
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getId().toString(),
                "Registration completed successfully."
        );

        return AuthResponse.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }


    @Override
    public AuthResponse login(LoginRequest request, HttpServletRequest httpServletRequest) {

        log.info("Login attempt received. email={}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        if (!authentication.isAuthenticated()) {
            log.warn("Login failed. Invalid credentials. email={}", request.getEmail());
            throw new BadRequestException("Invalid email or password.");
        }

        CustomUserDetail userDetails = (CustomUserDetail) authentication.getPrincipal();

        log.info("User authenticated successfully. userId={}", userDetails.getUser().getId());

        log.info("Access token generated successfully. userId={}", userDetails.getUser().getId());

        String accessToken = jwtService.generateAccessToken(null,userDetails);

        String refreshToken =
                sessionService.createSession(
                        userDetails.getUser(),
                        httpServletRequest.getRemoteAddr(),
                        httpServletRequest.getHeader("User-Agent"),
                        null
                );

        emailService.sendMailToOwner(
                "User Logged In",
                "USER_LOGIN",
                "A user has successfully logged into NextForge AI.",
                userDetails.getUser().getName(),
                userDetails.getUsername(),
                userDetails.getUser().getEmail(),
                userDetails.getUser().getId(),
                "User successfully authenticated."
        );

        return AuthResponse.builder()
                .userId(userDetails.getUser().getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(userDetails.getUsername())
                .email(userDetails.getUser().getEmail())
                .name(userDetails.getUser().getName())
                .build();
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request)  {

        log.info("Password reset requested for email={}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getEmail()));

        if (!user.isActive() || user.isDeleted()) {
            log.warn("Password reset rejected. Account inactive or deleted. userId={}", user.getId());
            throw new BadRequestException("Account is inactive or deleted.");
        }

        String resetToken = UUID.randomUUID().toString() + UUID.randomUUID().toString() + UUID.randomUUID().toString();

        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(resetToken)
                .expiresAt(System.currentTimeMillis() + PASSWORD_RESET_TOKEN_EXPIRY)
                .build();

        passwordResetTokenRepository.save(token);

        log.info("Password reset token generated successfully. userId={}", user.getId());

        String resetLink = UriComponentsBuilder.fromUriString(FRONTEND_URL)
                .path("/reset-password")
                .queryParam("token", resetToken)
                .toUriString();

        log.info("Sending password reset email. userId={}", user.getId());

        emailService.sendPasswordResetEmail(
                user.getEmail(),
                resetLink
        );

        log.info("Password reset email sent successfully. userId={}", user.getId());

        emailService.sendMailToOwner(
                "Password Reset Requested",
                "PASSWORD_RESET_REQUESTED",
                "A user has requested a password reset on NextForge AI.",
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getId().toString(),
                "A password reset request was initiated."
        );
    }
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request)  {

        log.info("Password reset request received.");

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> {
                    log.warn("Password reset failed. Invalid token.");
                    return new BadRequestException("Invalid password reset token.");
                });

        if (resetToken.isUsed()) {
            log.warn("Password reset failed. Token already used. userId={}",
                    resetToken.getUser().getId());
            throw new BadRequestException("This password reset token has already been used.");
        }

        if (System.currentTimeMillis() > resetToken.getExpiresAt()) {
            log.warn("Password reset failed. Token expired. userId={}",
                    resetToken.getUser().getId());
            throw new BadRequestException("Password reset token has expired.");
        }

        User user = resetToken.getUser();

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset token validated successfully. userId={}", user.getId());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password updated successfully. userId={}", user.getId());

        emailService.sendPasswordResetSuccessfulEmail(user.getEmail());

        log.info("Password reset completed successfully. userId={}", user.getId());

        emailService.sendMailToOwner(
                "Password Successfully Reset",
                "PASSWORD_RESET_SUCCESS",
                "A user has successfully reset their password on NextForge AI.",
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getId().toString(),
                "The user's password was successfully updated."
        );
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {

        log.info("Refresh token request received.");

        // Validate JWT signature & format
        try {
            jwtService.extractUserId(refreshToken);
        } catch (Exception ex) {
            log.warn("Invalid refresh token received.");
            throw new BadRequestException("Invalid refresh token.");
        }

        UserSession session = userSessionRepository
                .findByRefreshTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found in database.");
                    return new BadRequestException("Invalid refresh token.");
                });

        if (session.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Refresh token expired. userId={}", session.getUser().getId());
            throw new BadRequestException("Refresh token has expired.");
        }

        User user = session.getUser();

        // Extra security check
        String tokenUserId = jwtService.extractUserId(refreshToken);
        if (!user.getId().equals(tokenUserId)) {
            log.warn("Refresh token user mismatch.");
            throw new BadRequestException("Invalid refresh token.");
        }

        CustomUserDetail userDetails = new CustomUserDetail(user);

        String newAccessToken = jwtService.generateAccessToken(
                null,
                userDetails
        );

        String newRefreshToken = jwtService.generateRefreshToken(user);

        // Refresh Token Rotation
        session.setRefreshToken(newRefreshToken);
        session.setExpiresAt(
                Instant.now().plus(30, ChronoUnit.DAYS)
        );

        userSessionRepository.save(session);

        log.info("Refresh token rotated successfully. userId={}", user.getId());

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {

        log.info("Logout request received.");

        UserSession session = userSessionRepository
                .findByRefreshTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token."));

        session.setRevoked(true);

        userSessionRepository.save(session);

        log.info("Logout successful. userId={}", session.getUser().getId());
    }
}