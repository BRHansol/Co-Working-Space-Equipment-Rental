package com.example.roombooking.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Preserve existing local accounts when the User entity's table changes to users. */
@Configuration
@Profile("local & !postgres")
public class LocalUserTableMigrationConfig {
    private static final String MIGRATION_BEAN = "localUserTableMigration";
    private static final Logger log = LoggerFactory.getLogger(LocalUserTableMigrationConfig.class);

    @Bean
    InitializingBean localUserTableMigration(DataSource dataSource) {
        return () -> migrate(dataSource);
    }

    @Bean
    static BeanFactoryPostProcessor migrateUsersBeforeHibernate() {
        return beanFactory -> {
            if (beanFactory.containsBeanDefinition("entityManagerFactory")) {
                var definition = beanFactory.getBeanDefinition("entityManagerFactory");
                List<String> dependencies = new ArrayList<>();
                if (definition.getDependsOn() != null) {
                    dependencies.addAll(Arrays.asList(definition.getDependsOn()));
                }
                dependencies.add(MIGRATION_BEAN);
                definition.setDependsOn(dependencies.toArray(String[]::new));
            }
        };
    }

    static void migrate(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (!"H2".equals(connection.getMetaData().getDatabaseProductName())) {
                return;
            }
            String schema = connection.getSchema();
            String legacyTable = null;
            String currentTable = null;
            try (ResultSet tables = connection.getMetaData().getTables(null, schema, "%", null)) {
                while (tables.next()) {
                    String name = tables.getString("TABLE_NAME");
                    if ("user".equalsIgnoreCase(name)) legacyTable = name;
                    if ("users".equalsIgnoreCase(name)) currentTable = name;
                }
            }
            if (legacyTable == null) return;
            String qualifiedLegacyTable = quote(schema) + "." + quote(legacyTable);
            try (Statement statement = connection.createStatement()) {
                if (currentTable != null) {
                    throw new SQLException("Local H2 contains both user and users tables. "
                            + "Stop the application and restore the pre-merge database backup before retrying; "
                            + "no account data or foreign keys have been overwritten.");
                }
                String targetName = connection.getMetaData().storesUpperCaseIdentifiers() ? "USERS" : "users";
                statement.execute("ALTER TABLE " + qualifiedLegacyTable + " RENAME TO " + quote(targetName));
                log.info("Migrated local H2 user table to users; existing accounts and references are preserved");
            }
        }
    }

    private static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
