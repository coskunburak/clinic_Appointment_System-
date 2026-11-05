package com.example.clinic.catalog.domain;

import com.example.clinic.organization.domain.Organization;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceDefinitionTest {

    @Test
    @DisplayName("Should create valid service definition under organization")
    void shouldCreateValidServiceDefinition() {
        Organization org = new Organization("Acıbadem Sağlık");
        ServiceDefinition def = new ServiceDefinition(org, "Genel Muayene", "Rutin hekim kontrolü");

        assertThat(def.getId()).isNotNull();
        assertThat(def.getOrganization()).isEqualTo(org);
        assertThat(def.getName()).isEqualTo("Genel Muayene");
        assertThat(def.getDescription()).isEqualTo("Rutin hekim kontrolü");
        assertThat(def.isActive()).isTrue();
        assertThat(def.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Negative: should reject null organization")
    void shouldRejectNullOrganization() {
        assertThatThrownBy(() -> new ServiceDefinition(null, "Genel Muayene", "Açıklama"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Organization must not be null");
    }

    @Test
    @DisplayName("Negative: should reject null or blank service definition name")
    void shouldRejectBlankName() {
        Organization org = new Organization("Medical Park");

        assertThatThrownBy(() -> new ServiceDefinition(org, null, "Açıklama"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");

        assertThatThrownBy(() -> new ServiceDefinition(org, "   ", "Açıklama"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    @DisplayName("Should rename service definition with valid non-blank string")
    void shouldRenameServiceDefinition() {
        Organization org = new Organization("Memorial");
        ServiceDefinition def = new ServiceDefinition(org, "Eski Ad", "Açıklama");

        def.rename("Yeni Hizmet Adı");
        assertThat(def.getName()).isEqualTo("Yeni Hizmet Adı");

        assertThatThrownBy(() -> def.rename("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    @DisplayName("Should update description and handle null description")
    void shouldChangeDescription() {
        Organization org = new Organization("Medipol");
        ServiceDefinition def = new ServiceDefinition(org, "Kardiyoloji Muayenesi", "İlk açıklama");

        def.changeDescription("Güncellenmiş detaylı açıklama");
        assertThat(def.getDescription()).isEqualTo("Güncellenmiş detaylı açıklama");

        def.changeDescription(null);
        assertThat(def.getDescription()).isNull();
    }

    @Test
    @DisplayName("Should manage active lifecycle transitions explicitly")
    void shouldManageLifecycle() {
        Organization org = new Organization("Florence Nightingale");
        ServiceDefinition def = new ServiceDefinition(org, "Laboratuvar Testi", null);

        assertThat(def.isActive()).isTrue();
        def.deactivate();
        assertThat(def.isActive()).isFalse();

        def.activate();
        assertThat(def.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should verify entity equality based on BaseEntity UUID identity")
    void shouldFollowBaseEntityEquality() {
        UUID id = UUID.randomUUID();
        Organization org = new Organization("Şişli Etfal");

        ServiceDefinition def1 = new ServiceDefinition(id, org, "Hizmet 1", "Desc", true, java.time.Instant.now());
        ServiceDefinition def2 = new ServiceDefinition(id, org, "Hizmet 1 Farklı", "Desc 2", false, java.time.Instant.now());

        assertThat(def1).isEqualTo(def2);
        assertThat(def1.hashCode()).isEqualTo(def2.hashCode());
    }
}
