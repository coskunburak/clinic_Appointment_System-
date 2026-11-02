# Resmî teknik kaynaklar ve sürüm notu

**Araştırma tarihi:** 2026-11-02. S00 ve S01 denetimlerinde teyit edilen üretim seviyesi kütüphane sürümleri ve koordinatları:

| Teknoloji / konu | Seçilen sürüm ve koordinat | URL |
|---|---|---|
| JDK | Java 21 LTS (Eclipse Temurin / OpenJDK 21) | https://www.oracle.com/java/technologies/downloads/ |
| Spring Boot | 3.4.2 resmî referans / Java 21 uyumluluğu | https://docs.spring.io/spring-boot/system-requirements.html |
| Boot starter isimleri | `spring-boot-starter-web`, `spring-boot-starter-validation`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-data-jpa`, `spring-boot-starter-actuator` | https://docs.spring.io/spring-boot/reference/using/build-systems.html |
| PostgreSQL | 17 (Docker `postgres:17-alpine`) & JDBC `org.postgresql:postgresql` | https://www.postgresql.org/docs/17/ |
| Flyway | `org.flywaydb:flyway-core`, `org.flywaydb:flyway-database-postgresql` | https://documentation.red-gate.com/fd |
| Test Altyapısı | `spring-boot-starter-test`, `org.testcontainers:postgresql`, `org.springframework.boot:spring-boot-testcontainers` | https://java.testcontainers.org/modules/databases/postgres/ |
| Java JDK API | `java.time`, `java.math.BigDecimal`, `java.util.UUID` | https://docs.oracle.com/en/java/javase/21/docs/api/ |
| Spring Security | S04'te eklenecek | https://docs.spring.io/spring-security/reference/ |
| Maven Wrapper | 3.9.9 (`mvnw`, `mvnw.cmd`) | https://maven.apache.org/guides/ |
| OWASP ASVS | 5.0.0 stable güvenlik yaklaşımı | https://asvs.owasp.org/ |

## Lisans / Dağıtım Notu
Java 21 LTS sürümünde Eclipse Temurin açık kaynak OpenJDK dağıtımı hedeflenmiştir. CI ve yerel geliştirme Temurin 21 üzerinde hizalanmıştır.

## Spring Boot Sürüm Uzlaşısı (Version Reconciliation)
Eski planlama dokümanlarında geçen Boot 4.1.2 referansına karşılık, yerel depo ve S00 kapanışında kararlı çalışan Spring Boot 3.4.2 LTS sürümü korunmuştur. Bağımlılık adları (`flyway-database-postgresql`, `spring-boot-testcontainers`, `spring-boot-starter-web`) Boot 3.4.2 uyumlu yapılandırılmıştır.
