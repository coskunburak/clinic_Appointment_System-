package com.example.clinic.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DomainArchitectureTest {

    private static final List<String> FORBIDDEN_PACKAGES = List.of(
            "org.springframework",
            "jakarta.persistence",
            "org.hibernate",
            "java.sql",
            "javax.sql"
    );

    @Test
    @DisplayName("Architecture: Pure domain packages have zero imports from Spring, JPA, Hibernate, or JDBC")
    void pureDomainCoreMustNotDependOnFrameworks() throws IOException {
        Path projectRoot = Paths.get("").toAbsolutePath();
        Path srcMainJava = projectRoot.resolve("src/main/java/com/example/clinic");

        List<Path> pureDomainDirs = List.of(
                srcMainJava.resolve("booking/domain"),
                srcMainJava.resolve("scheduling/domain")
        );

        for (Path dir : pureDomainDirs) {
            assertThat(dir).exists();
            try (Stream<Path> files = Files.walk(dir)) {
                List<Path> javaFiles = files.filter(p -> p.toString().endsWith(".java")).toList();
                assertThat(javaFiles).isNotEmpty();

                for (Path javaFile : javaFiles) {
                    List<String> lines = Files.readAllLines(javaFile);
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.startsWith("import ")) {
                            for (String forbidden : FORBIDDEN_PACKAGES) {
                                assertThat(trimmed)
                                        .withFailMessage("Pure domain class %s contains forbidden import: %s",
                                                javaFile.getFileName(), trimmed)
                                        .doesNotContain("import " + forbidden);
                            }
                        }
                    }
                }
            }
        }

        // Check specific pure value objects in common.domain
        List<Path> pureCommonFiles = List.of(
                srcMainJava.resolve("common/domain/Money.java"),
                srcMainJava.resolve("common/domain/TimeInterval.java")
        );

        for (Path javaFile : pureCommonFiles) {
            assertThat(javaFile).exists();
            List<String> lines = Files.readAllLines(javaFile);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("import ")) {
                    for (String forbidden : FORBIDDEN_PACKAGES) {
                        assertThat(trimmed)
                                .withFailMessage("Pure value object %s contains forbidden import: %s",
                                        javaFile.getFileName(), trimmed)
                                .doesNotContain("import " + forbidden);
                    }
                }
            }
        }
    }
}
