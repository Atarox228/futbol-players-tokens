package com.desapp.futbolplayerstokens.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"userId","playerId"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long userId;
	private Long playerId;

	private int tokenQty;

	@Column(precision = 19, scale = 8)
	private BigDecimal avgBuyPrice;

	@Column(precision = 19, scale = 8)
	private BigDecimal currentValue;

	@Column(precision = 19, scale = 8)
	private BigDecimal profitLoss;
}


