package com.example.productservice.service;

import com.example.productservice.event.ProductViewEvent;
import lombok.AllArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.kafka.sender.SenderRecord;

@AllArgsConstructor
public class ProductViewEventProducer {

	private static final Logger logger = LoggerFactory.getLogger(ProductViewEventProducer.class);

	private final ReactiveKafkaProducerTemplate<String, ProductViewEvent> template;

	private final Sinks.Many<ProductViewEvent> sink;

	private final Flux<ProductViewEvent> productViewEventFlux;

	private final String topic;

	public void subscribe() {
		var senderRecordFlux = this.productViewEventFlux
				.map(productViewEvent -> new ProducerRecord<>(topic, productViewEvent.getProductId().toString(), productViewEvent))
				.map(producerRecord -> SenderRecord.create(producerRecord, producerRecord.key()));

		this.template
				.send(senderRecordFlux)
				.doOnNext(r -> logger.info("Emitted event: {}", r.correlationMetadata()))
				.subscribe();
	}

	public void emitEvent(ProductViewEvent productViewEvent) {
		this.sink.tryEmitNext(productViewEvent);
	}
}
