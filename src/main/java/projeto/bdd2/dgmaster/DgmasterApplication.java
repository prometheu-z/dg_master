package projeto.bdd2.dgmaster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class DgmasterApplication {

	public static void main(String[] args) {
		// DB-02: mantém CURDATE()/NOW() consistentes com America/Sao_Paulo.
		TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
		SpringApplication.run(DgmasterApplication.class, args);
	}

}
