package com.example.productservice.service;

import com.example.productservice.dto.ProductDto;
import com.example.productservice.event.ProductViewEvent;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.util.EntityDtoUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class ProductService {

	private final ProductRepository productRepository;

	private final ProductViewEventProducer productViewEventProducer;

	public Mono<ProductDto> getProduct(int id) {
		return this.productRepository.findById(id)
				// whenever the product is viewed, event would be emitted
				.doOnNext(e -> this.productViewEventProducer.emitEvent(new ProductViewEvent(e.getId())))
				.map(EntityDtoUtil::toDto);
	}
}
