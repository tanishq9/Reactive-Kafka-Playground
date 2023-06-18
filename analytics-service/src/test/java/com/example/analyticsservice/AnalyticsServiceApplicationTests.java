package com.example.analyticsservice;

import com.example.analyticsservice.dto.ProductTrendingDto;
import com.example.analyticsservice.event.ProductViewEvent;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.kafka.sender.SenderRecord;
import reactor.test.StepVerifier;

@AutoConfigureWebTestClient
class AnalyticsServiceApplicationTests extends AbstractIT {

	@Autowired
	private WebTestClient client;

	@Test
	void trendingTest() {
		// emit events by mimicking producer
		var events = Flux.just(
				createEvent(2, 2),
				createEvent(5, 5),
				createEvent(4, 2),
				createEvent(6, 3),
				createEvent(3, 3)
		).flatMap(Flux::fromIterable)
				.map(e -> SenderRecord.create(
						new ProducerRecord<>("product-view-events", e.getProductId().toString(), e), e.getProductId()
				));

		var resultFlux = this.<ProductViewEvent>createSender()
				.send(events)
				.doOnNext(event -> System.out.println("Produced: " + event.toString()));

		StepVerifier.create(resultFlux)
				.expectNextCount(15)
				.verifyComplete();

		// verify via trending endpoint
		var mono = this.client
				.get()
				.uri("/trending")
				.accept(MediaType.TEXT_EVENT_STREAM)
				.exchange()
				.returnResult(new ParameterizedTypeReference<List<ProductTrendingDto>>() {
				})
				.getResponseBody()
				.next(); // only receive the first event

		StepVerifier.create(mono)
				.consumeNextWith(this::validateResult)
				.verifyComplete();

	}

	private void validateResult(List<ProductTrendingDto> list) {
		Assertions.assertEquals(5, list.size());
		Assertions.assertTrue(list.stream().noneMatch(p -> p.getProductId() == 1));
	}

	private List<ProductViewEvent> createEvent(int productId, int count) {
		return IntStream.rangeClosed(1, count)
				.mapToObj(i -> new ProductViewEvent((productId)))
				.collect(Collectors.toList());
	}

}
