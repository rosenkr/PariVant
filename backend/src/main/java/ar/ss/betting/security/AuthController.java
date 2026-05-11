package ar.ss.betting.security;

import ar.ss.betting.security.gis.GoogleAuthRequest;
import ar.ss.betting.security.gis.GooglePrincipal;
import ar.ss.betting.security.gis.GoogleTokenVerifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final UserDetailsRepository userDetailsRepository;
    private final PasswordEncoder encoder;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthController(AuthenticationManager authenticationManager, JWTUtil jwtUtil, UserDetailsRepository userDetailsRepository, PasswordEncoder encoder, GoogleTokenVerifier googleTokenVerifier) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsRepository = userDetailsRepository;
        this.encoder = encoder;
        this.googleTokenVerifier = googleTokenVerifier;
    }


    // Manually delegates to authenticator manager to authenticate the request before generating token
    // Returns JWT token to client
    @PostMapping("/login")
    public AuthResponse loginWithEmail(@RequestBody AuthRequest authRequest){
        String email = authRequest.getEmail();
        Authentication authentication = new UsernamePasswordAuthenticationToken(email, authRequest.getPassword());
        authenticationManager.authenticate(authentication);

        // User is authenticated
        User user = userDetailsRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                ));
        return new AuthResponse(jwtUtil.generateToken(email), new UserDto(email, user.getRole()));
    }

    // Email + pw
    // until email verif impl, auto logins
    @PostMapping("/register")
    public AuthResponse registerAccountWithEmail(@RequestBody AuthRequest authRequest) {
        String email = authRequest.getEmail();
        if(userDetailsRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        User user = User.localUser(email, encoder.encode(authRequest.getPassword()));
        userDetailsRepository.save(user);

        return new AuthResponse(jwtUtil.generateToken(email), new UserDto(email,user.getRole()));
    }

    // Unlike register, doent authenticate a pre existing pw
    @PostMapping("/google")
    public AuthResponse registerOrLoginWithGoogle(@RequestBody GoogleAuthRequest googleAuthRequest) {
        GooglePrincipal principal;
        try{
            principal = googleTokenVerifier.verify(googleAuthRequest.credential());

            // Use the sub field as user identifier
            // Find as existing, or create new and get it
            User user = userDetailsRepository.findByGoogleSubject(principal.sub())
                    .orElseGet(() -> userDetailsRepository.save(
                            User.gmailUser(principal.email(), principal.sub())
                    ));

            return new AuthResponse(
                    jwtUtil.generateToken(user.getUsername()),
                    new UserDto(user.getUsername(), user.getRole())
            );

        }catch(Exception e){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
        }
    }


}
