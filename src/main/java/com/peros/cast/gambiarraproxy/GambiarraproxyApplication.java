package com.peros.cast.gambiarraproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GambiarraproxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(GambiarraproxyApplication.class, args);
	}

	@Bean
	public ServletRegistrationBean servletRegistrationBean(){
		return new ServletRegistrationBean(new Servlet(),"/redirect/*");
	}
}
