package br.com.srcsoftware.topomanager.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Com allowCredentials(true), NÃO pode usar setAllowedOrigins("*")
        config.setAllowCredentials(true);
        
        // Liste as origens explicitamente ou use Patterns
        config.setAllowedOriginPatterns(Arrays.asList(
            "https://topomanager.onrender.com",
            "http://localhost:[*]",
            "http://127.0.0.1:[*]"
        ));
        
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Exponha o header para que o JS consiga ler o nome do arquivo no download
        config.addExposedHeader("Content-Disposition");

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}