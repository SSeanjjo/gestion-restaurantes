package co.edu.uniquindio.comanda.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Documentación de la API REST interna (RNF-07). Disponible en /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI comandaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Comanda API")
                        .version("0.1.0")
                        .description("API del sistema de gestión de pedidos para restaurantes. Alcance: CU-01 Gestionar Pedido."))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
