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

	/**
	 * Builds the Swagger index transformer that injects quick-auth UI content.
	 *
	 * @param swaggerUiConfigProperties swagger UI configuration.
	 * @param swaggerUiOAuthProperties swagger UI OAuth configuration.
	 * @param swaggerWelcomeCommon common Swagger UI helper.
	 * @param objectMapperProvider Jackson object mapper provider.
	 * @return index transformer for Swagger UI.
	 */
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
