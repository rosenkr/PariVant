package ar.ss.betting.security.gis;

// Instead of GoogleIdToken, just a record for the data my app needs
public record GooglePrincipal(String sub, String email) {

}
