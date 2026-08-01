import org.springframework.cloud.contract.spec.Contract

/**
 * Consumer-driven contract for the outbox webhook receiver. It pins the request shape a webhook
 * consumer relies on (POST /api/demo/webhook/events with the Idempotency-Key and X-Event-Type
 * headers and a JSON body) and the expected 204 No Content response. Spring Cloud Contract turns
 * this into a producer test and into a reusable stub for consumer-side tests.
 */
Contract.make {
	description "should accept an outbox webhook event and return 204 No Content"
	request {
		method POST()
		url "/api/demo/webhook/events"
		headers {
			contentType applicationJson()
			header "Idempotency-Key", "event-123"
			header "X-Event-Type", "OrderCreated"
		}
		body([
			orderId: 42
		])
	}
	response {
		status NO_CONTENT()
	}
}
