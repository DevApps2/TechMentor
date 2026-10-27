package com.techmentor.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI techMentorOpenApi() {
        return new OpenAPI().info(new Info()
                .title("TechMentor API")
                .description("API da plataforma TechMentor: cursos, mentorias, oportunidades, gamificação e segurança.")
                .version("0.0.1"));
    }

    @Bean
    public OperationCustomizer descricaoHeaderUsuario() {
        return (operation, handlerMethod) -> {
            if (operation.getParameters() != null) {
                operation.getParameters().stream()
                        .filter(p -> "header".equals(p.getIn()) && "X-Usuario-Id".equals(p.getName()))
                        .forEach(p -> p.setDescription("ID do usuário que está executando a ação"));
            }
            return operation;
        };
    }
}
