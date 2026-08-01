package com.example.msbaseprj.api.greeting.controller;

import java.util.Locale;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.msbaseprj.api.greeting.model.GreetingV2;

/**
 * Version 2 of the greeting resource, exposed under the {@code /api/v2} URI
 * prefix. It returns an enriched representation ({@link GreetingV2}) while the
 * version 1 endpoint keeps serving its original contract, so clients can
 * migrate at their own pace.
 */
@RestController
@RequestMapping("/api/v2/greetings")
public class GreetingV2Controller {

	@GetMapping
	public GreetingV2 greet(@RequestParam(value = "name", defaultValue = "World") String name,
			@RequestParam(value = "language", defaultValue = "en") String language) {
		var normalizedLanguage = language.toLowerCase(Locale.ROOT);
		return new GreetingV2("Hello, " + name + "!", normalizedLanguage, "v2");
	}
}
