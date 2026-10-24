package com.orderflow.auth.service;

import com.orderflow.auth.dto.UserResponse;
import com.orderflow.auth.entity.Role;
import com.orderflow.auth.entity.User;
import com.orderflow.auth.exception.ForbiddenOperationException;
import com.orderflow.auth.exception.UserNotFoundException;
import com.orderflow.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return userRepository.findAll().stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public UserResponse updateRole(Long id, Role newRole, boolean callerIsSuperAdmin) {
        if (!callerIsSuperAdmin) {
            throw new ForbiddenOperationException(
                    "Apenas o administrador original pode alterar o role de outros usuários");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        if (user.isSuperAdmin()) {
            throw new ForbiddenOperationException(
                    "O role do administrador original não pode ser alterado");
        }
        user.setRole(newRole);
        return UserResponse.from(userRepository.save(user));
    }
}
