package com.example.analyticsservice.controller;

import com.example.analyticsservice.dto.ProductTrendingDto;
import com.example.analyticsservice.service.ProductTrendingBroadcastService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@AllArgsConstructor
@RequestMapping("trending")
public class TrendingController {

	private final ProductTrendingBroadcastService productTrendingBroadcastService;

	// streaming api, server send events
	@GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<List<ProductTrendingDto>> trending() {
		return productTrendingBroadcastService.getTrends();
	}
}
