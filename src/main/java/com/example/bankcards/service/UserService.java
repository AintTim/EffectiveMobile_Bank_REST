package com.example.bankcards.service;

import com.example.bankcards.dto.user.ChangePasswordRequest;
import com.example.bankcards.dto.user.RegisterUserRequest;
import com.example.bankcards.dto.user.UpdateUserRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.DuplicateUserException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
public class UserService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "email");
    private static final String DEFAULT_SORT_FIELD = "name";

    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDto registerUser(RegisterUserRequest request) {
        validateEmailUniqueness(request.getEmail());

        var user = mapper.toEntity(request);
        encodeAndSetUserPassword(user, request.getPassword());
        user.setRole(Role.USER);

        var savedUser = repository.save(user);
        return mapper.toDto(savedUser);
    }

    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        var user = findUserById(id);
        mapper.update(request, user);

        var updatedUser = repository.save(user);

        return mapper.toDto(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        var user = findUserById(id);
        repository.delete(user);
    }

    @Transactional
    public void changePassword(Long id, ChangePasswordRequest request) {
        var user = findUserById(id);
        validateOldPassword(user, request.getOldPassword());

        encodeAndSetUserPassword(user, request.getNewPassword());
        repository.save(user);
    }

    public List<UserDto> getAllUsers(String sortBy) {
        var sortFields = validateAndGetSortField(sortBy);
        var sort = Sort.by(sortFields);

        return repository.findAll(sort).stream()
                .map(mapper::toDto)
                .toList();
    }

    public UserDto getUserDto(Long id) {
        var user = findUserById(id);
        return mapper.toDto(user);
    }

    public User findUserById(Long userId) {
        return repository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }

    private void validateEmailUniqueness(String email) {
        if (repository.existsByEmail(email)) {
            throw new DuplicateUserException();
        }
    }

    private void validateOldPassword(User user, String oldPassword) {
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new AccessDeniedException("Password does not match");
        }
    }

    private String validateAndGetSortField(String sortBy) {
        if (sortBy == null || !ALLOWED_SORT_FIELDS.contains(sortBy.toLowerCase())) {
            return DEFAULT_SORT_FIELD;
        }
        return sortBy;
    }

    private void encodeAndSetUserPassword(User user, String rawPassword) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        user.setPassword(encodedPassword);
    }
}
