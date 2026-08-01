package com.example.msbaseprj.api.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.msbaseprj.api.user.mapper.UserMapper;
import com.example.msbaseprj.api.user.model.UserDto;
import com.example.msbaseprj.api.user.model.UserIdResponse;
import com.example.msbaseprj.repository.UserRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/user")
public class UserController {
	private final UserRepository userRepository;
	private final UserMapper userMapper;

	public UserController(UserRepository userRepository, UserMapper userMapper) {
		this.userRepository = userRepository;
		this.userMapper = userMapper;
	}

	@PostMapping
	public UserDto createUser(@Valid @RequestBody UserDto userDto) {
		return userDto;
	}

	@GetMapping
	public ResponseEntity<UserIdResponse> getUserIdByEmail(@RequestParam @Email String email) {
		return ResponseEntity.of(userRepository.findByEmail(email).map(user -> new UserIdResponse(user.getId())));
	}

	@GetMapping("/{id}")
	public ResponseEntity<UserDto> getUser(@PathVariable @Min(1) Long id) {
		return ResponseEntity.of(userRepository.findById(id).map(userMapper::toDto));
	}

}
