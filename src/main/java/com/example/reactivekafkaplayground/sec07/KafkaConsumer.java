package com.example.reactivekafkaplayground.sec07;

import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

/*
 * goal: to seek offset
 * */
public class KafkaConsumer {

	private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

	public static void main(String[] args) {

		// Consumer Properties
		Map<String, Object> serversConfig = Map.of(
				ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
				ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
				ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
				ConsumerConfig.GROUP_ID_CONFIG, "demo-group-123",
				ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
				ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, "1"
		);

		ReceiverOptions<Object, Object> subscription = ReceiverOptions.create(serversConfig)
				.addAssignListener(receiverPartitions -> {
					receiverPartitions.forEach(
							receiverPartition -> logger.info("assigned {}", receiverPartition.position())
					);
					/*receiverPartitions.forEach(
							receiverPartition -> receiverPartition.seek(receiverPartition.position() - 2)
					);*/
					receiverPartitions.stream()
							.filter(receiverPartition -> receiverPartition.topicPartition().partition() == 2)
							.findFirst()
							// we can also seek to some timestamp
							.ifPresent(receiverPartition -> receiverPartition.seek(receiverPartition.position() - 2));
				})
				.subscription(List.of("order-events")); // topic to subscribe

		// The scheduler for below uses non-daemon thread so we don't need to block the thread (Thread.sleep) to keep it running
		KafkaReceiver.create(subscription)
				.receive()
				.doOnNext(message -> logger.info("topic: {}, key: {}, value: {}", message.topic(), message.key(), message.value()))
				.doOnNext(message -> message.receiverOffset().acknowledge())
				.subscribe();
	}
}
