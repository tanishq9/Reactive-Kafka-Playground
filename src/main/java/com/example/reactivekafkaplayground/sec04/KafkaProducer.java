package com.example.reactivekafkaplayground.sec04;

import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
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
						.<String, String>create(serversConfig);
		//.maxInFlight(10000);

		Flux<SenderRecord<String, String, String>> flux =
				Flux.range(1, 10)
						.map(KafkaProducer::createSenderRecord);

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

	private static SenderRecord<String, String, String> createSenderRecord(Integer i) {
		var headers = new RecordHeaders();

		headers.add(new RecordHeader("client-id", "some-client".getBytes()));
		headers.add(new RecordHeader("tracing-id", "123".getBytes()));

		var producerRecord = new ProducerRecord<String, String>(
				"order-events",
				null, // null partition? using key it will find the value by default
				i.toString(),
				"order-" + i,
				headers
		);

		return SenderRecord.create(producerRecord, producerRecord.key());
	}
}