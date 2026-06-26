package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.MarketDepthDTO;
import com.desapp.futbolplayerstokens.controller.dto.MarketOverviewDTO;
import com.desapp.futbolplayerstokens.controller.dto.OrderBookStatsDTO;
import com.desapp.futbolplayerstokens.controller.dto.PlayerValuationDTO;
import com.desapp.futbolplayerstokens.controller.dto.PortfolioSummaryDTO;
import com.desapp.futbolplayerstokens.controller.dto.StrategyImpactDTO;
import com.desapp.futbolplayerstokens.controller.dto.TopTradedDTO;
import com.desapp.futbolplayerstokens.exception.ResourceNotFoundException;
import com.desapp.futbolplayerstokens.modelo.Order;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.Portfolio;
import com.desapp.futbolplayerstokens.modelo.Quote;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.modelo.User;
import com.desapp.futbolplayerstokens.repository.OrderRepository;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.repository.PortfolioRepository;
import com.desapp.futbolplayerstokens.repository.QuoteRepository;
import com.desapp.futbolplayerstokens.repository.StrategyConfigRepository;
import com.desapp.futbolplayerstokens.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/metrics")
public class MetricsControllerREST {

    private final OrderRepository orderRepository;
    private final PlayerRepository playerRepository;
    private final PortfolioRepository portfolioRepository;
    private final QuoteRepository quoteRepository;
    private final UserRepository userRepository;
    private final StrategyConfigRepository strategyConfigRepository;

    public MetricsControllerREST(OrderRepository orderRepository,
                                  PlayerRepository playerRepository,
                                  PortfolioRepository portfolioRepository,
                                  QuoteRepository quoteRepository,
                                  UserRepository userRepository,
                                  StrategyConfigRepository strategyConfigRepository) {
        this.orderRepository = orderRepository;
        this.playerRepository = playerRepository;
        this.portfolioRepository = portfolioRepository;
        this.quoteRepository = quoteRepository;
        this.userRepository = userRepository;
        this.strategyConfigRepository = strategyConfigRepository;
    }

    @GetMapping("/market-overview")
    public ResponseEntity<MarketOverviewDTO> marketOverview() {
        long openBuyOrders = orderRepository.countByStatusAndType(Order.OrderStatus.PENDING, Order.OrderType.BUY);
        long openSellOrders = orderRepository.countByStatusAndType(Order.OrderStatus.PENDING, Order.OrderType.SELL);
        BigDecimal totalValueLockedBuy = orderRepository.sumTotalByStatusAndType(Order.OrderStatus.PENDING, Order.OrderType.BUY);
        BigDecimal totalValueLockedSell = orderRepository.sumTotalByStatusAndType(Order.OrderStatus.PENDING, Order.OrderType.SELL);
        long activeUsers = portfolioRepository.countDistinctUserId();
        long totalPlayers = playerRepository.count();
        long totalTokensInCirculation = playerRepository.findAll().stream()
                .mapToLong(Player::getTotalTokens)
                .sum();

        return ResponseEntity.ok(MarketOverviewDTO.builder()
                .openBuyOrders(openBuyOrders)
                .openSellOrders(openSellOrders)
                .totalValueLockedBuy(totalValueLockedBuy)
                .totalValueLockedSell(totalValueLockedSell)
                .activeUsers(activeUsers)
                .totalPlayers(totalPlayers)
                .totalTokensInCirculation(totalTokensInCirculation)
                .build());
    }

    @GetMapping("/market-depth/{playerId}")
    public ResponseEntity<MarketDepthDTO> marketDepth(@PathVariable Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found"));

        List<Order> pendingOrders = orderRepository.findByPlayerAndStatusIn(player,
                List.of(Order.OrderStatus.PENDING, Order.OrderStatus.PARTIALLY_FILLED));

        Map<BigDecimal, List<Order>> bidsByPrice = pendingOrders.stream()
                .filter(o -> o.getType() == Order.OrderType.BUY)
                .collect(Collectors.groupingBy(Order::getPriceAtOrder));

        Map<BigDecimal, List<Order>> asksByPrice = pendingOrders.stream()
                .filter(o -> o.getType() == Order.OrderType.SELL)
                .collect(Collectors.groupingBy(Order::getPriceAtOrder));

        List<MarketDepthDTO.DepthLevel> bids = bidsByPrice.entrySet().stream()
                .map(e -> new MarketDepthDTO.DepthLevel(
                        e.getKey(),
                        e.getValue().stream().mapToInt(Order::getRemainingQuantity).sum(),
                        e.getValue().size()))
                .sorted(Comparator.comparing(MarketDepthDTO.DepthLevel::getPrice).reversed())
                .toList();

        List<MarketDepthDTO.DepthLevel> asks = asksByPrice.entrySet().stream()
                .map(e -> new MarketDepthDTO.DepthLevel(
                        e.getKey(),
                        e.getValue().stream().mapToInt(Order::getRemainingQuantity).sum(),
                        e.getValue().size()))
                .sorted(Comparator.comparing(MarketDepthDTO.DepthLevel::getPrice))
                .toList();

        BigDecimal bestBid = bids.isEmpty() ? BigDecimal.ZERO : bids.getFirst().getPrice();
        BigDecimal bestAsk = asks.isEmpty() ? BigDecimal.ZERO : asks.getFirst().getPrice();
        BigDecimal spread = (bestBid.compareTo(BigDecimal.ZERO) > 0 && bestAsk.compareTo(BigDecimal.ZERO) > 0)
                ? bestAsk.subtract(bestBid)
                : BigDecimal.ZERO;

        return ResponseEntity.ok(MarketDepthDTO.builder()
                .playerId(playerId)
                .playerName(player.getName())
                .bestBid(bestBid)
                .bestAsk(bestAsk)
                .spread(spread)
                .bids(bids)
                .asks(asks)
                .build());
    }

