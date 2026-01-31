package com.samlair.trase.agent.config;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SuppressWarnings("deprecation")
class SwaggerUiCustomizationConfigTest {

	@TempDir
	Path tempDir;

	@Test
	void transformSkipsResourcesWithoutIndexFilename() throws Exception {
		Resource resource = new NullFilenameResource("body");
		Resource transformed = transformer().transform(null, resource, passthroughChain());
		assertSame(resource, transformed);
	}

	@Test
	void transformSkipsNonIndexHtml() throws Exception {
		Path cssPath = tempDir.resolve("swagger-ui.css");
		Files.writeString(cssPath, "body {}");
		Resource resource = new FileSystemResource(cssPath.toFile());
		Resource transformed = transformer().transform(null, resource, passthroughChain());
		assertSame(resource, transformed);
	}

	@Test
	void transformSkipsIndexWithoutSwaggerUiPlaceholder() throws Exception {
		Path indexPath = tempDir.resolve("index.html");
		Files.writeString(indexPath, "<html><body>no swagger div</body></html>");
		Resource resource = new FileSystemResource(indexPath.toFile());
		Resource transformed = transformer().transform(null, resource, passthroughChain());
		assertSame(resource, transformed);
	}

	@Test
	void transformInjectsQuickAuthAssetsWhenTemplatePresent() throws Exception {
		Path indexPath = tempDir.resolve("index.html");
		Files.writeString(indexPath, "<html><body><div id=\"swagger-ui\"></div></body></html>");
		Resource resource = new FileSystemResource(indexPath.toFile());

		Resource transformed = transformer().transform(null, resource, passthroughChain());

		assertInstanceOf(TransformedResource.class, transformed);
		String html = new String(transformed.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		assertTrue(html.contains("swagger-quick-auth-template"));
		assertTrue(html.contains("/swagger-ui-extra/quick-auth.js"));
		assertTrue(html.contains("/swagger-ui-extra/quick-auth.css"));
	}

	@Test
	void transformSkipsInjectionWhenTemplateMissing() throws Exception {
		Path indexPath = tempDir.resolve("index.html");
		Files.writeString(indexPath, "<html><body><div id=\"swagger-ui\"></div></body></html>");
		Resource resource = new FileSystemResource(indexPath.toFile());
		QuickAuthSwaggerIndexTransformer transformer = transformer(
				new ClassPathResource("static/swagger-ui-extra/missing-template.html"));

		Resource transformed = transformer.transform(null, resource, passthroughChain());

		assertSame(resource, transformed);
	}

	@Test
	void transformSkipsInjectionWhenTemplateResourceNull() throws Exception {
		Path indexPath = tempDir.resolve("index.html");
		Files.writeString(indexPath, "<html><body><div id=\"swagger-ui\"></div></body></html>");
		Resource resource = new FileSystemResource(indexPath.toFile());

		QuickAuthSwaggerIndexTransformer transformer = transformer(null);
		Resource transformed = transformer.transform(null, resource, passthroughChain());

		assertSame(resource, transformed);
	}

	private QuickAuthSwaggerIndexTransformer transformer() {
		return transformer(new ClassPathResource("static/swagger-ui-extra/quick-auth.html"));
	}

	private QuickAuthSwaggerIndexTransformer transformer(Resource templateResource) {
		SwaggerUiConfigProperties uiConfig = new SwaggerUiConfigProperties();
		SwaggerUiOAuthProperties oauthConfig = new SwaggerUiOAuthProperties();
		SpringDocConfigProperties springDocConfig = new SpringDocConfigProperties();
		ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider(springDocConfig);
		SwaggerWelcomeCommon swaggerWelcome = new NoOpSwaggerWelcome(uiConfig, springDocConfig);
		return new QuickAuthSwaggerIndexTransformer(
				uiConfig, oauthConfig, swaggerWelcome, objectMapperProvider, templateResource);
	}

	private ResourceTransformerChain passthroughChain() {
		return new ResourceTransformerChain() {
			@Override
			public org.springframework.web.servlet.resource.ResourceResolverChain getResolverChain() {
				return null;
			}

			@Override
			public Resource transform(HttpServletRequest request, Resource resource) {
				return resource;
			}
		};
	}

	private static final class NoOpSwaggerWelcome extends SwaggerWelcomeCommon {

		private NoOpSwaggerWelcome(
				SwaggerUiConfigProperties swaggerUiConfig,
				SpringDocConfigProperties springDocConfigProperties) {
			super(swaggerUiConfig, springDocConfigProperties);
		}

		@Override
		protected void calculateUiRootPath(SwaggerUiConfigParameters swaggerUiConfigParameters,
				StringBuilder... stringBuilders) {
		}

		@Override
		protected void buildApiDocUrl(SwaggerUiConfigParameters swaggerUiConfigParameters) {
		}

		@Override
		protected String buildUrlWithContextPath(
				SwaggerUiConfigParameters swaggerUiConfigParameters,
				String contextPath) {
			return "";
		}

		@Override
		protected void buildSwaggerConfigUrl(SwaggerUiConfigParameters swaggerUiConfigParameters) {
		}

		@Override
		protected void buildFromCurrentContextPath(
				SwaggerUiConfigParameters swaggerUiConfigParameters,
				HttpServletRequest request) {
		}
	}

	private static final class NullFilenameResource extends AbstractResource {

		private final byte[] content;

		private NullFilenameResource(String body) {
			this.content = body.getBytes(StandardCharsets.UTF_8);
		}

		@Override
		public String getDescription() {
			return "In-memory resource with no filename";
		}

		@Override
		public InputStream getInputStream() {
			return new ByteArrayInputStream(content);
		}

		@Override
		public String getFilename() {
			return null;
		}

		@Override
		public URL getURL() throws IOException {
			try {
				return new URL("file:/swagger-ui.css");
			} catch (MalformedURLException ex) {
				throw new IOException(ex);
			}
		}
	}
}
