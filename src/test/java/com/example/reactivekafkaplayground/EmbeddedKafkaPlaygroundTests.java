package com.example.reactivekafkaplayground;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.test.condition.EmbeddedKafkaCondition;
import org.springframework.kafka.test.context.EmbeddedKafka;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;
import reactor.test.StepVerifier;

// For integration testing, we would be using EmbeddedKafka with Spring for Kafka server. Testcontainers could also be used.
@EmbeddedKafka(
		//ports = 9092,
		partitions = 1,
		brokerProperties = {
				"auto.create.topics.enable=false" // disabling this property at broker side to disable creating topics if not already present
		},
		topics = {
				"order-events"
		}
)
class EmbeddedKafkaPlaygroundTests {

	@Test
	void embeddedKafkaDemo() {

		String brokerAddress = EmbeddedKafkaCondition.getBroker().getBrokersAsString();

		StepVerifier.create(Producer.produceEvents(brokerAddress))
				.verifyComplete(); // verify we are getting complete signal i.e. able to produce successfully

		StepVerifier.create(Consumer.consumeEvents(brokerAddress))
				.verifyComplete(); // verify we are getting complete signal i.e. able to consume successfully
	}

	private static final class Producer {

		private static final Logger logger = LoggerFactory.getLogger(Producer.class);

		public static Mono<Void> produceEvents(String brokerAddress) {
			Map<String, Object> serversConfig = Map.of(
					ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerAddress,
					ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class,
					ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
			);

			SenderOptions<Integer, String> senderOptions = SenderOptions.create(serversConfig);

			Flux<SenderRecord<Integer, String, Integer>> senderRecordFlux = Flux.range(1, 10)
					.delayElements(Duration.ofMillis(100))
					.map(integer -> new ProducerRecord<Integer, String>("order-events", integer, "value-" + integer))
					.map(pr -> SenderRecord.create(pr, pr.key()));

			return KafkaSender.create(senderOptions)
					.send(senderRecordFlux)
					.doOnNext(integerSenderResult -> logger.info("Produced event with metadata: {}", integerSenderResult.recordMetadata().toString()))
					.then();
		}
	}

	private static final class Consumer {

		private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

		public static Mono<Void> consumeEvents(String brokerAddress) {
			Map<String, Object> serversConfig = Map.of(
					ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerAddress,
					ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class,
					ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
					ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest", // To consume all UnACKed messages from earliest or from the last ACKed offset
					ConsumerConfig.GROUP_ID_CONFIG, "demo-group",
					ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, "1"
			);

			ReceiverOptions<Integer, String> receiverOptions = ReceiverOptions.<Integer, String>create(serversConfig)
					.subscription(List.of("order-events"));

			return KafkaReceiver.create(receiverOptions)
					.receive()
					.take(10) // so as to stop the consumer else it would keep on running
					.doOnNext(record -> logger.info("Consumed event with key: {} and value: {}", record.key(), record.value()))
					.doOnNext(record -> record.receiverOffset().acknowledge())
					.then();
		}
	}
}
