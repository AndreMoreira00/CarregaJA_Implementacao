package br.edu.ifsp.carregaja.control;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import br.edu.ifsp.carregaja.repository.UsuarioRepository;
import br.edu.ifsp.carregaja.service.TokenService;

@Configuration
@EnableWebSecurity
public class SegurancaConfig {

  private static final String MORADOR = "MORADOR";
  private static final String SINDICO = "SINDICO";

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain regras(HttpSecurity http, TokenService tokenService, UsuarioRepository usuarioRepository)
      throws Exception {
    http.csrf().disable()
        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
        .exceptionHandling()
        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
        .accessDeniedHandler((requisicao, resposta, erro) -> resposta.setStatus(HttpStatus.FORBIDDEN.value())).and()
        .addFilterBefore(new TokenFiltro(tokenService, usuarioRepository), UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(regras -> regras
            .antMatchers(HttpMethod.POST, "/autenticacao").permitAll()
            .antMatchers(HttpMethod.PUT, "/autenticacao/senha").authenticated()
            .antMatchers("/usuarios/**").hasRole(SINDICO)
            .antMatchers(HttpMethod.POST, "/vagas/**").hasRole(SINDICO)
            .antMatchers(HttpMethod.PUT, "/vagas/**").hasRole(SINDICO)
            .antMatchers(HttpMethod.DELETE, "/vagas/**").hasRole(SINDICO)
            .antMatchers(HttpMethod.GET, "/veiculos").hasRole(SINDICO)
            .antMatchers(HttpMethod.GET, "/veiculos/meu").hasRole(MORADOR)
            .antMatchers(HttpMethod.POST, "/veiculos").hasRole(MORADOR)
            .antMatchers(HttpMethod.PUT, "/veiculos/**").hasRole(MORADOR)
            .anyRequest().hasAnyRole(MORADOR, SINDICO));
    return http.build();
  }
}
