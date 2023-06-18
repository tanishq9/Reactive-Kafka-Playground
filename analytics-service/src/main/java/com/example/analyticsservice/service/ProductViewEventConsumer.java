package com.example.analyticsservice.service;

import com.example.analyticsservice.entity.ProductViewCount;
import com.example.analyticsservice.event.ProductViewEvent;
import com.example.analyticsservice.repository.ProductViewRepository;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverRecord;

@Service
@AllArgsConstructor
public class ProductViewEventConsumer {

	private static final Logger log = LoggerFactory.getLogger(ProductViewEventConsumer.class);

	private final ReactiveKafkaConsumerTemplate<String, ProductViewEvent> template;
	private final ProductViewRepository productViewRepository;

	@PostConstruct
	public void subscribe() {
		this.template
				.receive()
				// collecting 1000 events or all events collected in last 1 second
				// this will help with network calls if applicable such as making calls to DB
				// bufferTimeout will help to do things in batches
				.bufferTimeout(1000, Duration.ofSeconds(1))
				// process number of items in buffer could be 1000 or less than 1000 (what all was aggregated in last 1 second)
				.flatMap(this::process)
				.subscribe();
	}

	private Mono<Void> process(List<ReceiverRecord<String, ProductViewEvent>> events) {
		var eventsMap = events.stream()
				.map(r -> r.value().getProductId())
				.collect(Collectors.groupingBy(
						Function.identity(), // product id
						Collectors.counting() // count of product id
				));

		return this.productViewRepository.findAllById(eventsMap.keySet())
				.collectMap(ProductViewCount::getId) // key in map, this will give Mono<Map<Int, ProductViewCount>>
				.defaultIfEmpty(Collections.emptyMap()) // what if there are empty records? we would want to insert for first time hence using defaultIfEmpty
				.map(dbMap -> eventsMap.keySet().stream()
						.map(productId -> updateViewCount(dbMap, eventsMap, productId)).collect(Collectors.toList()))
				// at this point, we are having a list of updated ProductViewCount objects
				.flatMapMany(this.productViewRepository::saveAll)
				.doOnComplete(() -> events.get(events.size() - 1).receiverOffset().acknowledge()) // ack'ng the last message is fine
				.doOnError(ex -> log.error(ex.getMessage()))
				.then();
	}

	private ProductViewCount updateViewCount(Map<Integer, ProductViewCount> dbMap, Map<Integer, Long> eventMap, int productId) {
		ProductViewCount productViewCount = dbMap.getOrDefault(productId, new ProductViewCount(productId, 0L, true));
		// update the count for product id
		productViewCount.setCount(productViewCount.getCount() + eventMap.get(productId));
		return productViewCount;
	}

}
