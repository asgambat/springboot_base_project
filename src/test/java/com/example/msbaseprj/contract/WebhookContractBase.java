package com.example.msbaseprj.contract;

import org.junit.jupiter.api.BeforeEach;

import com.example.msbaseprj.api.demo.controller.DemoOutboxWebhookController;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

/**
 * Base class for the Spring Cloud Contract generated producer tests. The plugin
 * generates, at build time, JUnit 5 tests (in
 * {@code target/generated-test-sources}) that extend this class, one per
 * contract under {@code src/test/resources/contracts}. Each generated test
 * fires the request described by the contract against the real controller and
 * asserts the response, so the producer cannot drift from the published
 * contract without failing the build.
 *
 * <p>
 * The webhook receiver has no collaborators, so a lightweight RestAssured
 * standalone MockMvc setup is enough and avoids booting the whole Spring
 * context.
 */
public abstract class WebhookContractBase {

	@BeforeEach
	void setup() {
		RestAssuredMockMvc.standaloneSetup(new DemoOutboxWebhookController());
	}
}
