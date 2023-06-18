package com.example.reactivekafkaplayground.sec09;

import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

public class KafkaProducer {

	private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);

	public static void main(String[] args) {

		Map<String, Object> serversConfig = Map.of(
				ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
				ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
				ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
		);

		SenderOptions<String, String> senderOptions = SenderOptions.<String, String>create(serversConfig);

		Flux<SenderRecord<String, String, String>> flux =
				Flux.range(1, 100)
						.doOnNext(System.out::println)
						.map(i -> new ProducerRecord<>("order-events", i.toString(), "order-" + i))
						.map(producerRecord -> SenderRecord.create(producerRecord, producerRecord.key()));

		var sender = KafkaSender.create(senderOptions);

		// why is the messages sent not like 1 to 100 but randomly?
		sender
				.send(flux)
				.doOnNext(stringSenderResult -> logger.info("correlation metadata is: {}", stringSenderResult.correlationMetadata()))
				.doOnComplete(sender::close)
				.subscribe();
	}
}