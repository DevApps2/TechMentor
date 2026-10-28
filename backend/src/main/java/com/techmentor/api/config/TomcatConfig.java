package com.techmentor.api.config;

import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {

    /**
     * Vários endpoints identificam o curso pelo título na URL (ex.: /cursos/UX%2FUI%20na%20prática).
     * Por padrão o Tomcat recusa "%2F" (barra codificada) com 400; no modo "passthrough" ele repassa
     * o trecho sem decodificar e o Spring MVC o entrega ao @PathVariable já como "UX/UI na prática".
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> permitirBarraCodificadaNaUrl() {
        return factory -> factory.addConnectorCustomizers(
                connector -> connector.setEncodedSolidusHandling("passthrough"));
    }
}
