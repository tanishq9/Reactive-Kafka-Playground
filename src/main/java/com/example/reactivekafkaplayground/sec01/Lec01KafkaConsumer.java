package com.example.reactivekafkaplayground.sec01;

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
public class Lec01KafkaConsumer {

	private static final Logger logger = LoggerFactory.getLogger(Lec01KafkaConsumer.class);

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
				//.consumerProperty(ConsumerConfig.GROUP_ID_CONFIG, "demo-grp");
				.subscription(List.of("order-events")); // topic to subscribe

		// The scheduler for below uses non-daemon thread so we don't need to block the thread (Thread.sleep) to keep it running
		KafkaReceiver.create(subscription)
				.receive()
				//.take(3) // Consumer would stop after consuming 3 messages
				.doOnNext(message -> logger.info(message.key() + " " + message.topic() + " " + message.value()))
				// ACK the message once you have processed so as to update the current offset for topic for the consumer group
				.doOnNext(message ->
						{
							// if (message.value().toString().equals("4")) { -- For some testing purpose what if the ACK is skipped
							// System.out.println("Acknowledging Now");
							message.receiverOffset().acknowledge();
							//}
						}
				)
				.subscribe();
	}
}
