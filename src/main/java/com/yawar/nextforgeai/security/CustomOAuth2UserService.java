package com.yawar.nextforgeai.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        log.info("========== Inside CustomOAuth2UserService ==========");

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        String googleId = oAuth2User.getAttribute("sub");
        Boolean emailVerified = oAuth2User.getAttribute("email_verified");

        log.info("Google OAuth login received. email={}, googleId={}", email, googleId);

        if (email == null || googleId == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info"),
                    "Google did not return the required user information."
            );
        }

        if (Boolean.FALSE.equals(emailVerified)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_verified"),
                    "Google account email is not verified."
            );
        }

        return oAuth2User;
    }
}