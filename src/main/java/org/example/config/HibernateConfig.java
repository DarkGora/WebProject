package org.example.config;

import lombok.extern.log4j.Log4j2;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;
@Log4j2
@Configuration
@ComponentScan(basePackages = "org.example")
@EnableTransactionManagement
public class HibernateConfig {

    @Bean
    public DataSource dataSource() {
        log.info("Creating DataSource for PostgreSQL");
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl("jdbc:postgresql://localhost:5444/db");
        dataSource.setUsername("user");
        dataSource.setPassword("12345");
        return dataSource;
    }

    @Bean
    public LocalSessionFactoryBean sessionFactory(DataSource dataSource) {
        log.info("Creating SessionFactory with DataSource: {}", dataSource);
        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setPackagesToScan("org.example.model");
        sessionFactory.setHibernateProperties(hibernateProperties());
        return sessionFactory;
    }

    private Properties hibernateProperties() {
        log.info("Configuring Hibernate properties");
        Properties props = new Properties();
        props.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        props.setProperty("hibernate.show_sql", "true");
        props.setProperty("hibernate.format_sql", "true");
        props.setProperty("hibernate.hbm2ddl.auto", "validate");
        props.setProperty("hibernate.current_session_context_class", "thread");
        props.setProperty("hibernate.jdbc.batch_size", "20");
        props.setProperty("hibernate.order_inserts", "true");
        props.setProperty("hibernate.order_updates", "true");
        return props;
    }

    @Bean
    public HibernateTransactionManager transactionManager(SessionFactory sessionFactory) {
        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        transactionManager.setSessionFactory(sessionFactory);
        transactionManager.setNestedTransactionAllowed(true);
        return transactionManager;
    }
}