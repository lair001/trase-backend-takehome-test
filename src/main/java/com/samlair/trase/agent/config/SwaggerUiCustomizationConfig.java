package com.samlair.trase.agent.config;

import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Adds a quick-auth dropdown to Swagger UI for dev/test users.
 */
@Configuration
public class SwaggerUiCustomizationConfig {

	@Bean
	public SwaggerIndexTransformer swaggerIndexTransformer(
			SwaggerUiConfigProperties swaggerUiConfigProperties,
			SwaggerUiOAuthProperties swaggerUiOAuthProperties,
			SwaggerWelcomeCommon swaggerWelcomeCommon,
			ObjectMapperProvider objectMapperProvider) {
		return new QuickAuthSwaggerIndexTransformer(
				swaggerUiConfigProperties,
				swaggerUiOAuthProperties,
				swaggerWelcomeCommon,
				objectMapperProvider);
	}
}
