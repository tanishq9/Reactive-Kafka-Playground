package com.example.reactivekafkaplayground.sec10;

import com.example.reactivekafkaplayground.sec03.KafkaConsumer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.util.retry.Retry;

public class KafkaConsumerErrorSimulateV1 {

	private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

	public static void main(String[] args) {

		// Consumer Properties
		Map<String, Object> serversConfig = Map.of(
				ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
				ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
				ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
				ConsumerConfig.GROUP_ID_CONFIG, "demo-group",
				// ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest", // To consume all UnACKed messages from earliest or from the last ACKed offset
				// By default consumption starts from the last ACKed offset
				ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, "1"
		);

		ReceiverOptions<Object, Object> subscription = ReceiverOptions.create(serversConfig)
				.subscription(List.of("order-events"));

		// The scheduler for below uses non-daemon thread so we don't need to block the thread (Thread.sleep) to keep it running
		KafkaReceiver.create(subscription)
				.receive()
				.log()
				.doOnNext(message -> logger.info("topic: {}, key: {}, value: {}", message.topic(), message.key(), message.value()))
				// Simulating error
				.map(message -> message.value().toString().toCharArray()[1000])
				.doOnError(throwable -> logger.error("Error message: " + throwable.getMessage()))
				// ACK the message once you have processed so as to update the current offset for topic for the consumer group
				//.doOnNext(message -> message.receiverOffset().acknowledge())
				//.retry(3)
				.retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1)))
				// .subscribe() // main method would exit on error and retries won't happen, this won't happen in server app as server would always be running and retries would take place.
				.blockLast();// just for demo

		// We are doing a lot of disconnect and re-connect with Kafka incase of any error (like due to processing issue), we should be handling this in a separate pipeline.

		// KafkaReceiver would give us a flux of items, when we get an item, we would be moving it into a separate processing pipeline, incase of error, the cancel signal would not be propagated to the main/receiver reactive pipeline, we can perform retry in this processing pipeline itself, BUT we would NOT be emitting error back to the receiver pipeline.
	}
}
