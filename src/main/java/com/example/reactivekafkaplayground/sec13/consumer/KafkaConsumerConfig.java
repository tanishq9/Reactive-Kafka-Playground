package com.example.reactivekafkaplayground.sec13.consumer;

import com.example.reactivekafkaplayground.sec13.OrderEvent;
import java.util.List;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import reactor.kafka.receiver.ReceiverOptions;

@Configuration
public class KafkaConsumerConfig {

	@Bean
	// This kafkaProperties object is same what is mentioned in application.yaml
	public ReceiverOptions<String, OrderEvent> receiverOptions(KafkaProperties kafkaProperties) {
		return ReceiverOptions.<String, OrderEvent>create(kafkaProperties.buildConsumerProperties())
				.consumerProperty(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS, "false") // by default, type id is removed after deserialization
				//.consumerProperty(JsonDeserializer.USE_TYPE_INFO_HEADERS, false)
				//.consumerProperty(JsonDeserializer.VALUE_DEFAULT_TYPE, "someCustomClass")
				.subscription(List.of("order-events"));
	}

	@Bean
	public ReactiveKafkaConsumerTemplate<String, OrderEvent> consumerTemplate(ReceiverOptions<String, OrderEvent> options) {
		return new ReactiveKafkaConsumerTemplate<>(options);
	}
}
