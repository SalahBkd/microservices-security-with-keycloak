package me.boukadi.suppliersservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.client.KeycloakClientRequestFactory;
import org.keycloak.adapters.springsecurity.client.KeycloakRestTemplate;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.stream.Stream;


@Entity
@Data
@NoArgsConstructor @AllArgsConstructor
class Supplier {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
}

@RepositoryRestResource
interface SupplierRepo extends JpaRepository<Supplier, Long> {}

@SpringBootApplication
public class SuppliersServiceApplication {


    public static void main(String[] args) {
        SpringApplication.run(SuppliersServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner start(SupplierRepo supplierRepo, RepositoryRestConfiguration repositoryRestConfiguration) {
        return args -> {
            repositoryRestConfiguration.exposeIdsFor(Supplier.class);
            Stream.of("hp", "dell", "lenovo").forEach(name -> {
                supplierRepo.save(new Supplier(null, name, "contact"+name+"@gmail.com"));
            });
        };
    }
}

// Keycloak needs this object to be in context
@Configuration
class KeycloakConfiguration {
    @Bean
    KeycloakSpringBootConfigResolver configResolver() {
        return new KeycloakSpringBootConfigResolver();
    }
}

@org.keycloak.adapters.springsecurity.KeycloakConfiguration
class KeycloakSpringSecurityAdapter extends KeycloakWebSecurityConfigurerAdapter {
    // KeycloakWebSecurityConfigurerAdapter class extends WebSecurityConfigurerAdapter

    @Override
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        // Session strategy
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        // users & roles management is handled by Keycloak
        auth.authenticationProvider(keycloakAuthenticationProvider());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http.authorizeRequests().antMatchers("/suppliers/**").hasAnyAuthority("app-manager");
    }
}


