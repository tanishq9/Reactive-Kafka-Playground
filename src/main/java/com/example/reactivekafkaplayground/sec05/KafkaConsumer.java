package com.example.reactivekafkaplayground.sec05;

import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
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
				ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, instanceId
		);

		ReceiverOptions<Object, Object> subscription = ReceiverOptions.create(serversConfig)
				//.consumerProperty(ConsumerConfig.GROUP_ID_CONFIG, "demo-grp");
				.subscription(List.of("order-events")); // topic to subscribe

		// The scheduler for below uses non-daemon thread so we don't need to block the thread (Thread.sleep) to keep it running
		KafkaReceiver.create(subscription)
				.receive()
				.doOnNext(message -> logger.info("topic: {}, key: {}, partition: {}, value: {}", message.topic(), message.key(), message.partition(), message.value()))
				.doOnNext(message -> {
							if (Long.parseLong(message.value().toString()) % 2 == 0) { // For some testing purpose what if the ACK is skipped
								System.out.println("Acknowledging even number: " + message.value().toString());
								message.receiverOffset().acknowledge();
							} else {
								System.out.println("NOT Acknowledging odd number: " + message.value().toString());
							}
						}
				)
				.subscribe();
	}
}

// Aim: To check after how long would the un-ack messages again reach consumer
// Why to test above? - To find time delta.
// Finding: Whenever a consumer is added to a partition, re-balancing is done, the un-ack messages are then redelivered to the consumers.
// More precise question now: After how much time are the unacknowledged kafka messages redelivered to the same existing customer?
// If some messages before an offset are not acknowledged but the message after that (greater offset) is acknowledged then those unacknowledged messages won't be redelivered.
// As a consequence Kafka will not track the "un-acknowledged" messages in between.
// https://stackoverflow.com/questions/63705895/when-will-kafka-retry-to-process-the-messages-that-have-not-been-acknowledged

// 3 consumer in consumer groups
// ACK only even numbers if message is number
// check lag in consumer group
// check if the odd numbers (for which ACK wasn't sent) are coming onto the consumers again

// docker exec -it <container-name> <command>
// docker exec -it

// List kafka topic, delete, create, describe and create with partitions
// kafka-topics.sh --bootstrap-server localhost:9092 --list
// kafka-topics.sh --bootstrap-server localhost:9092 --topic  --delete
// kafka-topics.sh --bootstrap-server localhost:9092 --topic order-events --delete
// kafka-topics.sh --bootstrap-server localhost:9092 --topic order-events --create
// kafka-topics.sh --bootstrap-server localhost:9092 --topic order-events --describe
// kafka-topics.sh --bootstrap-server localhost:9092 --topic order-events --create --partitions 2

// Produce to a topic, with keys
// kafka-console-producer.sh --bootstrap-server localhost:9092 --topic order-events
// kafka-console-producer.sh --bootstrap-server localhost:9092 --topic order-events --property key.separator=: --property parse.key=true

// Consume from a topic, from beginning, printing offset and timestamp
// kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic order-events --property print.offset=true --property print.timestamp=true
// kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic hello-world --from-beginning

// Consumer group, mention group, list all groups and describe group
// kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic order-events --property print.offset=true --property print.timestamp=true --group name
// kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
// kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group name --describe
