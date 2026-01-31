package com.samlair.trase.agent.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestIdFilterUnitTest {

	private final RequestIdFilter filter = new RequestIdFilter();

	@Test
	void generatesRequestIdWhenMissing() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		when(request.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).thenReturn(null);

		AtomicReference<String> captured = new AtomicReference<>();
		FilterChain chain = (req, res) -> captured.set(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));

		filter.doFilterInternal(request, response, chain);

		assertTrue(captured.get() != null && !captured.get().isBlank());
		verify(response).setHeader(eq(RequestIdFilter.REQUEST_ID_HEADER), eq(captured.get()));
		assertNull(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));
	}

	@Test
	void reusesProvidedRequestId() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		when(request.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).thenReturn("req-123");

		AtomicReference<String> captured = new AtomicReference<>();
		FilterChain chain = (req, res) -> captured.set(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));

		filter.doFilterInternal(request, response, chain);

		assertEquals("req-123", captured.get());
		verify(response).setHeader(eq(RequestIdFilter.REQUEST_ID_HEADER), eq("req-123"));
		assertNull(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));
	}

	@Test
	void generatesRequestIdWhenBlank() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		when(request.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).thenReturn("   ");

		AtomicReference<String> captured = new AtomicReference<>();
		FilterChain chain = (req, res) -> captured.set(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));

		filter.doFilterInternal(request, response, chain);

		assertTrue(captured.get() != null && !captured.get().isBlank());
		verify(response).setHeader(eq(RequestIdFilter.REQUEST_ID_HEADER), eq(captured.get()));
		assertNull(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));
	}
}
