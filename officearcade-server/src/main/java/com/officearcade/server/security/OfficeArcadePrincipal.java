package com.officearcade.server.security;

import com.officearcade.server.identity.AppRole;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class OfficeArcadePrincipal implements UserDetails {

    private final String id;
    private final String email;
    private final String displayName;
    private final AppRole role;
    private final boolean enabled;
    private final boolean suspended;
    private final String suspensionNote;

    public OfficeArcadePrincipal(
            String id,
            String email,
            String displayName,
            AppRole role,
            boolean enabled,
            boolean suspended,
            String suspensionNote
    ) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.enabled = enabled;
        this.suspended = suspended;
        this.suspensionNote = suspensionNote;
    }

    public String id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String displayName() {
        return displayName;
    }

    public AppRole role() {
        return role;
    }

    public boolean suspended() {
        return suspended;
    }

    public String suspensionNote() {
        return suspensionNote;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
