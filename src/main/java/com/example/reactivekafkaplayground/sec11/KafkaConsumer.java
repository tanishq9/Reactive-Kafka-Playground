package com.example.reactivekafkaplayground.sec11;

import java.util.Map;
import java.util.regex.Pattern;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

/*
 * goal: poison pill message
 * */
// Poison pill message: These messages are the ones which the consumer does not expect to consume from some topic, these messages can stop the consumer from further consuming other messages which it expects since these incorrect messages, which it is unable to process, are not acknowledged and consumer would keep on getting those on restarts.
// To counter above, we can write and use a custom deserialiser which doesn't fail on these incorrect type message and just logs them and returns a fallback value.
public class KafkaConsumer {

	private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

	public static void main(String[] args) {

		// Consumer Properties
		Map<String, Object> serversConfig = Map.of(
				ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
				ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
				// ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class,
				ConsumerConfig.GROUP_ID_CONFIG, "inventory-service-group",
				// ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest", // To consume all UnACKed messages from earliest or from the last ACKed offset
				// By default consumption starts from the last ACKed offset
				ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, "1"
		);

		// Consumer which consume integer values
		ReceiverOptions<String, Integer> subscription = ReceiverOptions.<String, Integer>create(serversConfig)
				.withValueDeserializer(errorHandlingDeserializer())
				.subscription(Pattern.compile("order.*")); // topic to subscribe

		// The scheduler for below uses non-daemon thread so we don't need to block the thread (Thread.sleep) to keep it running
		KafkaReceiver.create(subscription)
				.receive()
				.doOnNext(message -> logger.info("topic: {}, key: {}, value: {}", message.topic(), message.key(), message.value()))
				// ACK the message once you have processed so as to update the current offset for topic for the consumer group
				.doOnNext(message -> message.receiverOffset().acknowledge())
				.subscribe();
	}

	private static ErrorHandlingDeserializer<Integer> errorHandlingDeserializer() {
		System.out.println("Inside custom deserializer");
		var deserializer = new ErrorHandlingDeserializer<>(new IntegerDeserializer());
		deserializer.setFailedDeserializationFunction(
				failedDeserializationInfo -> {
					failedDeserializationInfo.getException().printStackTrace();
					logger.error("failed record: {}", new String(failedDeserializationInfo.getData()));
					return -1;
				}
		);
		return deserializer;
	}
}
