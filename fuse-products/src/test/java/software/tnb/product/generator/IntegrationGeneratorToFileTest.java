package software.tnb.product.generator;

import static org.junit.jupiter.api.Assertions.fail;

import static org.assertj.core.api.Assertions.assertThat;

import software.tnb.common.config.TestConfiguration;
import software.tnb.common.product.ProductType;
import software.tnb.common.utils.IOUtils;
import software.tnb.product.customizer.Customizer;
import software.tnb.product.integration.Resource;
import software.tnb.product.integration.builder.AbstractIntegrationBuilder;
import software.tnb.product.integration.builder.IntegrationBuilder;
import software.tnb.product.integration.generator.IntegrationGenerator;
import software.tnb.product.routebuilder.DummyRouteBuilder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Tag("unit")
public class IntegrationGeneratorToFileTest extends AbstractIntegrationGeneratorTest {
    private static final Path TEST_DIR = new File("./target/integrationGeneratorTest").toPath();

    @BeforeEach
    public void createDir() {
        try {
            Files.createDirectories(TEST_DIR.resolve("src/main/resources"));
        } catch (IOException e) {
            fail("Unable to create directory", e);
        }
    }

    @AfterEach
    public void deleteDir() {
        try {
            FileUtils.deleteDirectory(TEST_DIR.toFile());
        } catch (IOException e) {
            fail("Unable to delete directory", e);
        }
    }

    @Override
    public String process(AbstractIntegrationBuilder<?> ib) {
        IntegrationGenerator.createFiles(ib, TEST_DIR);
        return null;
    }

    @Override
    @Test
    public void shouldProcessRouteBuilderTest() {
        setProduct(ProductType.CAMEL_SPRINGBOOT);

        IntegrationBuilder ib = dummyIb();
        process(ib);

        final Path expectedPath = TEST_DIR
            .resolve("src/main/java/")
            .resolve(TestConfiguration.appGroupId().replaceAll("\\.", "/"))
            .resolve(DummyRouteBuilder.class.getSimpleName() + ".java");

        assertThat(expectedPath).exists();
        assertThat(ib.getRouteBuilders()).hasSize(1);
        assertThat(expectedPath).content().isEqualTo(ib.getRouteBuilders().get(0).toString());
    }

    @Test
    public void shouldCreatePropertiesFileTest() {
        setProduct(ProductType.CAMEL_QUARKUS);
        final String key = "Hello";
        final String value = "world";

        IntegrationBuilder ib = dummyIb().addToProperties(key, value);
        process(ib);

        final Path expectedPath = TEST_DIR.resolve("src/main/resources/application.properties");

        assertThat(expectedPath).exists();
        assertThat(expectedPath).content().contains(key + "=" + value);
    }

    @Override
    @Test
    public void shouldProcessAdditionalClassesTest() {
        setProduct(ProductType.CAMEL_SPRINGBOOT);
        IntegrationBuilder ib = builderWithAdditionalClass();
        final String classContent = ib.getAdditionalClasses().get(0).toString();

        process(ib);

        final Path expectedPath = TEST_DIR
            .resolve("src/main/java/")
            .resolve(StringUtils.substringBetween(classContent, "package ", ";").replaceAll("\\.", "/"))
            .resolve("AddedClass.java");

        assertThat(expectedPath).exists();
        assertThat(expectedPath).content().isEqualTo(classContent);
    }

    @Test
    public void shouldProcessResourcesTest() {
        setProduct(ProductType.CAMEL_SPRINGBOOT);
        final String resourceName = "my-file.txt";
        final String resourceContent = "File content";
        final Path expectedPath = TEST_DIR.resolve("src/main/resources").resolve(resourceName);

        process(dummyIb().addResource(new Resource(resourceName, resourceContent)));

        assertThat(expectedPath).exists();
        assertThat(expectedPath).content().isEqualTo(resourceContent);
    }

    @Test
    public void shouldAddResourcesToQuarkusPropertyTest() {
        setProduct(ProductType.CAMEL_QUARKUS);
        final String resourceName = "my-file.txt";

        IntegrationBuilder ib = dummyIb().addResource(new Resource(resourceName, ""));

        process(ib);

        assertThat(ib.getApplicationProperties()).containsKey("quarkus.native.resources.includes");
        assertThat(ib.getApplicationProperties().get("quarkus.native.resources.includes")).isEqualTo(resourceName);
    }

    @Test
    public void shouldAddImportForAdditionalClassTest() {
        setProduct(ProductType.CAMEL_SPRINGBOOT);
        IntegrationBuilder ib = builderWithAdditionalClass();

        IntegrationGenerator.createFiles(ib, TEST_DIR);

        final Path routeBuilderPath = TEST_DIR
            .resolve("src/main/java/")
            .resolve(TestConfiguration.appGroupId().replaceAll("\\.", "/"))
            .resolve(DummyRouteBuilder.class.getSimpleName() + ".java");
        assertThat(IOUtils.readFile(routeBuilderPath)).contains("import software.tnb.product.generator.AddedClass;");
    }

    @Test
    public void shouldCreateResourceAddedInCustomizerTest() {
        setProduct(ProductType.CAMEL_SPRINGBOOT);
        final String name = "customizerResource";
        final String content = "customizerResourceContent";
        Customizer c = new Customizer() {
            @Override
            public void customize() {
                getIntegrationBuilder().addResource(new Resource(name, content));
            }
        };
        process(dummyIb().addCustomizer(c));
        Path expectedFile = TEST_DIR.resolve("src/main/resources").resolve(name);
        assertThat(expectedFile).exists();
        assertThat(IOUtils.readFile(expectedFile)).isEqualTo(content);
    }
}
