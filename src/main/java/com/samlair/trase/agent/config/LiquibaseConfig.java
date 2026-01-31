package com.samlair.trase.agent.config;

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

/**
 * Runs Liquibase using Spring's resource resolution for nested jar compatibility.
 */
@Configuration
public class LiquibaseConfig {

	@Bean
	public SpringLiquibase liquibase(DataSource dataSource, ResourceLoader resourceLoader) {
		SpringLiquibase liquibase = new SpringLiquibase();
		liquibase.setDataSource(dataSource);
		liquibase.setChangeLog("classpath:rdbms/changelog.yml");
		liquibase.setResourceLoader(resourceLoader);
		return liquibase;
	}
}
