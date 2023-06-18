package com.example.reactivekafkaplayground.sec08;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

/*
 * goal: using concatMap and flatMap for consuming events for auto-ACK scenarios
 * */
// Consuming from Multiple Topics
public class KafkaConsumer {

	private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

	public static void main(String[] args) {

		// Consumer Properties
		Map<String, Object> serversConfig = Map.of(
				ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
				ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
				ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
				ConsumerConfig.GROUP_ID_CONFIG, "inventory-service-group",
				ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest", // To consume all UnACKed messages from earliest or from the last ACKed offset
				// By default consumption starts from the last ACKed offset
				ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, "1",
				ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 3 // default is 500
		);

		ReceiverOptions<Object, Object> subscription = ReceiverOptions.create(serversConfig)
				//.consumerProperty(ConsumerConfig.GROUP_ID_CONFIG, "demo-grp");
				.commitInterval(Duration.ofSeconds(1))
				.subscription(List.of("order-events")); // topic to subscribe

		// The scheduler for below uses non-daemon thread so we don't need to block the thread (Thread.sleep) to keep it running
		KafkaReceiver.create(subscription)
				//.receive()
				.receiveAutoAck()
				.log()
				// subscribe to individual flux
				//.concatMap(KafkaConsumer::batchProcess)
				// 256 is the default number of Flux(s) which can be subscribed at the same time
				.flatMap(KafkaConsumer::batchProcess, 256)
				.subscribe();
	}

	private static Mono<Void> batchProcess(Flux<ConsumerRecord<Object, Object>> recordFlux) {
		return recordFlux
				.doFirst(() -> logger.info("Flux starting"))
				.doOnNext(record -> logger.info("topic: {}, key: {}, value: {}", record.topic(), record.key(), record.value()))
				.then(Mono.delay(Duration.ofSeconds(1)))
				.then();
	}
	// record, message and event all are analogous to each other
}