    @GetMapping("/player-valuation/{playerId}")
    public ResponseEntity<PlayerValuationDTO> playerValuation(@PathVariable Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found"));

        Optional<Quote> latestQuoteOpt = quoteRepository.findTopByPlayerIdOrderByTimestampDesc(playerId);

        BigDecimal currentPrice = latestQuoteOpt.map(Quote::getPrice).orElse(BigDecimal.ZERO);
        LocalDateTime now = LocalDateTime.now();

        BigDecimal priceChange1d = calcPriceChange(playerId, now.minusDays(1));
        BigDecimal priceChange7d = calcPriceChange(playerId, now.minusDays(7));
        BigDecimal priceChange30d = calcPriceChange(playerId, now.minusDays(30));

        double volatility30d = calcVolatility(playerId, now.minusDays(30));

        return ResponseEntity.ok(PlayerValuationDTO.builder()
                .playerId(playerId)
                .playerName(player.getName())
                .currentPrice(currentPrice)
                .priceChange1d(priceChange1d)
                .priceChange7d(priceChange7d)
                .priceChange30d(priceChange30d)
                .volatility30d(volatility30d)
                .score(player.getScore())
                .position(player.getPosition())
                .team(player.getTeam())
                .build());
    }

    @GetMapping("/top-traded")
    public ResponseEntity<List<TopTradedDTO>> topTraded() {
        List<Object[]> results = orderRepository.findTopTradedPlayers();
        List<TopTradedDTO> list = new ArrayList<>();
        int rank = 1;
        for (Object[] row : results) {
            Long playerId = (Long) row[0];
            long orderCount = (long) row[1];
            long totalQuantity = (long) row[2];
            BigDecimal totalValue = (BigDecimal) row[3];

            Player player = playerRepository.findById(playerId).orElse(null);
            if (player == null) continue;

            list.add(TopTradedDTO.builder()
                    .rank(rank++)
                    .playerId(playerId)
                    .playerName(player.getName())
                    .team(player.getTeam())
                    .league(player.getLeague())
                    .orderCount(orderCount)
                    .totalQuantity(totalQuantity)
                    .totalValue(totalValue)
                    .build());
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/portfolio-summary/{userId}")
    public ResponseEntity<PortfolioSummaryDTO> portfolioSummary(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Portfolio> portfolioEntries = portfolioRepository.findByUser(user).stream()
                .filter(p -> p.getTokenQty() > 0)
                .toList();

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal currentValue = BigDecimal.ZERO;
        List<PortfolioSummaryDTO.PositionSummary> positions = new ArrayList<>();
        long fw = 0, mf = 0, df = 0, gk = 0;
        long laliga = 0, prem = 0, bundes = 0, seriea = 0, ligue1 = 0;

        for (Portfolio p : portfolioEntries) {
            BigDecimal avgPrice = p.getAvgBuyPrice();
            BigDecimal qty = BigDecimal.valueOf(p.getTokenQty());
            BigDecimal invested = avgPrice.multiply(qty);

            BigDecimal currentPrice = quoteRepository
                    .findTopByPlayerIdOrderByTimestampDesc(p.getPlayer().getId())
                    .map(Quote::getPrice)
                    .orElse(avgPrice);
            BigDecimal currVal = currentPrice.multiply(qty);
            BigDecimal pl = currVal.subtract(invested);
            BigDecimal plPct = invested.compareTo(BigDecimal.ZERO) > 0
                    ? pl.divide(invested, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            totalInvested = totalInvested.add(invested);
            currentValue = currentValue.add(currVal);

            positions.add(PortfolioSummaryDTO.PositionSummary.builder()
                    .playerId(p.getPlayer().getId())
                    .playerName(p.getPlayer().getName())
                    .position(p.getPlayer().getPosition())
                    .team(p.getPlayer().getTeam())
                    .tokenQty(p.getTokenQty())
                    .avgBuyPrice(avgPrice)
                    .currentPrice(currentPrice)
                    .currentValue(currVal)
                    .profitLoss(pl)
                    .profitLossPercent(plPct)
                    .build());

            String pos = p.getPlayer().getPosition() != null ? p.getPlayer().getPosition().toUpperCase() : "";
            switch (pos) {
                case "FW", "FORWARD", "DELANTERO" -> fw++;
                case "MF", "MIDFIELDER", "MEDIOCAMPISTA" -> mf++;
                case "DF", "DEFENDER", "DEFENSOR" -> df++;
                case "GK", "GOALKEEPER", "ARQUERO" -> gk++;
            }
            String league = p.getPlayer().getLeague() != null ? p.getPlayer().getLeague() : "";
            switch (league) {
                case "LaLiga" -> laliga++;
                case "Premier League" -> prem++;
                case "Bundesliga" -> bundes++;
                case "Serie A" -> seriea++;
                case "Ligue 1" -> ligue1++;
            }
        }

        BigDecimal profitLoss = currentValue.subtract(totalInvested);
        BigDecimal profitLossPercent = totalInvested.compareTo(BigDecimal.ZERO) > 0
                ? profitLoss.divide(totalInvested, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        positions.sort(Comparator.comparing(PortfolioSummaryDTO.PositionSummary::getCurrentValue).reversed());

        return ResponseEntity.ok(PortfolioSummaryDTO.builder()
                .userId(userId)
                .username(user.getUsername())
                .totalInvested(totalInvested)
                .currentValue(currentValue)
                .profitLoss(profitLoss)
                .profitLossPercent(profitLossPercent)
                .totalPositions(positions.size())
                .positions(positions)
                .diversification(PortfolioSummaryDTO.Diversification.builder()
                        .forwardCount(fw)
                        .midfielderCount(mf)
                        .defenderCount(df)
                        .goalkeeperCount(gk)
                        .laLigaCount(laliga)
                        .premierLeagueCount(prem)
                        .bundesligaCount(bundes)
                        .serieACount(seriea)
                        .ligue1Count(ligue1)
                        .build())
                .build());
    }

    @GetMapping("/order-book-stats")
    public ResponseEntity<OrderBookStatsDTO> orderBookStats() {
        long totalOrders = orderRepository.count();
        long filledOrders = orderRepository.findByStatus(Order.OrderStatus.FILLED).size();
        long cancelledOrders = orderRepository.findByStatus(Order.OrderStatus.CANCELLED).size();
        long pendingOrders = orderRepository.findByStatus(Order.OrderStatus.PENDING).size();

        double fillRate = totalOrders > 0 ? (double) filledOrders / totalOrders : 0;
        double cancelRate = totalOrders > 0 ? (double) cancelledOrders / totalOrders : 0;

        int avgOrderSize = 0;
        if (filledOrders + pendingOrders > 0) {
            List<Order> allNonCancelled = new ArrayList<>();
            allNonCancelled.addAll(orderRepository.findByStatus(Order.OrderStatus.FILLED));
            allNonCancelled.addAll(orderRepository.findByStatus(Order.OrderStatus.PENDING));
            allNonCancelled.addAll(orderRepository.findByStatus(Order.OrderStatus.PARTIALLY_FILLED));
            avgOrderSize = allNonCancelled.isEmpty() ? 0
                    : (int) Math.round(allNonCancelled.stream().mapToInt(Order::getQuantity).average().orElse(0));
        }

        return ResponseEntity.ok(OrderBookStatsDTO.builder()
                .fillRate(fillRate)
                .avgTimeToFillHours(0)
                .avgOrderSize(avgOrderSize)
                .cancelRate(cancelRate)
                .totalOrders(totalOrders)
                .filledOrders(filledOrders)
                .cancelledOrders(cancelledOrders)
                .pendingOrders(pendingOrders)
                .build());
    }

    @GetMapping("/strategy-impact")
    public ResponseEntity<List<StrategyImpactDTO>> strategyImpact() {
        List<StrategyConfig> allStrategies = strategyConfigRepository.findAll();
        List<StrategyImpactDTO> impacts = new ArrayList<>();

        for (StrategyConfig.StrategyType type : StrategyConfig.StrategyType.values()) {
            List<StrategyConfig> versions = strategyConfigRepository.findByTypeOrderByVersionDesc(type);
            if (versions.size() < 2) continue;

            StrategyConfig latest = versions.getFirst();
            StrategyConfig previous = versions.get(1);

            LocalDateTime switchTime = estimateSwitchTime(latest);
            LocalDateTime periodStart = switchTime.minusHours(1);
            LocalDateTime periodEnd = switchTime.plusHours(1);

            if (periodStart == null) continue;

            List<Quote> beforeQuotes = quoteRepository.findByStrategyAndVersionAndTimestampBetween(
                    previous.getId(), previous.getVersion(), periodStart, switchTime);
            List<Quote> afterQuotes = quoteRepository.findByStrategyAndVersionAndTimestampBetween(
                    latest.getId(), latest.getVersion(), switchTime, periodEnd);

            BigDecimal avgBefore = beforeQuotes.stream()
                    .map(Quote::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(Math.max(beforeQuotes.size(), 1)), 8, RoundingMode.HALF_UP);
            BigDecimal avgAfter = afterQuotes.stream()
                    .map(Quote::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(Math.max(afterQuotes.size(), 1)), 8, RoundingMode.HALF_UP);

            BigDecimal change = avgBefore.compareTo(BigDecimal.ZERO) > 0
                    ? avgAfter.subtract(avgBefore).divide(avgBefore, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            List<StrategyImpactDTO.PriceChange> topChanges = findTopPriceChanges(
                    beforeQuotes, afterQuotes, latest.getVersion(), previous.getVersion());

            impacts.add(StrategyImpactDTO.builder()
                    .strategyType(type.name())
                    .previousVersion(previous.getVersion())
                    .currentVersion(latest.getVersion())
                    .avgPriceBefore(avgBefore)
                    .avgPriceAfter(avgAfter)
                    .priceChangePercent(change)
                    .affectedPlayers(topChanges.size())
                    .topChanges(topChanges.stream().limit(10).toList())
                    .build());
        }

        return ResponseEntity.ok(impacts);
    }

    private BigDecimal calcPriceChange(Long playerId, LocalDateTime since) {
        List<BigDecimal> prices = quoteRepository.findPricesByPlayerIdSince(playerId, since);
        if (prices.size() < 2) return BigDecimal.ZERO;
        BigDecimal oldest = prices.getLast();
        BigDecimal newest = prices.getFirst();
        if (oldest.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return newest.subtract(oldest).divide(oldest, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    private double calcVolatility(Long playerId, LocalDateTime since) {
        List<BigDecimal> prices = quoteRepository.findPricesByPlayerIdSince(playerId, since);
        if (prices.size() < 2) return 0;
        double mean = prices.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double variance = prices.stream()
                .mapToDouble(p -> Math.pow(p.doubleValue() - mean, 2))
                .average()
                .orElse(0);
        return Math.sqrt(variance);
    }

    private LocalDateTime estimateSwitchTime(StrategyConfig config) {
        return strategyConfigRepository.findFirstByTypeAndVersionLessThanOrderByVersionDesc(
                        config.getType(), config.getVersion())
                .map(prev -> {
                    List<Quote> firstQuotes = quoteRepository.findByStrategyAndVersionAndTimestampBetween(
                            config.getId(), config.getVersion(),
                            LocalDateTime.MIN, LocalDateTime.MAX);
                    if (!firstQuotes.isEmpty()) {
                        return firstQuotes.getFirst().getTimestamp();
                    }
                    return null;
                })
                .orElse(LocalDateTime.now().minusDays(7));
    }

    private List<StrategyImpactDTO.PriceChange> findTopPriceChanges(
            List<Quote> before, List<Quote> after, int newVersion, int oldVersion) {
        Map<Long, BigDecimal> beforeMap = before.stream()
                .collect(Collectors.toMap(q -> q.getPlayer().getId(), Quote::getPrice,
                        (a, b) -> a));
        Map<Long, BigDecimal> afterMap = after.stream()
                .collect(Collectors.toMap(q -> q.getPlayer().getId(), Quote::getPrice,
                        (a, b) -> a));

        List<StrategyImpactDTO.PriceChange> changes = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : afterMap.entrySet()) {
            Long playerId = entry.getKey();
            BigDecimal newPrice = entry.getValue();
            BigDecimal oldPrice = beforeMap.get(playerId);
            if (oldPrice == null || oldPrice.compareTo(BigDecimal.ZERO) == 0) continue;
            BigDecimal changePct = newPrice.subtract(oldPrice)
                    .divide(oldPrice, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            Player player = playerRepository.findById(playerId).orElse(null);
            changes.add(StrategyImpactDTO.PriceChange.builder()
                    .playerId(playerId)
                    .playerName(player != null ? player.getName() : "Unknown")
                    .oldPrice(oldPrice)
                    .newPrice(newPrice)
                    .changePercent(changePct)
                    .build());
        }

        changes.sort(Comparator.comparing(StrategyImpactDTO.PriceChange::getChangePercent).reversed());
        return changes;
    }
}
