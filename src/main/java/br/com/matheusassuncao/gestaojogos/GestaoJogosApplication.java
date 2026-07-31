package br.com.matheusassuncao.gestaojogos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GestaoJogosApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestaoJogosApplication.class, args);
	}

}
