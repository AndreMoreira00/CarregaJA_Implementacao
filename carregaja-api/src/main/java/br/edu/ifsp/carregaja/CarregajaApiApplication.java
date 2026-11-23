package br.edu.ifsp.carregaja;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CarregajaApiApplication extends SpringBootServletInitializer {

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
    return builder.sources(CarregajaApiApplication.class);
  }

  @Bean
  public Clock relogio(@Value("${carregaja.fuso}") String fuso) {
    return Clock.system(ZoneId.of(fuso));
  }

  public static void main(String[] args) {
    SpringApplication.run(CarregajaApiApplication.class, args);
  }

}
