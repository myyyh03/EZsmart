package com.test.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Locale;

@SpringBootApplication
public class EzsmartApplication {

	public static void main(String[] args) {

		Locale.setDefault(Locale.US);

		SpringApplication.run(EzsmartApplication.class, args);
	}
}