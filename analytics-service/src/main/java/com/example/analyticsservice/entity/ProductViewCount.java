package com.example.analyticsservice.entity;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

@Data
@AllArgsConstructor
@NoArgsConstructor
// if we are going to implement id ourselves then we have to implement this interface
public class ProductViewCount implements Persistable<Integer> {

	@Id
	private Integer id;
	private Long count;

	@Transient
	private Boolean isNew;

	@Override
	public boolean isNew() {
		return this.isNew || Objects.isNull(id);
	}
}
