package com.mykaarma.kcommunications.config;


import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
		basePackages = { "com.mykaarma.kcommunications.communications.model.jpa", "com.mykaarma.kcommunications.communications.repository" }, 
		entityManagerFactoryRef = "communicationsEntityManagerFactory", 
		transactionManagerRef = "communicationsTransactionManager")
public class CommunicationsDBConfig {

	@Bean(name = "communicationsDataSourceProperties")
	@ConfigurationProperties(prefix = "spring.datasource.communications")
	public DataSourceProperties communicationsDataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean(name = "communicationsDataSource")
	@ConfigurationProperties(prefix = "spring.datasource.communications")
	public DataSource communicationsDataSource() {
		return communicationsDataSourceProperties().initializeDataSourceBuilder().build();
	}

	@Bean(name = "communicationsEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean communicationsEntityManagerFactory(
			EntityManagerFactoryBuilder builder,
			@Qualifier("communicationsDataSource") DataSource communicationsDataSource) {
		return builder
				.dataSource(communicationsDataSource)
				.packages("com.mykaarma.kcommunications.communications.model.jpa", "com.mykaarma.kcommunications.communications.repository")
				.persistenceUnit("communications")
				.build();
	}

	@Bean(name = "communicationsTransactionManager")
	public PlatformTransactionManager communicationsTransactionManager(
			@Qualifier("communicationsEntityManagerFactory") EntityManagerFactory communicationsEntityManagerFactory) {
		return new JpaTransactionManager(communicationsEntityManagerFactory);
	}

}