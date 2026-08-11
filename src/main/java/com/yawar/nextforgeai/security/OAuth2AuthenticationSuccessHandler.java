package com.yawar.nextforgeai.security;

import com.yawar.nextforgeai.entity.Plan;
import com.yawar.nextforgeai.entity.Subscription;
import com.yawar.nextforgeai.entity.User;
import com.yawar.nextforgeai.entity.UserSession;
import com.yawar.nextforgeai.entity.enums.Provider;
import com.yawar.nextforgeai.entity.enums.SubscriptionStatus;
import com.yawar.nextforgeai.error.BadRequestException;
import com.yawar.nextforgeai.repository.PlanRepository;
import com.yawar.nextforgeai.repository.SubscriptionRepository;
import com.yawar.nextforgeai.repository.UserRepository;
import com.yawar.nextforgeai.repository.UserSessionRepository;
import com.yawar.nextforgeai.service.EmailService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${frontend.url}")
    private String FRONTEND_URL;

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserSessionRepository userSessionRepository;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {    log.info("========== Inside CustomOAuth2UserService ==========");

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        User user = getOrCreateGoogleUser(oauthUser);

        String accessToken = createAccessToken(user);

        String refreshToken = createRefreshToken(user);

        saveUserSession(user, refreshToken, request);

        addRefreshCookie(response, refreshToken);

        redirectToFrontend(response, accessToken);

        emailService.sendMailToOwner(
                "User Logged In",
                "USER_LOGIN",
                "A user has successfully logged into NextForge AI.",
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getId().toString(),
                "Login method: Google OAuth2"
        );
    }

    private User getOrCreateGoogleUser(OAuth2User oauthUser) {

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String picture = oauthUser.getAttribute("picture");
        String googleId = oauthUser.getAttribute("sub");

        log.info("Processing Google user. email={}", email);

        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user != null) {

            log.info("Existing user found. userId={}", user.getId());

            if (user.isDeleted()) {
                throw new BadRequestException("This account has been deleted.");
            }

            // Link Google account if not already linked
            if (user.getProvider() != Provider.GOOGLE) {

                log.info("Linking existing account with Google. userId={}", user.getId());

                user.setProvider(Provider.GOOGLE);
                user.setProviderUserId(googleId);
            }

            user.setName(name);
            user.setProfilePicture(picture);
            user.setActive(true);
            user.setEmailVerified(true);

            return userRepository.save(user);
        }

        log.info("No existing user found. Creating new Google account.");

        String username = generateUniqueUsername(email);

        user = User.builder()
                .email(email)
                .name(name)
                .username(username)
                .provider(Provider.GOOGLE)
                .providerUserId(googleId)
                .profilePicture(picture)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build();

        user = userRepository.save(user);

        createFreeSubscription(user);

        log.info("Google user created successfully. userId={}", user.getId());

        return user;
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

    private void createFreeSubscription(User user) {

        Plan plan = planRepository.findByName("FREE")
                .orElse(null);

        if (plan == null) {
            log.warn("FREE plan not found. Skipping subscription creation.");
            return;
        }

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(Instant.now())
                .currentPeriodEnd(null)
                .cancelAtPeriodEnd(false)
                .build();

        subscriptionRepository.save(subscription);

        log.info("FREE subscription created. userId={}", user.getId());
    }

    private String createAccessToken(User user) {

        log.info("Generating access token. userId={}", user.getId());

        CustomUserDetail userDetail = new CustomUserDetail(user);

        String accessToken = jwtService.generateAccessToken(
                null,
                userDetail
        );

        log.info("Access token generated successfully. userId={}", user.getId());

        return accessToken;
    }

    private String createRefreshToken(User user) {

        log.info("Generating refresh token. userId={}", user.getId());

        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("Refresh token generated successfully. userId={}", user.getId());

        return refreshToken;
    }

    private void saveUserSession(
            User user,
            String refreshToken,
            HttpServletRequest request
    ) {

        log.info("Creating user session. userId={}", user.getId());

        UserSession session = UserSession.builder()
                .user(user)
                .refreshToken(refreshToken)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .revoked(false)
                .ipAddress(request.getRemoteAddr())
                .userAgent(request.getHeader("User-Agent"))
                .build();

        userSessionRepository.save(session);

        log.info("User session created successfully. sessionId={}, userId={}",
                session.getId(),
                user.getId());
    }

    private void addRefreshCookie(
            HttpServletResponse response,
            String refreshToken
    ) {

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)          // Change to true in production (HTTPS)
                .sameSite("Lax")        // Use "None" if frontend & backend are on different domains
                .path("/nextforgeai/api/v1/auth")   // Cookie sent only to auth endpoints
                .maxAge(Duration.ofDays(30))
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );

        log.info("Refresh token cookie added successfully.");
    }

    private void redirectToFrontend(
            HttpServletResponse response,
            String accessToken
    ) throws IOException {

        log.info("Redirecting user to frontend after successful Google login.");

        String redirectUrl = UriComponentsBuilder
                .fromUriString(FRONTEND_URL)
                .path("/oauth-success")
                .queryParam("token", accessToken)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}