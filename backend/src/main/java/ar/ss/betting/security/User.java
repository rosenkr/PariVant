package ar.ss.betting.security;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "app_user")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;
    @Column(name = "p_hash")
    private String pHash;
    @Getter
    @Enumerated(EnumType.STRING)
    private Role role;

    // Should store Google OIC sub claim
    @Getter
    @Column(name = "google_subject", unique = true)
    private String googleSubject;

    private User(String email, String pHash, Role role, String googleSubject) {
        this.email = email;
        this.pHash = pHash;
        this.role = role;
        this.googleSubject = googleSubject;
    }

    public static User localUser(String email, String pHash) {
        return new User(email, pHash, Role.ROLE_USER, null);
    }

    public static User gmailUser(String gmail, String googleSubject) {
        return new User(gmail, null, Role.ROLE_USER, googleSubject);
    }

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
