package com.example.bankcards.mapper;

import com.example.bankcards.dto.user.RegisterUserRequest;
import com.example.bankcards.dto.user.UpdateUserRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);

    User toEntity(RegisterUserRequest request);

    void update(UpdateUserRequest request, @MappingTarget User user);
}
