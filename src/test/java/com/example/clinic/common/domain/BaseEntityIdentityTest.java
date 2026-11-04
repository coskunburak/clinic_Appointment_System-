package com.example.clinic.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseEntityIdentityTest {

    private static class ConcreteEntityA extends BaseEntity {
        ConcreteEntityA(UUID id, Instant createdAt) {
            super(id, createdAt);
        }

        ConcreteEntityA() {
            super();
        }
    }

    private static class ConcreteEntityB extends BaseEntity {
        ConcreteEntityB(UUID id, Instant createdAt) {
            super(id, createdAt);
        }
    }

    @Test
    @DisplayName("Positive: Two entities of the same type with identical UUID are equal")
    void shouldBeEqualWhenSameTypeAndId() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        ConcreteEntityA e1 = new ConcreteEntityA(id, now);
        ConcreteEntityA e2 = new ConcreteEntityA(id, now.plusSeconds(10)); // different timestamp, same identity

        assertThat(e1).isEqualTo(e2);
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
    }

    @Test
    @DisplayName("Negative: Two entities of different concrete types with identical UUID are NOT equal")
    void shouldNotBeEqualAcrossDifferentEntityTypes() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        ConcreteEntityA a = new ConcreteEntityA(id, now);
        ConcreteEntityB b = new ConcreteEntityB(id, now);

        assertThat(a).isNotEqualTo(b);
        assertThat(b).isNotEqualTo(a);
    }

    @Test
    @DisplayName("Negative: Entities with different IDs are not equal")
    void shouldNotBeEqualWithDifferentIds() {
        ConcreteEntityA e1 = new ConcreteEntityA(UUID.randomUUID(), Instant.now());
        ConcreteEntityA e2 = new ConcreteEntityA(UUID.randomUUID(), Instant.now());

        assertThat(e1).isNotEqualTo(e2);
    }

    @Test
    @DisplayName("Negative: Two unsaved entities with null IDs are NOT equal")
    void shouldNotBeEqualWhenBothIdsAreNull() {
        ConcreteEntityA e1 = new ConcreteEntityA();
        ConcreteEntityA e2 = new ConcreteEntityA();

        assertThat(e1.getId()).isNull();
        assertThat(e2.getId()).isNull();
        assertThat(e1).isNotEqualTo(e2);
    }

    @Test
    @DisplayName("Positive: HashSet membership remains stable across property reads and hashCode calculations")
    void shouldMaintainStableHashSetMembership() {
        UUID id = UUID.randomUUID();
        ConcreteEntityA entity = new ConcreteEntityA(id, Instant.now());

        Set<ConcreteEntityA> set = new HashSet<>();
        set.add(entity);

        assertThat(set).contains(entity);
        assertThat(set.contains(new ConcreteEntityA(id, Instant.now()))).isTrue();
    }

    @Test
    @DisplayName("Negative: BaseEntity constructor rejects null id or createdAt")
    void shouldRejectNullIdentityOrTimestamp() {
        assertThatThrownBy(() -> new ConcreteEntityA(null, Instant.now()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("id must not be null");

        assertThatThrownBy(() -> new ConcreteEntityA(UUID.randomUUID(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("createdAt must not be null");
    }
}
