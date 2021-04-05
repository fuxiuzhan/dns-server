package
        com.fxz.console.config;

import springfox.documentation.service.Contact;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author fxz
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Value(("${dns.console.swagger.name:fuled-dns-server}"))
    private String name;
    @Value(("${dns.console.swagger.url:https://www.fuled.xyz}"))
    private String url;
    @Value(("${dns.console.swagger.mail:fuxiuzhan@163.com}"))
    private String mail;

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .pathMapping("/")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.fxz.console.controller"))
                .paths(PathSelectors.any())
                .build().apiInfo(new ApiInfoBuilder()
                        .title("dns-server-console")
                        .description("dns-server-console")
                        .version("1.0")
                        .contact(new Contact(name, url, mail))
                        .license("The Apache License")
                        .licenseUrl("http://www.fuled.xyz")
                        .build());
    }
}