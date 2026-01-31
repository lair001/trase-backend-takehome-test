package com.samlair.trase.agent.config;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

final class QuickAuthSwaggerIndexTransformer extends SwaggerIndexPageTransformer {

	private static final String SWAGGER_UI_DIV = "<div id=\"swagger-ui\"></div>";
	private static final String QUICK_AUTH_TEMPLATE_PATH = "static/swagger-ui-extra/quick-auth.html";
	private static final String QUICK_AUTH_CSS_TAG =
			"<link rel=\"stylesheet\" type=\"text/css\" href=\"/swagger-ui-extra/quick-auth.css\" />";
	private static final String QUICK_AUTH_JS_TAG =
			"<script src=\"/swagger-ui-extra/quick-auth.js\"></script>";

	private final Resource templateResource;

	QuickAuthSwaggerIndexTransformer(
			SwaggerUiConfigProperties swaggerUiConfigProperties,
			SwaggerUiOAuthProperties swaggerUiOAuthProperties,
			SwaggerWelcomeCommon swaggerWelcomeCommon,
			ObjectMapperProvider objectMapperProvider) {
		this(swaggerUiConfigProperties, swaggerUiOAuthProperties, swaggerWelcomeCommon,
				objectMapperProvider, new ClassPathResource(QUICK_AUTH_TEMPLATE_PATH));
	}

	QuickAuthSwaggerIndexTransformer(
			SwaggerUiConfigProperties swaggerUiConfigProperties,
			SwaggerUiOAuthProperties swaggerUiOAuthProperties,
			SwaggerWelcomeCommon swaggerWelcomeCommon,
			ObjectMapperProvider objectMapperProvider,
			Resource templateResource) {
		super(swaggerUiConfigProperties, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider);
		this.templateResource = templateResource;
	}

	@Override
	public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain chain)
			throws IOException {
		Resource transformed = super.transform(request, resource, chain);
		String filename = resource.getFilename();
		if (filename == null || !filename.equals("index.html")) {
			return transformed;
		}
		String html = StreamUtils.copyToString(transformed.getInputStream(), StandardCharsets.UTF_8);
		if (!html.contains(SWAGGER_UI_DIV)) {
			return transformed;
		}
		String template = loadQuickAuthTemplate();
		if (template.isBlank()) {
			return transformed;
		}
		String injection = template
				+ System.lineSeparator()
				+ QUICK_AUTH_CSS_TAG
				+ System.lineSeparator()
				+ QUICK_AUTH_JS_TAG;
		String updated = html.replace(SWAGGER_UI_DIV, injection + System.lineSeparator() + SWAGGER_UI_DIV);
		return new TransformedResource(transformed, updated.getBytes(StandardCharsets.UTF_8));
	}

	private String loadQuickAuthTemplate() throws IOException {
		if (templateResource == null || !templateResource.exists()) {
			return "";
		}
		return StreamUtils.copyToString(templateResource.getInputStream(), StandardCharsets.UTF_8);
	}
}
