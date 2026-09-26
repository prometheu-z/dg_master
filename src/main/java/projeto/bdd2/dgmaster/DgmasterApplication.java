package projeto.bdd2.dgmaster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DgmasterApplication {

	public static void main(String[] args) {
		SpringApplication.run(DgmasterApplication.class, args);
	}

}
