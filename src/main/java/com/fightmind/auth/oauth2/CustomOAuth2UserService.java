package com.fightmind.auth.oauth2;

import com.fightmind.auth.CustomUserDetails;
import com.fightmind.user.User;
import com.fightmind.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Hook invoked by Spring Security after a successful Google login.
 *
 * 1. Takes the user's Google Profile.
 * 2. If it's their first time logging in, registers them in our PostgreSQL DB.
 * 3. If they are a returning user, updates their profile.
 * 4. Returns a CustomUserDetails (which allows issuing our own JWT later).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Fetch the user's profile from Google using the Spring Security default logic
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String googleId = (String) attributes.get("sub");

        log.info("Google OAuth2 login callback received for email: {}", email);

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            // If the user previously signed up with email/password, 
            // but is now logging in with Google, we link the accounts.
            if (!"GOOGLE".equals(user.getProvider())) {
                user.setProvider("GOOGLE");
                user.setGoogleId(googleId);
                user = userRepository.save(user);
                log.info("Linked existing LOCAL account to GOOGLE for: {}", email);
            }
        } else {
            // First time login — register the user automatically
            user = User.builder()
                    .email(email)
                    .username(generateUniqueUsername(name))
                    .provider("GOOGLE")
                    .googleId(googleId)
                    // No password needed for OAuth2
                    .build();

            user = userRepository.save(user);
            log.info("Registered new GOOGLE account for: {}", email);
        }

        return CustomUserDetails.create(user, attributes);
    }

    private String generateUniqueUsername(String name) {
        String baseName = name.replaceAll("\\s+", "").toLowerCase();
        String username = baseName;
        int count = 1;
        
        while (userRepository.existsByUsername(username)) {
            username = baseName + count;
            count++;
        }
        return username;
    }
}
