package com.example.reactivekafkaplayground.sec10;

import com.example.reactivekafkaplayground.sec03.KafkaConsumer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.retry.Retry;

public class KafkaConsumerV3 {

	private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

	public static void main(String[] args) {

		// Consumer Properties
		Map<String, Object> serversConfig = Map.of(
				ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
				ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
				ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
				ConsumerConfig.GROUP_ID_CONFIG, "demo-group",
				// ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest", // To consume all UnACKed messages from earliest or from the last ACKed offset
				// By default consumption starts from the last ACKed offset
				ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, "1"
		);

		ReceiverOptions<Object, Object> subscription = ReceiverOptions.create(serversConfig)
				.subscription(List.of("order-events"));

		// KafkaReceiver would give us a flux of items, when we get an item, we would be moving it into a separate processing pipeline, incase of error, the cancel signal would not be propagated to the main/receiver reactive pipeline, we can perform retry in this processing pipeline itself, BUT we would NOT be emitting error back to the receiver pipeline.

		// The scheduler for below uses non-daemon thread so we don't need to block the thread (Thread.sleep) to keep it running
		KafkaReceiver.create(subscription)
				.receive()
				.log()
				// delegate the processing to separate pipeline so as to handle retries there itself rather than disconnecting and reconnecting to kafka in the receiver pipeline
				.concatMap(KafkaConsumerV3::process)
				.subscribe();
	}

	private static Mono<Void> process(ReceiverRecord<Object, Object> receiverRecord) {
		return Mono.just(receiverRecord)
				.doOnNext(record -> {
					if (record.key().toString().equals("5")) {
						// this error would be propagated to the main pipeline since onErrorResume is only for out of bounds exception
						// and kafka consumer would be stopped
						throw new RuntimeException("DB is down");
					}
					int index = ThreadLocalRandom.current().nextInt(1, 10);
					// Below line can result in out of bounds exception
					logger.info("key: {}, value: {}, index: {}", record.key(), record.value().toString().toCharArray()[index], index);
					record.receiverOffset().acknowledge();
				})
				.log()
				.retryWhen(retrySpec())
				//.doOnError(ex -> logger.error(ex.getMessage()))
				.onErrorResume(IndexOutOfBoundsException.class, ex -> Mono.fromRunnable(() -> receiverRecord.receiverOffset().acknowledge()))
				//.doFinally(s -> receiverRecord.receiverOffset().acknowledge())
				//.onErrorComplete() // change error to complete signal, so as to not propagate error signal to downstream and continue processing
				.then();
	}

	private static Retry retrySpec() {
		return Retry.fixedDelay(3, Duration.ofSeconds(1))
				.filter(IndexOutOfBoundsException.class::isInstance) // i.e. only applicable for this exception
				.onRetryExhaustedThrow((spec, signal) -> signal.failure());
	}
}
