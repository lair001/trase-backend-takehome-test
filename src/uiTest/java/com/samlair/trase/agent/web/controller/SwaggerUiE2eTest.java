package com.samlair.trase.agent.web.controller;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.samlair.trase.agent.IntegrationTestBase;
import java.net.URI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerUiE2eTest extends IntegrationTestBase {

	@Test
	void quickAuthDropdownAuthorizesAndExecutesRequest() {
		try (Playwright playwright = Playwright.create()) {
			Browser browser = playwright.chromium()
					.launch(new BrowserType.LaunchOptions().setHeadless(true));
			Page page = browser.newPage();
			String baseUrl = "http://localhost:" + getPort();

			page.navigate(baseUrl + "/swagger-ui/index.html");
			page.waitForSelector("#swagger-ui", new Page.WaitForSelectorOptions().setTimeout(10_000));
			page.waitForSelector("#swagger-quick-auth-user",
					new Page.WaitForSelectorOptions().setTimeout(10_000));
			page.waitForFunction("() => window.ui && window.ui.preauthorizeApiKey");

			page.selectOption("#swagger-quick-auth-user", "admin");
			page.click("#swagger-quick-auth-btn");
			page.waitForFunction("() => {"
					+ "const el = document.getElementById('swagger-quick-auth-status');"
					+ "return el && el.textContent.includes('Authorized as admin');"
					+ "}");

			page.waitForSelector("div.opblock",
					new Page.WaitForSelectorOptions().setTimeout(10_000).setState(WaitForSelectorState.ATTACHED));
			Locator opblocks = page.locator("div.opblock");
			Locator opblock = null;
			int count = opblocks.count();
			for (int i = 0; i < count; i++) {
				Locator candidate = opblocks.nth(i);
				Locator methodLocator = candidate.locator(".opblock-summary-method");
				Locator pathLocator = candidate.locator(".opblock-summary-path");
				if (methodLocator.count() == 0 || pathLocator.count() == 0) {
					continue;
				}
				String method = methodLocator.first().textContent();
				String path = pathLocator.first().textContent();
				if (method != null && path != null
						&& method.trim().equalsIgnoreCase("get")
						&& path.trim().equals("/agents")) {
					opblock = candidate;
					break;
				}
			}
			assertThat(opblock).as("Expected GET /agents opblock").isNotNull();
			opblock.waitFor(new Locator.WaitForOptions().setTimeout(10_000)
					.setState(WaitForSelectorState.ATTACHED));
			Locator summary = opblock.locator(".opblock-summary-control");
			summary.waitFor(new Locator.WaitForOptions().setTimeout(10_000)
					.setState(WaitForSelectorState.ATTACHED));
			summary.click(new Locator.ClickOptions().setForce(true));

			Locator tryItOut = opblock.locator("button.try-out__btn");
			tryItOut.waitFor(new Locator.WaitForOptions().setTimeout(10_000)
					.setState(WaitForSelectorState.ATTACHED));
			tryItOut.click(new Locator.ClickOptions().setForce(true));

			Locator execute = opblock.locator("button.execute");
			execute.waitFor(new Locator.WaitForOptions().setTimeout(10_000)
					.setState(WaitForSelectorState.ATTACHED));
			Response response = page.waitForResponse(
					resp -> {
						if (!"GET".equalsIgnoreCase(resp.request().method())) {
							return false;
						}
						URI uri = URI.create(resp.url());
						return "/agents".equals(uri.getPath());
					},
					() -> execute.click(new Locator.ClickOptions().setForce(true))
			);
			assertThat(response.status()).isEqualTo(200);
			browser.close();
		}
	}
}
