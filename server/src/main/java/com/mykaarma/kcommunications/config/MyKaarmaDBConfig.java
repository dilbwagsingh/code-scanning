package com.mykaarma.kcommunications.config;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
		basePackages = {"com.mykaarma.kcommunications.model.jpa", "com.mykaarma.kcommunications.jpa.repository"}, 
		entityManagerFactoryRef = "mkEntityManagerFactory", 
		transactionManagerRef = "mkTransactionManager"
		)
public class MyKaarmaDBConfig {

	@Primary
	@Bean(name = "mkDataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource.mykaarma")
    public DataSourceProperties mkDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = "mkDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.mykaarma")
    public DataSource mkDataSource() {
        return mkDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Primary
    @Bean(name = "mkEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean mkEntityManagerFactory(EntityManagerFactoryBuilder builder,
    		@Qualifier("mkDataSource") DataSource mkDataSource) {
        return builder
        		.dataSource(mkDataSource)
        		.packages("com.mykaarma.kcommunications.model.jpa", "com.mykaarma.kcommunications.jpa.repository")
        		.persistenceUnit("mk").build();
    }

    @Primary
    @Bean(name = "mkTransactionManager")
    public PlatformTransactionManager mkTransactionManager(@Qualifier("mkEntityManagerFactory") EntityManagerFactory mkEntityManagerFactory) {
        return new JpaTransactionManager(mkEntityManagerFactory);
    }

}