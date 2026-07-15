package br.edu.infnet.petclinic.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Habilita o Spring Data JPA Auditing para preencher os campos
 * {@code @CreatedDate}/{@code @LastModifiedDate} das entidades.
 * Fica isolado em uma classe própria para poder ser importado em fatias
 * de teste ({@code @DataJpaTest}) sem carregar o contexto inteiro.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
