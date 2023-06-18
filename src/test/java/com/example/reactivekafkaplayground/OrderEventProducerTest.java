package com.example.reactivekafkaplayground;

import com.example.reactivekafkaplayground.sec13.OrderEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.test.StepVerifier;

@TestPropertySource(properties = "app=producer")
public class OrderEventProducerTest extends AbstractIT {

	private static final Logger logger = LoggerFactory.getLogger(OrderEventProducerTest.class);

	// Mock consumer and actual producer is used to test behaviour of the actual producer
	@Test
	public void producerTest() {
		// Get test consumer which would consume events from actual/real producer
		KafkaReceiver<String, OrderEvent> kafkaReceiver = createReceiver("order-events");

		Flux<ReceiverRecord<String, OrderEvent>> receiverRecordFlux = kafkaReceiver
				.receive()
				.take(10)
				.doOnNext(record -> logger.info("key: {}, value: {}", record.key(), record.value()));
		// don't subscribe to above, StepVerifier would do that

		StepVerifier.create(receiverRecordFlux) // whatever is passed in .create, that is subscribed, hence the events in reactive pipeline are consumed
				// check if test consumer has received events produced as per logic by real producer
				.consumeNextWith(record -> Assertions.assertNotNull(record.value().getOrderId()))
				.expectNextCount(9)
				.verifyComplete();
		// The above is just one way of asserting in reactive pipeline, thenConsumeWhile could be used as well to assert something on every item
	}
}
