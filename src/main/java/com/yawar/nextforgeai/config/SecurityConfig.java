package com.yawar.nextforgeai.config;

import com.yawar.nextforgeai.security.CustomOAuth2UserService;
import com.yawar.nextforgeai.security.JwtAuthenticationFilter;
import com.yawar.nextforgeai.security.OAuth2AuthenticationFailureHandler;
import com.yawar.nextforgeai.security.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {

        httpSecurity.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator/health" ,
                            "/actuator/info","/errors", "/auth/**", "/v3/api-docs", "/payments/webhooks/**")
                            .permitAll()
                            .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                            .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                            .anyRequest().authenticated();
                })
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(user ->
                                user.userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                ).exceptionHandling(exceptionHandlingConfigure -> {
                    exceptionHandlingConfigure.accessDeniedHandler((request, response, accessDeniedException) -> {
                        handlerExceptionResolver.resolveException(request, response, null, accessDeniedException);
                    });
                })
                .httpBasic(httpBasic -> httpBasic.disable())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("""
                                    {
                                      "message": "Unauthorized"
                                    }
                                    """);
                        })
                );

        return httpSecurity.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }

}
