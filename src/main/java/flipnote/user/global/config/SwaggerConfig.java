package flipnote.user.global.config;

import flipnote.user.global.constants.HttpConstants;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FlipNote User/Auth API")
                        .description("FlipNote User/Auth API")
                        .version("1.0.0"))
                .servers(List.of(
                        new Server().url("https://api3.flipnote.site").description("Production"),
                        new Server().url("http://localhost:8081").description("Local")
                ));
    }

    @Bean
    public OperationCustomizer hideInternalHeaders() {
        return (operation, handlerMethod) -> {
            if (operation.getParameters() != null) {
                operation.getParameters().removeIf(p ->
                        HttpConstants.USER_ID_HEADER.equals(p.getName()));
            }
            return operation;
        };
    }
}
