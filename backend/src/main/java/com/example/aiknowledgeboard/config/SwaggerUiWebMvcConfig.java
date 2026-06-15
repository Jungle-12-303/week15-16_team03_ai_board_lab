package com.example.aiknowledgeboard.config;

import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerResourceResolver;
import org.springdoc.webmvc.ui.SwaggerWebMvcConfigurer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerUiWebMvcConfig {

    @Bean
    SwaggerWebMvcConfigurer swaggerWebMvcConfigurer(
            SwaggerUiConfigProperties swaggerUiConfigProperties,
            WebProperties springWebProperties,
            WebMvcProperties springWebMvcProperties,
            SwaggerIndexTransformer swaggerIndexTransformer,
            SwaggerResourceResolver swaggerResourceResolver,
            SwaggerWelcomeCommon swaggerWelcomeCommon
    ) {
        return new Boot35SwaggerWebMvcConfigurer(
                swaggerUiConfigProperties,
                springWebProperties,
                springWebMvcProperties,
                swaggerIndexTransformer,
                swaggerResourceResolver,
                swaggerWelcomeCommon
        );
    }

    private static class Boot35SwaggerWebMvcConfigurer extends SwaggerWebMvcConfigurer {
        private final SwaggerUiConfigProperties swaggerUiConfigProperties;

        Boot35SwaggerWebMvcConfigurer(
                SwaggerUiConfigProperties swaggerUiConfigProperties,
                WebProperties springWebProperties,
                WebMvcProperties springWebMvcProperties,
                SwaggerIndexTransformer swaggerIndexTransformer,
                SwaggerResourceResolver swaggerResourceResolver,
                SwaggerWelcomeCommon swaggerWelcomeCommon
        ) {
            super(
                    swaggerUiConfigProperties,
                    springWebProperties,
                    springWebMvcProperties,
                    swaggerIndexTransformer,
                    swaggerResourceResolver,
                    swaggerWelcomeCommon
            );
            this.swaggerUiConfigProperties = swaggerUiConfigProperties;
        }

        @Override
        protected SwaggerResourceHandlerConfig[] getSwaggerHandlerConfigs() {
            String swaggerUiPath = getUiRootPath() + "/swagger-ui";
            String swaggerUiResourceLocation = "classpath:/META-INF/resources/webjars/swagger-ui/"
                    + swaggerUiConfigProperties.getVersion()
                    + "/";

            return new SwaggerResourceHandlerConfig[]{
                    SwaggerResourceHandlerConfig.createCached()
                            .setPatterns(swaggerUiPath + "/**")
                            .setLocations(swaggerUiResourceLocation),
                    SwaggerResourceHandlerConfig.createUncached()
                            .setPatterns(swaggerUiPath + "/*swagger-initializer.js")
                            .setLocations(swaggerUiResourceLocation)
            };
        }

        @Override
        protected SwaggerResourceHandlerConfig[] getSwaggerWebjarHandlerConfigs() {
            return new SwaggerResourceHandlerConfig[]{};
        }
    }
}
