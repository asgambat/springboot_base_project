package com.example.msbaseprj.api.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.msbaseprj.api.user.model.UserDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @PostMapping
    public UserDto createUser(@Valid @RequestBody UserDto userDto) {
        return userDto;
    }

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable @Min(1) Long id) {
        return new UserDto( "User" + id, "user" + id + "@example.com");
    }

}
