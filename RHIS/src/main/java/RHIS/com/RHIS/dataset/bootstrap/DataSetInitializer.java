package RHIS.com.RHIS.dataset.bootstrap;

import RHIS.com.RHIS.auth.role.RoleEntity;
import RHIS.com.RHIS.auth.role.RoleRepository;
import RHIS.com.RHIS.auth.user.UserEntity;
import RHIS.com.RHIS.auth.user.UserRepository;
import RHIS.com.RHIS.dataset.entity.DataSetEntity;
import RHIS.com.RHIS.dataset.entity.DataSetField;
import RHIS.com.RHIS.dataset.model.DataSetFieldType;
import RHIS.com.RHIS.dataset.repository.DataSetFieldRepository;
import RHIS.com.RHIS.dataset.repository.DataSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class DataSetInitializer implements ApplicationRunner {

    private static final String PRODUCT_SCHEMA = "public";

    private static final String TABLES_QUERY = """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = ?
              AND table_type = 'BASE TABLE'
                AND  lower(table_name)  like lower('rhis%')
            ORDER BY table_name
            """;
    private static final String COLUMNS_QUERY = """
            SELECT
                c.column_name,
                c.ordinal_position,
                c.data_type,
                c.udt_name,
                c.is_nullable,
                EXISTS (
                    SELECT 1
                    FROM information_schema.table_constraints tc
                    JOIN information_schema.key_column_usage kcu
                      ON kcu.constraint_catalog = tc.constraint_catalog
                     AND kcu.constraint_schema = tc.constraint_schema
                     AND kcu.constraint_name = tc.constraint_name
                    WHERE tc.constraint_type = 'PRIMARY KEY'
                      AND tc.table_schema = c.table_schema
                      AND tc.table_name = c.table_name
                      AND kcu.column_name = c.column_name
                ) AS primary_key
            FROM information_schema.columns c
            WHERE c.table_schema = ?
              AND c.table_name = ?
            ORDER BY c.ordinal_position
            """;

    private final JdbcTemplate jdbcTemplate;
    private final DataSetRepository dataSetRepository;
    private final DataSetFieldRepository dataSetFieldRepository;
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;


    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        List<String> tableNames = findTables(PRODUCT_SCHEMA);

        synchronizeDataSets(tableNames);

        // bootstrapping users

        var roleAdmin = createRole("ROLE_ADMIN");
        if (userRepository.findByEmail("test@gmail.com")
                .isEmpty()) {
            UserEntity userEntity = new UserEntity();
            userEntity.setEmail("test@gmail.com");
            userEntity.setPassword(passwordEncoder.encode("testtest"));
            userEntity.getRoles()
                    .add(roleAdmin);
            userRepository.save(userEntity);

        }


        if (userRepository.findByEmail("naaymyh@gmail.com")
                .isEmpty()) {
            UserEntity student = new UserEntity();
            student.setEmail("naaymyh@gmail.com");

            student.setPassword(passwordEncoder.encode("testtest"));
            student.getRoles()
                    .add(roleAdmin);
            userRepository.save(student);
        }


    }

    private void synchronizeDataSets(List<String> tableNames) {
        Set<String> discoveredTableNames = Set.copyOf(tableNames);
        Map<String, DataSetEntity> existingDataSets = dataSetRepository.findAll().stream()
                .collect(Collectors.toMap(DataSetEntity::getSourceName, Function.identity()));

        for (String tableName : tableNames) {
            try {
                List<ColumnInfo> columns = findColumns(PRODUCT_SCHEMA, tableName);
                DataSetEntity dataSet = existingDataSets.get(tableName);

                if (dataSet == null) {
                    dataSet = dataSetRepository.save(
                            new DataSetEntity(formatDisplayName(tableName), tableName)
                    );
                } else {
                    dataSet.setActive(true);
                    dataSet = dataSetRepository.save(dataSet);
                }

                synchronizeFields(dataSet, columns);
                log.info("Synchronized DataSet '{}' with {} fields.",
                        dataSet.getDisplayName(), columns.size());
            } catch (IllegalStateException e) {
                log.warn("Skipping table '{}': {}", tableName, e.getMessage());
            }
        }

        existingDataSets.values().stream()
                .filter(dataSet -> !discoveredTableNames.contains(dataSet.getSourceName()))
                .forEach(dataSet -> {
                    dataSet.setActive(false);
                    dataSetFieldRepository
                            .findByDataset_IdOrderByPositionAsc(dataSet.getId())
                            .forEach(field -> field.setActive(false));
                });
        dataSetRepository.saveAll(existingDataSets.values());
    }

    private void synchronizeFields(DataSetEntity dataSet, List<ColumnInfo> columns) {
        Map<String, DataSetField> existingFields = dataSetFieldRepository
                .findByDataset_IdOrderByPositionAsc(dataSet.getId())
                .stream()
                .collect(Collectors.toMap(DataSetField::getSourceName, Function.identity()));

        Set<String> discoveredColumnNames = columns.stream()
                .map(ColumnInfo::sourceName)
                .collect(Collectors.toSet());

        List<DataSetField> synchronizedFields = columns.stream()
                .map(column -> synchronizeField(dataSet, existingFields.get(column.sourceName()), column))
                .collect(Collectors.toList());

        List<DataSetField> removedFields = existingFields.values().stream()
                .filter(field -> !discoveredColumnNames.contains(field.getSourceName()))
                .peek(field -> field.setActive(false))
                .toList();

        synchronizedFields.addAll(removedFields);
        dataSetFieldRepository.saveAll(synchronizedFields);
    }

    private DataSetField synchronizeField(
            DataSetEntity dataSet,
            DataSetField existingField,
            ColumnInfo column
    ) {
        DataSetField field = existingField;
        if (field == null) {
            field = new DataSetField();
            field.setDataset(dataSet);
            field.setSourceName(column.sourceName());
            field.setDisplayName(columnDisplayName(column.sourceName()));
            field.setVisible(true);
        }

        field.setPosition(column.position());
        field.setDataType(column.dataType());
        field.setNullable(column.nullable());
        field.setPrimaryKey(column.primaryKey());
        field.setActive(true);
        return field;
    }

    private RoleEntity createRole(String roleName) {

        var role = roleRepository.findByName(roleName)
                .orElseGet(() ->
                        roleRepository.save(RoleEntity.builder()
                                .name(roleName)
                                .build())

                );
        return role;


    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private List<String> findTables(String schemaName) {
        return jdbcTemplate.queryForList(TABLES_QUERY
                ,
                String.class,
                schemaName
        );
    }

    private List<ColumnInfo> findColumns(String schemaName, String tableName) {
        List<ColumnInfo> columns = jdbcTemplate.query(
                COLUMNS_QUERY,
                (resultSet, rowNumber) -> new ColumnInfo(
                        resultSet.getString("column_name"),
                        resultSet.getInt("ordinal_position"),
                        DataSetFieldType.fromPostgreSql(
                                resultSet.getString("data_type"),
                                resultSet.getString("udt_name")
                        ),
                        "YES".equalsIgnoreCase(resultSet.getString("is_nullable")),
                        resultSet.getBoolean("primary_key")
                ),
                schemaName,
                tableName
        );

        if (columns.isEmpty()) {
            throw new IllegalStateException(
                    "La table obligatoire " + schemaName + "." + tableName + " est introuvable."
            );
        }

        return columns;
    }

    /**
     * Converts snake_case table/column names to Title Case display names.
     * Example: "product_category" → "Product Category"
     */
    private String formatDisplayName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        if (name.equals("rhis_shift")) {
            return "Plannig";
        }

        String[] parts = name.split("_");

        StringBuilder sb = new StringBuilder();

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];

            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return sb.toString().trim();
    }

    private String columnDisplayName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }

        String[] parts = name.split("_");

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return sb.toString().trim();
    }

    // -------------------------------------------------------------------------
    // Inner record
    // -------------------------------------------------------------------------

    private record ColumnInfo(
            String sourceName,
            int position,
            DataSetFieldType dataType,
            boolean nullable,
            boolean primaryKey
    ) {
    }


}
