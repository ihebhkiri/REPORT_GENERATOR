package RHIS.com.RHIS;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RhisApplication {

	public static void main(String[] args) {
		SpringApplication.run(RhisApplication.class, args);
	}

}
