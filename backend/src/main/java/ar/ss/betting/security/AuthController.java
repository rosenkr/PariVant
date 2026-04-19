package ar.ss.betting.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthenticationProvider authenticationProvider;

    public AuthController(AuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    @PostMapping("/authenticate")
    public void generateToken(AuthRequest authRequest){
        try {
            Authentication authentication = new UsernamePasswordAuthenticationToken();
            authenticationProvider.authenticate(authentication);
        } catch (BadCredentialsException e) {

        }
    }
}
