package com.example.roombooking.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptStatementFailedException;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BookingEquipmentSqlTest {
    @Test
    void freshSchemaEnforcesForeignKeysAndRequiredQuantity() throws Exception {
        DataSource database = database();
        schema(database);
        execute(database, "INSERT INTO bookings VALUES (41, 'CANCELLED', 'fixture')",
                "INSERT INTO equipment VALUES (73, 'Fixture', 'fixture', 5)",
                "INSERT INTO booking_equipment (booking_id,equipment_id,quantity) VALUES (41,73,1)");

        assertThrows(SQLException.class, () -> execute(database,
                "INSERT INTO booking_equipment (booking_id,equipment_id,quantity) VALUES (999,73,1)"));
        assertThrows(SQLException.class, () -> execute(database,
                "INSERT INTO booking_equipment (booking_id,equipment_id,quantity) VALUES (41,999,1)"));
        assertThrows(SQLException.class, () -> execute(database,
                "INSERT INTO booking_equipment (booking_id,equipment_id,quantity) VALUES (41,73,NULL)"));
        assertEquals(1, number(database, "SELECT COUNT(*) FROM booking_equipment"));
    }

    @Test
    void upgradingAnExistingTablePreservesRowsAndCanBeRepeated() throws Exception {
        DataSource database = database();
        legacyTable(database);
        execute(database, "INSERT INTO bookings VALUES (41, 'CANCELLED', 'fixture')",
                "INSERT INTO equipment VALUES (73, 'Fixture', 'fixture', 5)",
                "INSERT INTO booking_equipment (booking_id,equipment_id,quantity) VALUES (41,73,2)");
        schema(database);
        schema(database);

        assertEquals(2, number(database, "SELECT quantity FROM booking_equipment WHERE booking_id=41 AND equipment_id=73"));
        assertThrows(SQLException.class, () -> execute(database,
                "UPDATE booking_equipment SET quantity=0"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NULL", "0", "-1"})
    void migrationRejectsInvalidExistingQuantitiesWithoutDeletingRows(String quantity) throws Exception {
        DataSource database = database();
        legacyTable(database);
        execute(database, "INSERT INTO booking_equipment (booking_id,equipment_id,quantity) VALUES (41,73," + quantity + ")");

        // A missing resource must not be mistaken for a rejected database migration.
        assertThrows(ScriptStatementFailedException.class, () -> schema(database));
        assertEquals(1, number(database, "SELECT COUNT(*) FROM booking_equipment"));
    }

    @Test
    void migrationPreservesExistingEquipmentLinksWhenAddingIndexes() throws Exception {
        DataSource database = database();
        legacyTable(database);
        execute(database, "INSERT INTO booking_equipment (booking_id,equipment_id,quantity) VALUES (41,73,1),(41,73,2)");

        schema(database);
        assertEquals(2, number(database, "SELECT COUNT(*) FROM booking_equipment"));
        assertEquals(3, number(database, "SELECT SUM(quantity) FROM booking_equipment"));
    }

    @Test
    void seedWithoutExplicitParentsDoesNothing() throws Exception {
        DataSource database = database();
        schema(database);
        seed(database);
        assertEquals(0, number(database, "SELECT COUNT(*) FROM booking_equipment"));
    }

    @Test
    void seedUsesRealFixtureIdsAndIsIdempotent() throws Exception {
        DataSource database = database();
        schema(database);
        fixtures(database, "CANCELLED");
        seed(database);
        seed(database);

        assertEquals(1, number(database, "SELECT COUNT(*) FROM booking_equipment WHERE booking_id=41 AND equipment_id=73 AND quantity=1"));
        assertEquals(1, number(database, "SELECT COUNT(*) FROM bookings"));
        assertEquals(1, number(database, "SELECT COUNT(*) FROM equipment"));
    }

    @Test
    void seedDoesNotAttachEquipmentToAnActiveBooking() throws Exception {
        DataSource database = database();
        schema(database);
        fixtures(database, "PENDING");
        seed(database);
        assertEquals(0, number(database, "SELECT COUNT(*) FROM booking_equipment"));
    }

    @Test
    void seedDoesNothingWhenFixtureParentsAreAmbiguous() throws Exception {
        DataSource database = database();
        schema(database);
        fixtures(database, "CANCELLED");
        execute(database, "INSERT INTO bookings VALUES (42,'CANCELLED','[nuttachai_673380581-8_04-demo] equipment-link fixture')");
        seed(database);
        assertEquals(0, number(database, "SELECT COUNT(*) FROM booking_equipment"));
    }

    private DataSource database() throws SQLException {
        DataSource database = new DriverManagerDataSource("jdbc:h2:mem:nuttachai_673380581-8_04-sql-" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        execute(database, "CREATE TABLE bookings (id BIGINT PRIMARY KEY,status VARCHAR(20),purpose VARCHAR(255))",
                "CREATE TABLE equipment (id BIGINT PRIMARY KEY,name VARCHAR(100),category VARCHAR(50),total_quantity INTEGER)");
        return database;
    }

    private void legacyTable(DataSource database) throws SQLException {
        execute(database, "CREATE TABLE booking_equipment (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                + "booking_id BIGINT NOT NULL,equipment_id BIGINT NOT NULL,quantity INTEGER)");
    }

    private void fixtures(DataSource database, String status) throws SQLException {
        execute(database, "INSERT INTO bookings VALUES (41,'" + status + "','[nuttachai_673380581-8_04-demo] equipment-link fixture')",
                "INSERT INTO equipment VALUES (73,'nuttachai_673380581-8_04 demo equipment','nuttachai_673380581-8_04-demo',5)");
    }

    private void schema(DataSource database) throws SQLException { script(database, "schema.sql"); }
    private void seed(DataSource database) throws SQLException { script(database, "data.sql"); }

    private void script(DataSource database, String name) throws SQLException {
        try (var connection = database.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/nuttachai_673380581-8_04/" + name));
        }
    }

    private void execute(DataSource database, String... sql) throws SQLException {
        try (var connection = database.getConnection(); var statement = connection.createStatement()) {
            for (String command : sql) statement.execute(command);
        }
    }

    private long number(DataSource database, String sql) throws SQLException {
        try (var connection = database.getConnection(); var statement = connection.createStatement();
             var rows = statement.executeQuery(sql)) {
            assertTrue(rows.next());
            return rows.getLong(1);
        }
    }
}
