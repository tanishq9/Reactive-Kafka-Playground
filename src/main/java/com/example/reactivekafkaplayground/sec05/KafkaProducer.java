package com.example.reactivekafkaplayground.sec05;

import java.time.Duration;
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

/*
 * goal: to demo partition re-balancing, ensure that topic has multiple partitions.
 * */
public class KafkaProducer {

	private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);

	public static void main(String[] args) {

		// Producer has to serialise whereas consumer has to deserialize.
		Map<String, Object> serversConfig = Map.of(
				ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
				ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
				ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
		);

		SenderOptions<String, String> senderOptions = SenderOptions.<String, String>create(serversConfig);
		//.maxInFlight()

		Flux<SenderRecord<String, String, String>> flux = Flux.interval(Duration.ofMillis(50)) // every 50m produce a value
				.take(10_000)
				.map(i -> new ProducerRecord<String, String>(
								"order-events",
								i.toString(),
								"order-" + i
						)
				)
				.map(
						producerRecord -> SenderRecord.create(producerRecord, producerRecord.key())
				);

		KafkaSender.create(senderOptions)
				// send has to return a flux
				.send(flux)
				.doOnNext(stringSenderResult -> logger.info("correlation metadata is: {}", stringSenderResult.correlationMetadata()))
				.doOnComplete(() -> logger.info("Completed"))
				.subscribe();
	}
}
