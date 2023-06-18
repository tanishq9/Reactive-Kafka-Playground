package com.example.productservice.config;

import com.example.productservice.event.ProductViewEvent;
import com.example.productservice.service.ProductViewEventProducer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.core.publisher.Sinks;
import reactor.kafka.sender.SenderOptions;

@Configuration
public class KafkaProducerConfig {

	@Bean
	public SenderOptions<String, ProductViewEvent> senderOptions(KafkaProperties kafkaProperties) {
		return SenderOptions.create(kafkaProperties.buildProducerProperties());
	}

	@Bean
	public ReactiveKafkaProducerTemplate<String, ProductViewEvent> reactiveKafkaProducerTemplate(SenderOptions<String, ProductViewEvent> senderOptions) {
		return new ReactiveKafkaProducerTemplate<String, ProductViewEvent>(senderOptions);
	}

	@Bean
	public ProductViewEventProducer productViewEventProducer(ReactiveKafkaProducerTemplate<String, ProductViewEvent> template) {
		// Sinks.many because we would be continuing to emit events
		// unicast because we have only 1 subscriber i.e. kafka sender
		var sink = Sinks.many().unicast().<ProductViewEvent>onBackpressureBuffer();
		var flux = sink.asFlux();

		var eventProducer = new ProductViewEventProducer(template, sink, flux, "product-view-events");
		eventProducer.subscribe();

		return eventProducer;
	}
}
