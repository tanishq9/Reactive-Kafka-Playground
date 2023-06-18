package com.example.reactivekafkaplayground.sec13.producer;

import com.example.reactivekafkaplayground.sec13.OrderEvent;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ProducerRunner implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(ProducerRunner.class);

	@Autowired
	public ReactiveKafkaProducerTemplate<String, OrderEvent> template;

	@Override
	public void run(String... args) throws Exception {
		this.orderEventFlux()
				.flatMap(orderEvent -> this.template.send("order-events", orderEvent.getOrderId().toString(), orderEvent))
				.doOnNext(senderResult -> logger.info("result: {}", senderResult.recordMetadata()))
				.subscribe();
	}

	private Flux<OrderEvent> orderEventFlux() {
		return Flux.interval(Duration.ofMillis(500))
				.take(1000)
				.map(i -> new OrderEvent(
						UUID.randomUUID(),
						i,
						LocalDateTime.now()
				));
	}
}
