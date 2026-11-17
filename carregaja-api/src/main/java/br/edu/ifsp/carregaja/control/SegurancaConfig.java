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
        // mvcMatchers, e não antMatchers: o Spring MVC atende "/tarifas/" como "/tarifas", e o casamento exato do
        // antMatchers deixaria a barra final cair numa regra mais frouxa (RF-03, RN-07)
        .authorizeHttpRequests(regras -> regras
            // As telas (HTML, CSS e JS) ficam em resources/static e são públicas: quem protege os dados é a API
            .antMatchers(HttpMethod.GET, "/", "/*.html", "/css/**", "/js/**").permitAll()
            .mvcMatchers(HttpMethod.POST, "/autenticacao").permitAll()
            .mvcMatchers(HttpMethod.PUT, "/autenticacao/senha").authenticated()
            .mvcMatchers("/usuarios/**").hasRole(SINDICO)
            .mvcMatchers(HttpMethod.POST, "/vagas/**").hasRole(SINDICO)
            .mvcMatchers(HttpMethod.PUT, "/vagas/**").hasRole(SINDICO)
            .mvcMatchers(HttpMethod.DELETE, "/vagas/**").hasRole(SINDICO)
            .mvcMatchers(HttpMethod.GET, "/tarifas").hasRole(SINDICO)
            .mvcMatchers(HttpMethod.POST, "/tarifas").hasRole(SINDICO)
            .mvcMatchers(HttpMethod.GET, "/sessoes/abertas").hasRole(SINDICO)
            .mvcMatchers(HttpMethod.POST, "/sessoes/*/encerrar-administrativamente").hasRole(SINDICO)
            .mvcMatchers("/sessoes/**").hasRole(MORADOR)
            .mvcMatchers("/consumos/**").hasRole(MORADOR)
            .mvcMatchers("/fechamentos/**").hasRole(SINDICO)
            .mvcMatchers(HttpMethod.GET, "/historico/**").hasRole(SINDICO)
            .mvcMatchers(HttpMethod.GET, "/veiculos").hasRole(SINDICO)
            .mvcMatchers(HttpMethod.GET, "/veiculos/meu").hasRole(MORADOR)
            .mvcMatchers(HttpMethod.POST, "/veiculos").hasRole(MORADOR)
            .mvcMatchers(HttpMethod.PUT, "/veiculos/**").hasRole(MORADOR)
            .anyRequest().hasAnyRole(MORADOR, SINDICO));
    return http.build();
  }
}
