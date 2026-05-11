package ar.ss.betting.security.gis;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;


// Receives raw token string, verifies using GoogleIdTokenVerifier, extracts claims
// of interest, returns my own GooglePrincipal
@Component
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(@Value("${google.web.client.id}") String webClientId) throws GeneralSecurityException, IOException {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(Collections.singletonList(webClientId))
                .build();
    }

    public GooglePrincipal verify(String token) throws GeneralSecurityException, IOException {
        GoogleIdToken idToken = verifier.verify(token);
        if (idToken == null) {
            throw new GeneralSecurityException("Invalid token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String subject = payload.getSubject();
        String email = payload.getEmail();

        if (subject == null || email == null) {
            throw new GeneralSecurityException("Google ID token is missing required claims.");
        }

        return new GooglePrincipal(subject, email);

    }
}
