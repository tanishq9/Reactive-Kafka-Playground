package com.example.reactivekafkaplayground;

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

@SpringBootTest
@EmbeddedKafka(
		partitions = 1,
		topics = {"order-events"},
		bootstrapServersProperty = "spring.kafka.bootstrapServers" // this property would update this value in application.yaml when running the app
)
public abstract class AbstractIT {

	@Autowired
	private EmbeddedKafkaBroker broker;

	protected <V> KafkaReceiver<String, V> createReceiver(String... topics) {
		return createReceiver(options ->
				options.withKeyDeserializer(new StringDeserializer())
						.withValueDeserializer(new JsonDeserializer<V>().trustedPackages("*"))
						.subscription(List.of(topics))
		);
	}

	// UnaryOperator is a functional interface which takes 1 arg and returns the result of the same type
	protected <K, V> KafkaReceiver<K, V> createReceiver(UnaryOperator<ReceiverOptions<K, V>> builder) {
		// By default KafkaTestUtils uses IntegerDeserializer for key and StringDeserializer for value
		Map<String, Object> props = KafkaTestUtils.consumerProps("test-group", "true", broker);
		ReceiverOptions<K, V> defaultOptions = ReceiverOptions.create(props);
		ReceiverOptions<K, V> appliedOptions = builder.apply(defaultOptions); // adding onto the default builder options
		return KafkaReceiver.create(appliedOptions);
	}

	protected <V> KafkaSender<String, V> createSender() {
		return createSender(options ->
				options.withKeySerializer(new StringSerializer())
						.withValueSerializer(new JsonSerializer<>())
		);
	}

	protected <K, V> KafkaSender<K, V> createSender(UnaryOperator<SenderOptions<K, V>> builder) {
		// By default KafkaTestUtils uses IntegerDeserializer for key and StringDeserializer for value
		Map<String, Object> props = KafkaTestUtils.producerProps(broker);
		SenderOptions<K, V> defaultOptions = SenderOptions.create(props);
		SenderOptions<K, V> appliedOptions = builder.apply(defaultOptions); // adding onto the default builder options
		return KafkaSender.create(appliedOptions);
	}
}
