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

/**
 * Unit tests for SwaggerUiCustomizationConfigTest.
 */
@SuppressWarnings("deprecation")
class SwaggerUiCustomizationConfigTest {

	@TempDir
	Path tempDir;

	/**
	 * Verifies transform skips resources without index filename.
	 */
	@Test
	void transformSkipsResourcesWithoutIndexFilename() throws Exception {
		Resource resource = new NullFilenameResource("body");
		Resource transformed = transformer().transform(null, resource, passthroughChain());
		assertSame(resource, transformed);
	}

	/**
	 * Verifies transform skips non index html.
	 */
	@Test
	void transformSkipsNonIndexHtml() throws Exception {
		Path cssPath = tempDir.resolve("swagger-ui.css");
		Files.writeString(cssPath, "body {}");
		Resource resource = new FileSystemResource(cssPath.toFile());
		Resource transformed = transformer().transform(null, resource, passthroughChain());
		assertSame(resource, transformed);
	}

	/**
	 * Verifies transform skips index without swagger ui placeholder.
	 */
	@Test
	void transformSkipsIndexWithoutSwaggerUiPlaceholder() throws Exception {
		Path indexPath = tempDir.resolve("index.html");
		Files.writeString(indexPath, "<html><body>no swagger div</body></html>");
		Resource resource = new FileSystemResource(indexPath.toFile());
		Resource transformed = transformer().transform(null, resource, passthroughChain());
		assertSame(resource, transformed);
	}

	/**
	 * Verifies transform injects quick auth assets when template present.
	 */
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

	/**
	 * Verifies transform skips injection when template missing.
	 */
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

	/**
	 * Verifies transform skips injection when template resource null.
	 */
	@Test
	void transformSkipsInjectionWhenTemplateResourceNull() throws Exception {
		Path indexPath = tempDir.resolve("index.html");
		Files.writeString(indexPath, "<html><body><div id=\"swagger-ui\"></div></body></html>");
		Resource resource = new FileSystemResource(indexPath.toFile());

		QuickAuthSwaggerIndexTransformer transformer = transformer(null);
		Resource transformed = transformer.transform(null, resource, passthroughChain());

		assertSame(resource, transformed);
	}

	/**
	 * Helper for transformer.
	 */
	private QuickAuthSwaggerIndexTransformer transformer() {
		return transformer(new ClassPathResource("static/swagger-ui-extra/quick-auth.html"));
	}

	/**
	 * Helper for transformer.
	 */
	private QuickAuthSwaggerIndexTransformer transformer(Resource templateResource) {
		SwaggerUiConfigProperties uiConfig = new SwaggerUiConfigProperties();
		SwaggerUiOAuthProperties oauthConfig = new SwaggerUiOAuthProperties();
		SpringDocConfigProperties springDocConfig = new SpringDocConfigProperties();
		ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider(springDocConfig);
		SwaggerWelcomeCommon swaggerWelcome = new NoOpSwaggerWelcome(uiConfig, springDocConfig);
		return new QuickAuthSwaggerIndexTransformer(
				uiConfig, oauthConfig, swaggerWelcome, objectMapperProvider, templateResource);
	}

	/**
	 * Helper for passthrough chain.
	 */
	private ResourceTransformerChain passthroughChain() {
		return new ResourceTransformerChain() {
			/**
			 * Helper for get resolver chain.
			 */
			@Override
			public org.springframework.web.servlet.resource.ResourceResolverChain getResolverChain() {
				return null;
			}

			/**
			 * Helper for transform.
			 */
			@Override
			public Resource transform(HttpServletRequest request, Resource resource) {
				return resource;
			}
		};
	}

	private static final class NoOpSwaggerWelcome extends SwaggerWelcomeCommon {

		/**
		 * Helper for no-op swagger welcome.
		 */
		private NoOpSwaggerWelcome(
				SwaggerUiConfigProperties swaggerUiConfig,
				SpringDocConfigProperties springDocConfigProperties) {
			super(swaggerUiConfig, springDocConfigProperties);
		}

		/**
		 * Helper for calculate UI root path.
		 */
		@Override
		protected void calculateUiRootPath(SwaggerUiConfigParameters swaggerUiConfigParameters,
				StringBuilder... stringBuilders) {
		}

		/**
		 * Helper for build api doc url.
		 */
		@Override
		protected void buildApiDocUrl(SwaggerUiConfigParameters swaggerUiConfigParameters) {
		}

		/**
		 * Helper for build url with context path.
		 */
		@Override
		protected String buildUrlWithContextPath(
				SwaggerUiConfigParameters swaggerUiConfigParameters,
				String contextPath) {
			return "";
		}

		/**
		 * Helper for build swagger config url.
		 */
		@Override
		protected void buildSwaggerConfigUrl(SwaggerUiConfigParameters swaggerUiConfigParameters) {
		}

		/**
		 * Helper for build from current context path.
		 */
		@Override
		protected void buildFromCurrentContextPath(
				SwaggerUiConfigParameters swaggerUiConfigParameters,
				HttpServletRequest request) {
		}
	}

	private static final class NullFilenameResource extends AbstractResource {

		private final byte[] content;

		/**
		 * Helper for null filename resource.
		 */
		private NullFilenameResource(String body) {
			this.content = body.getBytes(StandardCharsets.UTF_8);
		}

		/**
		 * Helper for get description.
		 */
		@Override
		public String getDescription() {
			return "In-memory resource with no filename";
		}

		/**
		 * Helper for get input stream.
		 */
		@Override
		public InputStream getInputStream() {
			return new ByteArrayInputStream(content);
		}

		/**
		 * Helper for get filename.
		 */
		@Override
		public String getFilename() {
			return null;
		}

		/**
		 * Helper for get url.
		 */
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
