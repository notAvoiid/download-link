package com.abreu.download_link;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DownloadLinkApplication {

	public static void main(String[] args) {
		SpringApplication.run(DownloadLinkApplication.class, args);
	}

}
