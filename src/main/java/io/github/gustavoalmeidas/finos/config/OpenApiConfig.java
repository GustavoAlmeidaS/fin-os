package io.github.gustavoalmeidas.finos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:FinOS}")
    private String applicationName;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName + " - API de Controladoria Financeira Pessoal")
                        .version("1.0")
                        .description("Documentação da API operacional para controle financeiro pessoal, " +
                                "com identidade, contas, categorias e lançamentos do ledger.")
                        .contact(new Contact()
                                .name("FinOS")
                                .email("support@finos.com")
                                .url("https://github.com/gustavoalmeidas/finos"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Servidor local"),
                        new Server()
                                .url("https://api.finos.com")
                                .description("Servidor de produção")))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Autenticação JWT. Informe o token como Bearer.")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
} 
