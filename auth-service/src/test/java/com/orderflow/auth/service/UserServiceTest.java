package com.orderflow.auth.service;

import com.orderflow.auth.dto.UserResponse;
import com.orderflow.auth.entity.Role;
import com.orderflow.auth.entity.User;
import com.orderflow.auth.exception.ForbiddenOperationException;
import com.orderflow.auth.exception.UserNotFoundException;
import com.orderflow.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository repo;
    @InjectMocks UserService service;

    private User user(Long id, String name, Role role, boolean superAdmin) {
        var u = new User(name, name.toLowerCase() + "@e.com", "hash", role);
        u.setId(id);
        u.setSuperAdmin(superAdmin);
        return u;
    }

    @Test
    void listAll_returnsSortedByName() {
        when(repo.findAll()).thenReturn(List.of(
                user(2L, "Bob", Role.USER, false),
                user(1L, "Alice", Role.ADMIN, false),
                user(3L, "Carlos", Role.USER, false)
        ));
        List<UserResponse> rows = service.listAll();
        assertThat(rows).extracting(UserResponse::name).containsExactly("Alice", "Bob", "Carlos");
    }

    @Test
    void updateRole_bySuperAdmin_succeeds() {
        var target = user(7L, "Maria", Role.USER, false);
        when(repo.findById(7L)).thenReturn(Optional.of(target));
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse resp = service.updateRole(7L, Role.ADMIN, true);
        assertThat(resp.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void updateRole_byNonSuperAdmin_throws403() {
        assertThatThrownBy(() -> service.updateRole(7L, Role.ADMIN, false))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(repo, never()).findById(any());
    }

    @Test
    void updateRole_targetIsSuperAdmin_throws403() {
        var target = user(1L, "Original", Role.ADMIN, true);
        when(repo.findById(1L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.updateRole(1L, Role.USER, true))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void updateRole_targetNotFound_throws404() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateRole(99L, Role.ADMIN, true))
                .isInstanceOf(UserNotFoundException.class);
    }
}
