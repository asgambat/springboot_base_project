package com.example.msbaseprj.api.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.example.msbaseprj.api.user.model.UserDto;
import com.example.msbaseprj.entity.User;

/**
 * MapStruct mapper between the {@link User} JPA entity and the {@link UserDto}
 * API model. The implementation is generated at compile time (see the
 * {@code mapstruct-processor} annotation processor configured in
 * {@code pom.xml}) and registered as a Spring bean, so it can be injected like
 * any other component. {@code unmappedTargetPolicy = ERROR} makes the build
 * fail if any target property is left unmapped.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {

	UserDto toDto(User user);
}
