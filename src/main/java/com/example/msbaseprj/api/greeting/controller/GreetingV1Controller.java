package com.example.msbaseprj.api.greeting.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.msbaseprj.api.greeting.model.GreetingV1;

/**
 * Version 1 of the greeting resource, exposed under the {@code /api/v1} URI
 * prefix. URI-based versioning keeps each version explicit and cacheable; a new
 * major version is introduced as a new path so existing clients are never
 * broken by additive or breaking changes.
 */
@RestController
@RequestMapping("/api/v1/greetings")
public class GreetingV1Controller {

	@GetMapping
	public GreetingV1 greet(@RequestParam(value = "name", defaultValue = "World") String name) {
		return new GreetingV1("Hello, " + name + "!");
	}
}
