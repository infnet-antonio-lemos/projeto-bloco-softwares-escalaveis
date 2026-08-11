package br.edu.infnet.gateway;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CORS no ponto único de entrada.
 *
 * <p>Nos dois fluxos reais as chamadas chegam same-origin — o proxy do Vite em
 * desenvolvimento e o nginx em Docker (que repassa {@code $http_host}) fazem o browser
 * enxergar tudo na mesma origem. Esta configuração é a rede de segurança para quem
 * acessar o gateway diretamente de {@code :5173} ou {@code :3000}.
 *
 * <p>Registrado com ordem alta (baixa precedência numérica) para rodar antes dos
 * filtros de roteamento do gateway, garantindo que o preflight OPTIONS seja
 * respondido aqui em vez de ser encaminhado ao serviço de destino.
 */
@Configuration
public class CorsConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        FilterRegistrationBean<CorsFilter> registration =
                new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
