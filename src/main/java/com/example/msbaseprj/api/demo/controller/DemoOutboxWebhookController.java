package com.example.msbaseprj.api.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("demo")
@RequestMapping("/api/demo/webhook")
public class DemoOutboxWebhookController {
	private static final Logger log = LoggerFactory.getLogger(DemoOutboxWebhookController.class);

	@PostMapping("/events")
	public ResponseEntity<Void> receiveEvent(@RequestHeader("Idempotency-Key") String idempotencyKey,
			@RequestHeader("X-Event-Type") String eventType, @RequestBody String payload) {
		log.info("Demo webhook received event type={} idempotencyKey={} payload={}", eventType, idempotencyKey,
				payload);
		return ResponseEntity.noContent().build();
	}
}