package com.Odin.FirstTask.service;

import com.Odin.FirstTask.dto.CreateUserRequest;
import com.Odin.FirstTask.dto.UserResponseDto;
import com.Odin.FirstTask.exception.UserNotFoundException;
import com.Odin.FirstTask.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {

    private final Map<Long, User> userDatabase = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public UserResponseDto createUser(CreateUserRequest request) {
        // Business logic assumes validated payload.
        // If controller validation checks are missing, bad inputs trigger unhandled exceptions here:

        // 1. Boundary check: age must be at least 18
        if (request.getAge() == null || request.getAge() < 18) {
            throw new IllegalArgumentException("User must be at least 18 years old; received: " + request.getAge());
        }

        // 2. Email validation (throws NullPointerException if email is null)
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (!normalizedEmail.contains("@")) {
            throw new IllegalArgumentException("Invalid email format: " + request.getEmail());
        }

        // 3. Name check (throws NullPointerException if name is null)
        if (request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be empty");
        }

        // 4. Address check (throws NullPointerException if address is null)
        String city = request.getAddress().getCity();
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City cannot be empty");
        }

        long id = idGenerator.getAndIncrement();
        User user = User.builder()
                .id(id)
                .name(request.getName().trim())
                .email(normalizedEmail)
                .age(request.getAge())
                .role(request.getRole() != null ? request.getRole() : "USER")
                .address(request.getAddress())
                .createdAt(LocalDateTime.now())
                .build();

        userDatabase.put(id, user);
        return mapToDto(user);
    }

    public UserResponseDto getUserById(Long id) {
        User user = userDatabase.get(id);
        if (user == null) {
            throw new UserNotFoundException("User not found with id: " + id);
        }
        return mapToDto(user);
    }

    public List<UserResponseDto> getAllUsers() {
        List<UserResponseDto> list = new ArrayList<>();
        for (User user : userDatabase.values()) {
            list.add(mapToDto(user));
        }
        return list;
    }

    private UserResponseDto mapToDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .age(user.getAge())
                .role(user.getRole())
                .address(user.getAddress())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
