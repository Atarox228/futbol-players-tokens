package com.desapp.futbolplayerstokens.config;

import com.desapp.futbolplayerstokens.modelo.Order;
import com.desapp.futbolplayerstokens.repository.OrderRepository;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.repository.PortfolioRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public io.micrometer.core.instrument.binder.MeterBinder marketMetrics(
            OrderRepository orderRepository,
            PlayerRepository playerRepository,
            PortfolioRepository portfolioRepository) {
        return registry -> {
            Gauge.builder("market.open.orders", orderRepository,
                            repo -> repo.findByStatus(Order.OrderStatus.PENDING).size())
                    .description("Current number of pending orders")
                    .tag("type", "all")
                    .register(registry);

            Gauge.builder("market.open.orders", orderRepository,
                            repo -> repo.findByStatusAndType(Order.OrderStatus.PENDING, Order.OrderType.BUY).size())
                    .description("Current number of pending buy orders")
                    .tag("type", "buy")
                    .register(registry);

            Gauge.builder("market.open.orders", orderRepository,
                            repo -> repo.findByStatusAndType(Order.OrderStatus.PENDING, Order.OrderType.SELL).size())
                    .description("Current number of pending sell orders")
                    .tag("type", "sell")
                    .register(registry);

            Gauge.builder("players.total", playerRepository, PlayerRepository::count)
                    .description("Total number of players")
                    .register(registry);

            Gauge.builder("users.active", portfolioRepository, PortfolioRepository::countDistinctUserId)
                    .description("Number of users with non-empty portfolio")
                    .register(registry);
        };
    }
}
