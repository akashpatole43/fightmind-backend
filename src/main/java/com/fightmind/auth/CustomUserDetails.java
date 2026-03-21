package com.fightmind.auth;

import com.fightmind.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Adapter that provides the User entity to Spring Security.
 * Implements BOTH UserDetails (for email/password login) and
 * OAuth2User (for Google login).
 */
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails, OAuth2User {

    private final User user;
    private Map<String, Object> attributes;

    // Factory proxy for OAuth2
    public static CustomUserDetails create(User user, Map<String, Object> attributes) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        userDetails.setAttributes(attributes);
        return userDetails;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public User getUser() {
        return user;
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(user.getRole()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        // Spring Security usually expects the login identifier here (we use email for login)
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

    // ── OAuth2User Methods ──────────────────────────────────────────

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return user.getId().toString();
    }
}
