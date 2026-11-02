package com.techmentor.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Liga o @Async (e-mails sem fazer o usuário esperar o servidor de e-mail)
 * e o @Scheduled (lembretes de mentoria de minuto em minuto).
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}