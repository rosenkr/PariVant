package ar.ss.betting.security;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// Implements UserDetails which the Spring authentication provider understands
// email is the effective username either from gmail or email flows
@Entity
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;
    private String pHash;
    @Enumerated(EnumType.STRING)
    private Role role;

    // Should store Google OIC sub claim
    @Column(unique = true)
    private String googleSubject;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public @Nullable String getPassword() {
        return pHash;
    }

    @Override
    public String getUsername() {
        return email;
    }


}
