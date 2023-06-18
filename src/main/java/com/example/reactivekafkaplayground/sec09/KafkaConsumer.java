package com.example.reactivekafkaplayground.sec09;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.GroupedFlux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;

/*
 * goal: using flatMap WITH ordering using GroupedFlux
 * */
// If we are doing parallel processing using flatMap for the events being consumed and want to achieve ordering as well then we can use groupBy based on something.
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

		ReceiverOptions<String, String> subscription = ReceiverOptions.<String, String>create(serversConfig)
				//.consumerProperty(ConsumerConfig.GROUP_ID_CONFIG, "demo-grp");
				.commitInterval(Duration.ofSeconds(1))
				.subscription(List.of("order-events")); // topic to subscribe

		// The scheduler for below uses non-daemon thread so we don't need to block the thread (Thread.sleep) to keep it running
		KafkaReceiver.create(subscription)
				.receive()
				.groupBy(rr -> Integer.parseInt(rr.key()) % 5) // for example
				//.groupBy(rr -> rr.partition()) // for example
				.flatMap(KafkaConsumer::batchProcess, 256)
				.subscribe();
	}

	private static Mono<Void> batchProcess(GroupedFlux<Integer, ReceiverRecord<String, String>> groupedFlux) {
		return groupedFlux
				.publishOn(Schedulers.boundedElastic()) // if any blocking operation is done in reactive pipeline
				.doFirst(() -> logger.info("Flux starting, mod is: " + groupedFlux.key())) // groupedFlux.key() -> after grouping what is the key of this flux
				.doOnNext(receiverRecord -> logger.info("topic: {}, partition: {}, key: {}, value: {}", receiverRecord.topic(), receiverRecord.partition(), receiverRecord.key(), receiverRecord.value()))
				.doOnNext(receiverRecord -> receiverRecord.receiverOffset().acknowledge()) // ack the event
				.then(Mono.delay(Duration.ofSeconds(1)))
				.then();
	}
	// record, message and event all are analogous to each other
}
