package com.orderflow.auth.repository;

import com.orderflow.auth.entity.Role;
import com.orderflow.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UserRepositoryTest {

    @Autowired UserRepository repo;

    @Test
    void findByEmail_returnsUser() {
        repo.save(new User("César", "cesar@email.com", "hash", Role.USER));
        assertThat(repo.findByEmail("cesar@email.com")).isPresent();
        assertThat(repo.findByEmail("nope@email.com")).isEmpty();
    }

    @Test
    void existsByEmail() {
        repo.save(new User("César", "cesar@email.com", "hash", Role.USER));
        assertThat(repo.existsByEmail("cesar@email.com")).isTrue();
        assertThat(repo.existsByEmail("other@email.com")).isFalse();
    }

    @Test
    void uniqueEmailConstraint() {
        repo.save(new User("A", "dup@email.com", "hash", Role.USER));
        assertThatThrownBy(() -> {
            repo.saveAndFlush(new User("B", "dup@email.com", "hash", Role.USER));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
