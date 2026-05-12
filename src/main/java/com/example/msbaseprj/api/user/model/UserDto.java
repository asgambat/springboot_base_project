package com.example.msbaseprj.api.user.model;

import com.example.msbaseprj.api.user.model.validator.AllUpperCase;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserDto(
    @NotBlank(message = "{name.notblank}")
    String name,

    @AllUpperCase(message = "{email.uppercase}")
    @NotBlank(message = "{email.notblank}")
    @Email(message = "{email.invalid}")
    String email
) {}
