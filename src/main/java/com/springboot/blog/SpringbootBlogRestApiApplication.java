package com.springboot.blog;

import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@EnableCaching
@EnableScheduling
@SpringBootApplication
public class SpringbootBlogRestApiApplication {
	@Bean
	public ModelMapper modelMapper(){
		return new ModelMapper();
	}
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
	public static void main(String[] args) {
		SpringApplication.run(SpringbootBlogRestApiApplication.class, args);
		ModelMapper modelMapper = new ModelMapper();
		Configuration configuration = modelMapper.getConfiguration();
		configuration.setMatchingStrategy(MatchingStrategies.STRICT);
	}
}
