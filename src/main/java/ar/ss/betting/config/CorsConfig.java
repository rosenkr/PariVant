package ar.ss.betting.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
// When you deploy:
//
//set app.cors.allowed-origins=https://<your-vercel-app>.vercel.app
//
//keep localhost origins for dev only
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;
    private final String[] allowedMethods;

    public CorsConfig(
            @Value("${app.cors.allowed-origins:}") String allowedOrigins,
            @Value("${app.cors.allowed-methods:GET,POST,OPTIONS}") String allowedMethods
    ) {
        this.allowedOrigins = splitCsv(allowedOrigins);
        this.allowedMethods = splitCsv(allowedMethods);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Apply to all endpoints for now (public + internal).
        // Later you can tighten this, e.g. only /public/** for SPA.
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.length == 0 ? new String[] {"*"} : allowedOrigins)
                .allowedMethods(allowedMethods.length == 0 ? new String[] {"*"} : allowedMethods)
                .allowedHeaders("*")
                .allowCredentials(false);
    }

    private static String[] splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return new String[0];
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
    }
}