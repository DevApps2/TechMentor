package com.techmentor.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
public class ApiApplication {

	// O relógio do servidor passa a ser o do TechMentor, esteja a máquina onde estiver (nuvem em UTC, por exemplo).
	// Assim LocalDateTime.now(), o banco e as datas enviadas ao app falam sempre do mesmo fuso (APP_FUSO muda).
	static {
		String fuso = System.getenv().getOrDefault("APP_FUSO", "America/Sao_Paulo");
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(fuso)));
	}

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

}
