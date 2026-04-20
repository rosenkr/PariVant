package ar.ss.betting.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, JWTUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }


    // Manually delegates to authenticator manager to authenticate the request before generating token
    // Returns JWT token to client
    @PostMapping("/authenticate")
    public String generateToken(@RequestBody AuthRequest authRequest){
        String username = authRequest.getUsername();
        try {
            Authentication authentication = new UsernamePasswordAuthenticationToken(username, authRequest.getPassword());
            authenticationManager.authenticate(authentication);

            // User is authenticated

            return jwtUtil.generateToken(username);
        } catch (Exception e) {
            throw e;
        }
    }
}
