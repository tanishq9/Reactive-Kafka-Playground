package com.example.productservice;

import com.example.productservice.event.ProductViewEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

@AutoConfigureWebTestClient
class ProductServiceApplicationTests extends AbstractIT {

	@Autowired
	private WebTestClient client;

	@Test
	void productViewAndEventTest() {
		// view products
		viewProductSuccess(1);
		viewProductSuccess(1);
		viewProductError(1000);
		viewProductSuccess(5);

		// check if the events are emitted
		var flux = this.<ProductViewEvent>createReceiver("product-view-events")
				.receive()
				.take(3);

		StepVerifier.create(flux)
				.consumeNextWith(e -> Assertions.assertEquals(1, e.value().getProductId()))
				.consumeNextWith(e -> Assertions.assertEquals(1, e.value().getProductId()))
				.consumeNextWith(e -> Assertions.assertEquals(5, e.value().getProductId()))
				.verifyComplete();
	}

	private void viewProductSuccess(int id) {
		client
				.get()
				.uri("/product/" + id)
				.exchange()
				.expectStatus().is2xxSuccessful()
				.expectBody()
				.jsonPath("$.id").isEqualTo(id)
				.jsonPath("$.description").isEqualTo("product-" + id);
	}

	private void viewProductError(int id) {
		client
				.get()
				.uri("/product/" + id)
				.exchange()
				.expectStatus().is4xxClientError();
	}
}
