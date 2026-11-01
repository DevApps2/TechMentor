package com.techmentor.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Liga o @Async, usado para mandar e-mails sem fazer o usuário esperar o servidor de e-mail. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
