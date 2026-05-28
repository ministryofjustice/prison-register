package uk.gov.justice.digital.hmpps.prisonregister.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
class ResourceServerConfiguration {
  @Bean
  fun filterChain(http: HttpSecurity): SecurityFilterChain = http
    .headers { it.frameOptions { fo -> fo.sameOrigin() } }
    .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
    .csrf { it.disable() }
    .authorizeHttpRequests {
      it.requestMatchers(
        "/webjars/**", "/favicon.ico", "/csrf",
        "/health/**", "/info", "/ping", "/h2-console/**",
        "/v3/api-docs/**", "/api/swagger.json", "/swagger-ui/**",
        "/v3/api-docs", "/swagger-ui.html",
        "/swagger-resources", "/swagger-resources/configuration/ui", "/swagger-resources/configuration/security",
        "/prisons/**",
        "/gp/**",
      ).permitAll()
      it.anyRequest().authenticated()
    }
    .oauth2ResourceServer { it.jwt { jwt -> jwt.jwtAuthenticationConverter(AuthAwareTokenConverter()) } }
    .build()
}
