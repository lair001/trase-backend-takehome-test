package com.samlair.trase.agent;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class TraseAgentApplicationUnitTest {

	@Test
	void mainDelegatesToSpringApplicationRun() {
		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			springApplication.when(() -> SpringApplication.run(any(Class.class), any(String[].class)))
					.thenReturn(mock(org.springframework.context.ConfigurableApplicationContext.class));

			TraseAgentApplication.main(new String[] {"--test"});

			springApplication.verify(() -> SpringApplication.run(TraseAgentApplication.class, new String[] {"--test"}),
					times(1));
		}
	}
}
