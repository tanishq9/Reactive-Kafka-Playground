package com.example.reactivekafkaplayground.sec10;

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

		SenderOptions<String, String> senderOptions =
				SenderOptions
						.<String, String>create(serversConfig)
						.maxInFlight(10000);
		//.maxInFlight()

		Flux<SenderRecord<String, String, String>> flux = Flux.range(1, 100)
				.map(i -> new ProducerRecord<String, String>(
								"order-events",
								i.toString(),
								"order-" + i
						)
				)
				.map(
						producerRecord -> SenderRecord.create(producerRecord, producerRecord.key())
				);

		long startTime = System.currentTimeMillis();

		var sender = KafkaSender.create(senderOptions);

		sender
				.send(flux)
				.doOnNext(stringSenderResult -> logger.info("correlation metadata is: {}", stringSenderResult.correlationMetadata()))
				.doOnComplete(() -> {
					logger.info("Total time taken: {} ms", (System.currentTimeMillis() - startTime));
					sender.close();
				})
				.subscribe();
	}
}
