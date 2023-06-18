package com.example.analyticsservice.service;

import com.example.analyticsservice.dto.ProductTrendingDto;
import com.example.analyticsservice.repository.ProductViewRepository;
import java.time.Duration;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor // to only autowire final fields
public class ProductTrendingBroadcastService {

	private final ProductViewRepository repository;

	private Flux<List<ProductTrendingDto>> trends;

	@PostConstruct
	private void init() {
		this.trends = this.repository.findTop5ByOrderByCountDesc()
				.map(productViewCount -> new ProductTrendingDto(productViewCount.getId(), productViewCount.getCount()))
				.collectList()
				.filter(l -> !l.isEmpty()) // we do not want to emit empty list
				.repeatWhen(l -> l.delayElements(Duration.ofSeconds(3))) // after every 3 seconds when a value is emitted, pipeline would be triggered to emit more item
				.distinctUntilChanged()
				.cache(1); // giving the cached version to everyone instead of creating a new publisher everytime
	}

	public Flux<List<ProductTrendingDto>> getTrends() {
		return this.trends;
	}
}
