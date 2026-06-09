package com.desapp.futbolplayerstokens.modelo;

import com.desapp.futbolplayerstokens.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    // ── User ──

    @Test
    void userBuilder() {
        User u = User.builder()
                .id(1L).username("test").email("test@test.com")
                .password("pass").role(User.Role.USER)
                .balance(new BigDecimal("1000"))
                .build();
        assertEquals(1L, u.getId());
        assertEquals("test", u.getUsername());
        assertEquals("test@test.com", u.getEmail());
        assertEquals("pass", u.getPassword());
        assertEquals(User.Role.USER, u.getRole());
        assertEquals(new BigDecimal("1000"), u.getBalance());
    }

    @Test
    void userNoArgsConstructor() {
        User u = new User();
        assertNull(u.getId());
        assertNull(u.getUsername());
    }

    @Test
    void userAllArgsConstructor() {
        User u = new User(1L, "u", "e@e.com", "p", User.Role.ADMIN, new BigDecimal("500"), "key");
        assertEquals("u", u.getUsername());
        assertEquals(User.Role.ADMIN, u.getRole());
        assertEquals("key", u.getApiKey());
    }

    @Test
    void userRoleValues() {
        assertEquals(3, User.Role.values().length);
        assertTrue(contains(User.Role.values(), User.Role.USER));
        assertTrue(contains(User.Role.values(), User.Role.ADMIN));
        assertTrue(contains(User.Role.values(), User.Role.SUPERUSER));
    }

    @Test
    void userSetters() {
        User u = new User();
        u.setUsername("new");
        u.setEmail("n@n.com");
        u.setPassword("newpass");
        u.setRole(User.Role.SUPERUSER);
        u.setBalance(new BigDecimal("999"));
        u.setApiKey("k");
        assertEquals("new", u.getUsername());
        assertEquals("n@n.com", u.getEmail());
        assertEquals("newpass", u.getPassword());
        assertEquals(User.Role.SUPERUSER, u.getRole());
        assertEquals(new BigDecimal("999"), u.getBalance());
        assertEquals("k", u.getApiKey());
    }

    // ── Order ──

    @Test
    void orderBuilderAndDefaults() {
        User u = User.builder().id(1L).username("u").email("e").password("p")
                .role(User.Role.USER).balance(BigDecimal.ZERO).build();
        Player p = Player.builder().name("P1").team("T").build();
        Order o = Order.builder()
                .user(u).player(p).type(Order.OrderType.BUY)
                .quantity(10).priceAtOrder(new BigDecimal("100")).total(new BigDecimal("1000"))
                .idempotencyKey("key-1")
                .build();
        o.prePersist();
        assertNotNull(o.getCreatedAt());
        assertEquals(Order.OrderStatus.PENDING, o.getStatus());
        assertEquals(10, o.getRemainingQuantity());
    }

    @Test
    void orderPrePersistKeepsExistingValues() {
        User u = User.builder().id(1L).username("u").email("e").password("p")
                .role(User.Role.USER).balance(BigDecimal.ZERO).build();
        Player p = Player.builder().name("P1").team("T").build();
        LocalDateTime now = LocalDateTime.now();
        Order o = Order.builder()
                .user(u).player(p).type(Order.OrderType.SELL)
                .quantity(5).priceAtOrder(new BigDecimal("50")).total(new BigDecimal("250"))
                .idempotencyKey("key-2")
                .createdAt(now).status(Order.OrderStatus.PARTIALLY_FILLED).remainingQuantity(2)
                .build();
        o.prePersist();
        assertEquals(now, o.getCreatedAt());
        assertEquals(Order.OrderStatus.PARTIALLY_FILLED, o.getStatus());
        assertEquals(2, o.getRemainingQuantity());
    }

    @Test
    void orderTypeEnum() {
        assertEquals(2, Order.OrderType.values().length);
        assertEquals(Order.OrderType.BUY, Order.OrderType.valueOf("BUY"));
        assertEquals(Order.OrderType.SELL, Order.OrderType.valueOf("SELL"));
    }

    @Test
    void orderStatusEnum() {
        assertEquals(4, Order.OrderStatus.values().length);
        assertEquals(Order.OrderStatus.PENDING, Order.OrderStatus.valueOf("PENDING"));
        assertEquals(Order.OrderStatus.PARTIALLY_FILLED, Order.OrderStatus.valueOf("PARTIALLY_FILLED"));
        assertEquals(Order.OrderStatus.FILLED, Order.OrderStatus.valueOf("FILLED"));
        assertEquals(Order.OrderStatus.CANCELLED, Order.OrderStatus.valueOf("CANCELLED"));
    }

    @Test
    void orderSetters() {
        User u = User.builder().id(1L).username("u").email("e").password("p")
                .role(User.Role.USER).balance(BigDecimal.ZERO).build();
        Player p = Player.builder().name("P1").team("T").build();
        Order o = new Order();
        o.setUser(u);
        o.setPlayer(p);
        o.setType(Order.OrderType.BUY);
        o.setQuantity(3);
        o.setPriceAtOrder(new BigDecimal("30"));
        o.setTotal(new BigDecimal("90"));
        o.setIdempotencyKey("k");
        o.setStatus(Order.OrderStatus.FILLED);
        o.setRemainingQuantity(0);
        assertSame(u, o.getUser());
        assertSame(p, o.getPlayer());
        assertEquals(Order.OrderType.BUY, o.getType());
        assertEquals(3, o.getQuantity());
        assertEquals(new BigDecimal("30"), o.getPriceAtOrder());
        assertEquals(new BigDecimal("90"), o.getTotal());
        assertEquals("k", o.getIdempotencyKey());
        assertEquals(Order.OrderStatus.FILLED, o.getStatus());
        assertEquals(0, o.getRemainingQuantity());
    }

    // ── Portfolio ──

    @Test
    void portfolioBuilder() {
        User u = User.builder().id(1L).username("u").email("e").password("p")
                .role(User.Role.USER).balance(BigDecimal.ZERO).build();
        Player p = Player.builder().name("P1").team("T").build();
        Portfolio port = Portfolio.builder()
                .id(1L).user(u).player(p).tokenQty(50)
                .avgBuyPrice(new BigDecimal("80"))
                .build();
        assertEquals(1L, port.getId());
        assertSame(u, port.getUser());
        assertSame(p, port.getPlayer());
        assertEquals(50, port.getTokenQty());
        assertEquals(new BigDecimal("80"), port.getAvgBuyPrice());
    }

    @Test
    void portfolioSetters() {
        Portfolio port = new Portfolio();
        port.setTokenQty(10);
        port.setAvgBuyPrice(new BigDecimal("100"));
        assertEquals(10, port.getTokenQty());
        assertEquals(new BigDecimal("100"), port.getAvgBuyPrice());
    }

    // ── Quote ──

    @Test
    void quoteBuilder() {
        Player p = Player.builder().name("P1").team("T").build();
        Quote q = Quote.builder()
                .id(1L).player(p).price(new BigDecimal("150"))
                .strategyId(1L).strategyVersion(1)
                .trigger(QuoteTrigger.MANUAL)
                .build();
        q.prePersist();
        assertEquals(1L, q.getId());
        assertSame(p, q.getPlayer());
        assertEquals(new BigDecimal("150"), q.getPrice());
        assertEquals(1L, q.getStrategyId());
        assertEquals(Integer.valueOf(1), q.getStrategyVersion());
        assertEquals(QuoteTrigger.MANUAL, q.getTrigger());
        assertNotNull(q.getTimestamp());
        assertNotNull(q.getLastModifiedAt());
    }

    @Test
    void quotePrePersistDefaults() {
        Player p = Player.builder().name("P1").team("T").build();
        Quote q = Quote.builder().player(p).price(new BigDecimal("100")).build();
        q.prePersist();
        assertNotNull(q.getTimestamp());
        assertNotNull(q.getLastModifiedAt());
    }

    @Test
    void quotePreUpdate() {
        Player p = Player.builder().name("P1").team("T").build();
        Quote q = Quote.builder().player(p).price(new BigDecimal("100")).build();
        q.prePersist();
        LocalDateTime before = q.getLastModifiedAt();
        q.preUpdate();
        assertTrue(q.getLastModifiedAt().isAfter(before) || q.getLastModifiedAt().equals(before));
    }

    // ── QuoteTrigger ──

    @Test
    void quoteTriggerEnum() {
        assertEquals(2, QuoteTrigger.values().length);
        assertEquals(QuoteTrigger.MANUAL, QuoteTrigger.valueOf("MANUAL"));
        assertEquals(QuoteTrigger.SCHEDULED, QuoteTrigger.valueOf("SCHEDULED"));
    }

    // ── StrategyConfig ──

    @Test
    void strategyConfigBuilder() {
        Map<String, BigDecimal> weights = Map.of("goals", new BigDecimal("0.5"), "assists", new BigDecimal("0.3"));
        StrategyConfig sc = StrategyConfig.builder()
                .id(1L).valorBase(new BigDecimal("1")).factorEscala(new BigDecimal("10"))
                .type(StrategyConfig.StrategyType.GENERAL).version(1)
                .weights(weights)
                .build();
        assertEquals(1L, sc.getId());
        assertEquals(new BigDecimal("1"), sc.getValorBase());
        assertEquals(new BigDecimal("10"), sc.getFactorEscala());
        assertEquals(StrategyConfig.StrategyType.GENERAL, sc.getType());
        assertEquals(Integer.valueOf(1), sc.getVersion());
        assertEquals(2, sc.getWeights().size());
    }

    @Test
    void strategyConfigDefaultWeights() {
        StrategyConfig sc = StrategyConfig.builder()
                .valorBase(new BigDecimal("1")).factorEscala(new BigDecimal("10"))
                .type(StrategyConfig.StrategyType.FORWARD).version(1)
                .build();
        assertNotNull(sc.getWeights());
        assertTrue(sc.getWeights().isEmpty());
    }

    @Test
    void strategyConfigSetters() {
        StrategyConfig sc = new StrategyConfig();
        sc.setValorBase(new BigDecimal("2"));
        sc.setFactorEscala(new BigDecimal("20"));
        sc.setType(StrategyConfig.StrategyType.DEFENDER);
        sc.setVersion(2);
        assertEquals(new BigDecimal("2"), sc.getValorBase());
        assertEquals(new BigDecimal("20"), sc.getFactorEscala());
        assertEquals(StrategyConfig.StrategyType.DEFENDER, sc.getType());
        assertEquals(Integer.valueOf(2), sc.getVersion());
    }

    @Test
    void strategyConfigTypeEnum() {
        assertEquals(5, StrategyConfig.StrategyType.values().length);
        assertTrue(contains(StrategyConfig.StrategyType.values(), StrategyConfig.StrategyType.GENERAL));
        assertTrue(contains(StrategyConfig.StrategyType.values(), StrategyConfig.StrategyType.FORWARD));
        assertTrue(contains(StrategyConfig.StrategyType.values(), StrategyConfig.StrategyType.MIDFIELDER));
        assertTrue(contains(StrategyConfig.StrategyType.values(), StrategyConfig.StrategyType.DEFENDER));
        assertTrue(contains(StrategyConfig.StrategyType.values(), StrategyConfig.StrategyType.GOALKEEPER));
    }

    // ── TeamEnum ──

    @Test
    void teamEnum_fromId_found() {
        assertEquals(TeamEnum.BARCELONA, TeamEnum.fromId(81));
        assertEquals(TeamEnum.REAL_MADRID, TeamEnum.fromId(86));
        assertEquals(TeamEnum.LIVERPOOL, TeamEnum.fromId(64));
        assertEquals(TeamEnum.BAYERN, TeamEnum.fromId(5));
        assertEquals(TeamEnum.PSG, TeamEnum.fromId(524));
        assertEquals(TeamEnum.MILAN, TeamEnum.fromId(98));
    }

    @Test
    void teamEnum_fromId_notFound() {
        assertThrows(ResourceNotFoundException.class, () -> TeamEnum.fromId(-1));
    }

    @Test
    void teamEnum_fields() {
        assertEquals(81, TeamEnum.BARCELONA.getId());
        assertEquals("Barcelona", TeamEnum.BARCELONA.getName());
        assertEquals(LeagueConstant.LALIGA, TeamEnum.BARCELONA.getLeague());
        assertEquals(LeagueConstant.PREMIER_LEAGUE, TeamEnum.LIVERPOOL.getLeague());
        assertEquals(LeagueConstant.SERIE_A, TeamEnum.INTER.getLeague());
        assertEquals(LeagueConstant.BUNDESLIGA, TeamEnum.BAYERN.getLeague());
        assertEquals(LeagueConstant.LIGUE_1, TeamEnum.PSG.getLeague());
    }

    // ── LeagueConstant ──

    @Test
    void leagueConstant_values() {
        assertEquals("LaLiga", LeagueConstant.LALIGA);
        assertEquals("Premier League", LeagueConstant.PREMIER_LEAGUE);
        assertEquals("Ligue 1", LeagueConstant.LIGUE_1);
        assertEquals("Bundesliga", LeagueConstant.BUNDESLIGA);
        assertEquals("Serie A", LeagueConstant.SERIE_A);
    }

    @Test
    void leagueConstant_utilityClass() throws Exception {
        Constructor<LeagueConstant> c = LeagueConstant.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(c.getModifiers()));
        c.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class, c::newInstance);
        assertInstanceOf(AssertionError.class, ex.getCause());
    }

    // ── helpers ──

    private static <T extends Enum<T>> boolean contains(T[] values, T target) {
        for (T v : values) if (v == target) return true;
        return false;
    }
}
