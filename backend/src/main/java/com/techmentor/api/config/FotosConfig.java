package com.techmentor.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class FotosConfig implements WebMvcConfigurer {

    @Value("${app.pasta-fotos:fotos}")
    private String pastaFotos;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            Path pasta = Path.of(pastaFotos).toAbsolutePath();
            Files.createDirectories(pasta);

            String local = pasta.toUri().toString();
            if (!local.endsWith("/")) {
                local += "/";
            }
            registry.addResourceHandler("/fotos/**").addResourceLocations(local);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível criar a pasta de fotos", e);
        }
    }
}