package projeto.bdd2.dgmaster.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dgMasterOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DG Master API")
                        .description("Sistema de Aluguel de Jogos de Tabuleiro - API REST completa")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipe DG Master")
                                .email("contato@dgmaster.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Servidor Local"),
                        new Server().url("https://api.dgmaster.com").description("Servidor de Produção")
                ))
                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("basicAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("basicAuth",
                                new SecurityScheme()
                                        .type(Type.HTTP)
                                        .scheme("basic")
                                        .description("Autenticação Basic Auth (usuário:senha)"))
                        .addSecuritySchemes("sessionAuth",
                                new SecurityScheme()
                                        .type(Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .name("JSESSIONID")
                                        .description("Autenticação por Sessão (Cookie)")));
    }
}
