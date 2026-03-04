package br.com.srcsoftware.topomanager.config;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Value("${app.api.url:http://localhost:8080/api/excel}")
    private String apiUrl;

    @GetMapping("/url")
    public Map<String, String> getApiUrl() {
        return Collections.singletonMap("apiUrl", apiUrl);
    }
}