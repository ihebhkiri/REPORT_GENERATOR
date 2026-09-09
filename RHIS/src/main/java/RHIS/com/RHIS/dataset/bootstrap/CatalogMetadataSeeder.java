package RHIS.com.RHIS.dataset.bootstrap;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

@Component
@Order(10)
@RequiredArgsConstructor
public class CatalogMetadataSeeder implements ApplicationRunner {
    private final DataSource dataSource;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var script = new ResourceDatabasePopulator(new ClassPathResource("db/catalog-metadata-seed.sql"));
        script.setSqlScriptEncoding("UTF-8");
        script.execute(dataSource);
    }
}
