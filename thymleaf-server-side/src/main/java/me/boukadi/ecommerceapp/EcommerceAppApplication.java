package me.boukadi.ecommerceapp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.client.KeycloakClientRequestFactory;
import org.keycloak.adapters.springsecurity.client.KeycloakRestTemplate;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.keycloak.adapters.springsecurity.facade.SimpleHttpFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Entity
@Data @NoArgsConstructor @AllArgsConstructor
class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private double price;
}
interface ProductRepo extends JpaRepository<Product, Long> {}
@Controller
class ProductController {
    @Autowired
    private ProductRepo productRepo;
    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/products")
    public String getProducts(Model model) {
        model.addAttribute("products", productRepo.findAll());
        return "products";
    }



}

@SpringBootApplication
public class EcommerceAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(EcommerceAppApplication.class, args);
    }
    @Bean
    CommandLineRunner start(ProductRepo productRepo) {
        return args -> {
          productRepo.save(new Product(null, "hp", 5000.0));
          productRepo.save(new Product(null, "dell", 6000.0));
          productRepo.save(new Product(null, "lenovo", 7000.0));
          productRepo.findAll().forEach(product -> System.out.println(product.getName()));
        };
    }
}

// ====================================== SECURITY ======================================
// Keycloak needs this object to be in context
@Configuration
class KeycloakConfiguration {
    @Bean
    KeycloakSpringBootConfigResolver configResolver() {
        return new KeycloakSpringBootConfigResolver();
    }

    @Bean
    KeycloakRestTemplate keycloakRestTemplate(KeycloakClientRequestFactory keycloakClientRequestFactory) {
        return new KeycloakRestTemplate(keycloakClientRequestFactory);
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
        http.authorizeRequests().antMatchers("/products/**").authenticated();
    }
}

@Controller
class SecurityController {
    @Autowired
    private AdapterDeploymentContext adapterDeploymentContext;

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) throws ServletException {
        request.logout();
        return "redirect:/";
    }

    @GetMapping("/profile")
    public String profile(
            RedirectAttributes attributes,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException {
        HttpFacade facade = new SimpleHttpFacade(request, response);
        KeycloakDeployment deployment = adapterDeploymentContext.resolveDeployment(facade);
        attributes.addAttribute("referrer", deployment.getResourceName());
        attributes.addAttribute("referrer_uri", request.getHeader("referer"));

        // getAccountUrl() return the URL of the account to be modified
        return "redirect:" + deployment.getAccountUrl();
    }
}

@Controller
class SupplierController {
    @Autowired
    private KeycloakRestTemplate keycloakRestTemplate;

    @GetMapping("/suppliers")
    public String index(Model model) {
        // RestTemplate allow us to communicate with HTTP
        // WITH RestTemplate: we send JWT token for each request
        // WITH KEYCLOAK: keycloakRestTemplate does that automatically, it's a class that inherits from RestTemplate
        ResponseEntity<PagedModel<Supplier>> response =
                keycloakRestTemplate.exchange("http://localhost:8083/suppliers",
                        HttpMethod.GET, null, new ParameterizedTypeReference<PagedModel<Supplier>>() {});
        model.addAttribute("suppliers", response.getBody().getContent());
        return "suppliers";
    }

    @ExceptionHandler(Exception.class)
    public String exceptionHandler() {
        return "errors";
    }
}

@Data
class Supplier {
    private Long id;
    private String name;
    private String email;
}
