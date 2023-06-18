package com.example.reactivekafkaplayground.sec11;

import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
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
				ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class
		);

		SenderOptions<String, Integer> senderOptions = SenderOptions.<String, Integer>create(serversConfig);

		Flux<SenderRecord<String, Integer, String>> flux =
				Flux.range(1, 10)
						.map(i -> new ProducerRecord<String, Integer>(
										"order-events",
										i.toString(), i
								)
						)
						.map(producerRecord -> SenderRecord.create(producerRecord, producerRecord.key()));

		var sender = KafkaSender.create(senderOptions);

		sender
				.send(flux)
				.doOnNext(stringSenderResult -> logger.info("correlation metadata is: {}", stringSenderResult.correlationMetadata()))
				.doOnComplete(sender::close)
				.subscribe();
	}
}
