package com.example.reactivekafkaplayground.sec06;

import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

/*
 * goal: to demo a simple kafka consumer using reactor kafka
 * producer ---> kafka broker <--- consumer
 * */
public class KafkaConsumer {

	private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

	public static void startConsumer(String instanceId) {

		// Consumer Properties
		Map<String, Object> serversConfig = Map.of(
				ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
				ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
				ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
				ConsumerConfig.GROUP_ID_CONFIG, "demo-group-123",
				ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
				ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, instanceId,
				ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, CooperativeStickyAssignor.COOPERATIVE_STICKY_ASSIGNOR_NAME
		);

		ReceiverOptions<Object, Object> subscription = ReceiverOptions.create(serversConfig)
				//.consumerProperty(ConsumerConfig.GROUP_ID_CONFIG, "demo-grp");
				.subscription(List.of("order-events")); // topic to subscribe

		// The scheduler for below uses non-daemon thread so we don't need to block the thread (Thread.sleep) to keep it running
		KafkaReceiver.create(subscription)
				.receive()
				.doOnNext(message -> logger.info("topic: {}, key: {}, partition: {}, value: {}", message.topic(), message.key(), message.partition(), message.value()))
				// ACK the message once you have processed so as to update the current offset for topic for the consumer group
				.doOnNext(message -> message.receiverOffset().acknowledge())
				.subscribe();
	}
}
