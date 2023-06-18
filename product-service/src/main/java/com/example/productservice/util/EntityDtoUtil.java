package com.example.productservice.util;

import com.example.productservice.dto.ProductDto;
import com.example.productservice.entity.Product;
import org.springframework.beans.BeanUtils;

public class EntityDtoUtil {

	public static ProductDto toDto(Product product) {
		var dto = new ProductDto();
		BeanUtils.copyProperties(product, dto);
		return dto;
	}

	public static Product toEntity(ProductDto productDto) {
		var product = new Product();
		BeanUtils.copyProperties(productDto, product);
		return product;
	}
}
