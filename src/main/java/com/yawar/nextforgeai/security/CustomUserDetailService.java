package com.yawar.nextforgeai.security;

import com.yawar.nextforgeai.entity.User;
import com.yawar.nextforgeai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findActiveOrUndeletedUserByIdentifier(username)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid Username/Email or Password"));
        return new CustomUserDetail(user);
    }
}
