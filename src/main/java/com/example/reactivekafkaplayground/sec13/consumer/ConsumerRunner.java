package com.example.reactivekafkaplayground.sec13.consumer;

import com.example.reactivekafkaplayground.sec13.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Service;

@Service
public class ConsumerRunner implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(ConsumerRunner.class);

	@Autowired
	private ReactiveKafkaConsumerTemplate<String, OrderEvent> reactiveKafkaConsumerTemplate;

	@Override
	public void run(String... args) throws Exception {
		this.reactiveKafkaConsumerTemplate
				.receive()
				.doOnNext(receiverRecord -> receiverRecord.headers().forEach(header -> logger.info("Header key: {}, Header value: {}", header.key(), new String(header.value()))))
				.doOnNext(receiverRecord -> logger.info("key: {}, value:{}", receiverRecord.key(), receiverRecord.value()))
				.subscribe();
	}
}
