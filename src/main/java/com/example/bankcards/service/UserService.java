package com.example.bankcards.service;

import com.example.bankcards.dto.user.ChangePasswordRequest;
import com.example.bankcards.dto.user.RegisterUserRequest;
import com.example.bankcards.dto.user.UpdateUserRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.exception.DuplicateUserException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.repository.UserRepository;
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

    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    public UserDto registerUser(RegisterUserRequest request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new DuplicateUserException();
        }

        var user = mapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.USER);
        repository.save(user);

        return mapper.toDto(user);
    }

    public List<UserDto> getAllUsers(String sortBy) {
        if (!Set.of("name", "email").contains(sortBy))
            sortBy = "name";

        return repository.findAll(Sort.by(sortBy))
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    public UserDto getUser(Long id) {
        var user = repository.findById(id).orElseThrow(UserNotFoundException::new);
        return mapper.toDto(user);
    }

    public UserDto updateUser(Long id, UpdateUserRequest request) {
        var user = repository.findById(id).orElseThrow(UserNotFoundException::new);

        mapper.update(request, user);
        repository.save(user);

        return mapper.toDto(user);
    }

    public void deleteUser(Long id) {
        var user = repository.findById(id).orElseThrow(UserNotFoundException::new);
        repository.delete(user);
    }

    public void changePassword(Long id, ChangePasswordRequest request) {
        var user = repository.findById(id).orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AccessDeniedException("Password does not match");
        }
        user.setPassword(request.getNewPassword());
        repository.save(user);
    }
}
