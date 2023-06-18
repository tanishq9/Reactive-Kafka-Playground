package com.example.reactivekafkaplayground;

import com.example.reactivekafkaplayground.sec13.OrderEvent;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.test.StepVerifier;

@TestPropertySource(properties = "app=consumer")
@ExtendWith(OutputCaptureExtension.class)
public class OrderEventConsumerTest extends AbstractIT {

	private static final Logger logger = LoggerFactory.getLogger(OrderEventConsumerTest.class);

	@Test
	public void consumerTest(CapturedOutput output) {
		// Get test producer which would produce events for actual/real consumer
		KafkaSender<String, OrderEvent> kafkaSender = createSender();

		UUID randomUUID = UUID.randomUUID();
		Mono<SenderRecord<String, OrderEvent, String>> senderRecordFlux =
				Mono.just(new ProducerRecord<String, OrderEvent>("order-events", "1", new OrderEvent(randomUUID, 1, LocalDateTime.now())))
						.map(producerRecord -> SenderRecord.create(producerRecord, producerRecord.key()))
						.doOnNext(senderRecord -> logger.info("Produced event with value: " + senderRecord.value()));

		Mono<Void> senderResultFlux = kafkaSender
				.send(senderRecordFlux)
				.then(Mono.delay(Duration.ofSeconds(3))) // Wait for 3 seconds more after events are produced
				.then();

		// StepVerifier is used just to subscribe to the senderResultFlux
		StepVerifier.create(senderResultFlux)
				// check if test producer has produced events - not required.
				// how to check whether actual consumer has consumed those? See below assertion.
				//.expectNextCount(1)
				.verifyComplete();

		Assertions.assertTrue(output.getOut().contains(randomUUID.toString()));
	}
}
