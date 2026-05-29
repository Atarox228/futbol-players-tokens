# PlayerScraperServiceImpl - Refactoring Implementation Guide

## Quick Reference: Before & After Examples

This guide provides practical code examples for implementing each refactoring pattern.

---

## Pattern 1: Three Extractors → Generic Lambda-Based Extractor

### Before (147 lines across 3 methods)
```java
// extractSummaryPlayerFromRow - 47 lines
private PlayerDetailDTO extractSummaryPlayerFromRow(WebElement row) {
    try {
        PlayerDetailDTO player = createPlayerFromRow(row);
        if (player == null) {
            return null;
        }
        List<WebElement> cells = row.findElements(By.tagName("td"));
        if (cells.size() > 4) {
            player.setAppearances(parseAppearances(cells.get(4).getText()));
        }
        if (cells.size() > 5) {
            player.setMinutes(parseIntegerStat(cells.get(5).getText()));
        }
        // ... 9 more cells ... (repeated pattern)
        return player;
    } catch (Exception e) {
        return null;
    }
}

// extractDefensivePlayerFromRow - 46 lines
private PlayerDetailDTO extractDefensivePlayerFromRow(WebElement row) {
    // IDENTICAL PATTERN with different column indices and setters
    // 46 lines of nearly identical code
}

// extractOffensivePlayerFromRow - 49 lines  
private PlayerDetailDTO extractOffensivePlayerFromRow(WebElement row) {
    // IDENTICAL PATTERN with different column indices and setters
    // 49 lines of nearly identical code
}
```

### After (60 lines total - 87 line reduction)
```java
// 1. Define the record for column mappings
private record ColumnMapping(
    int index,
    java.util.function.BiConsumer<PlayerDetailDTO, String> setter
) {}

// 2. Create mapping constants as static fields
private static final List<ColumnMapping> SUMMARY_MAPPINGS = List.of(
    new ColumnMapping(4, (p, v) -> p.setAppearances(parseAppearances(v))),
    new ColumnMapping(5, (p, v) -> p.setMinutes(parseIntegerStat(v))),
    new ColumnMapping(6, (p, v) -> p.setGoals(parseIntegerStat(v))),
    new ColumnMapping(7, (p, v) -> p.setAssists(parseIntegerStat(v))),
    new ColumnMapping(8, (p, v) -> p.setYellowCards(parseIntegerStat(v))),
    new ColumnMapping(9, (p, v) -> p.setRedCards(parseIntegerStat(v))),
    new ColumnMapping(10, (p, v) -> p.setShotsOnTarget(parseDecimalStat(v))),
    new ColumnMapping(11, (p, v) -> p.setPassAccuracy(parseDecimalStat(v))),
    new ColumnMapping(12, (p, v) -> p.setAerialWon(parseDecimalStat(v))),
    new ColumnMapping(13, (p, v) -> p.setPlayerOfTheMatch(parseIntegerStat(v))),
    new ColumnMapping(14, (p, v) -> p.setRating(parseDecimalStat(v)))
);

private static final List<ColumnMapping> DEFENSIVE_MAPPINGS = List.of(
    new ColumnMapping(4, (p, v) -> p.setAppearances(parseAppearances(v))),
    new ColumnMapping(5, (p, v) -> p.setMinutes(parseIntegerStat(v))),
    new ColumnMapping(6, (p, v) -> p.setTackles(parseDecimalStat(v))),
    new ColumnMapping(7, (p, v) -> p.setInterceptions(parseDecimalStat(v))),
    new ColumnMapping(8, (p, v) -> p.setFaults(parseDecimalStat(v))),
    new ColumnMapping(9, (p, v) -> p.setOffsidesGiven(parseDecimalStat(v))),
    new ColumnMapping(10, (p, v) -> p.setClears(parseDecimalStat(v))),
    new ColumnMapping(11, (p, v) -> p.setDribbled(parseDecimalStat(v))),
    new ColumnMapping(12, (p, v) -> p.setBlocks(parseDecimalStat(v))),
    new ColumnMapping(13, (p, v) -> p.setOwnGoals(parseIntegerStat(v))),
    new ColumnMapping(14, (p, v) -> p.setRating(parseDecimalStat(v)))
);

private static final List<ColumnMapping> OFFENSIVE_MAPPINGS = List.of(
    new ColumnMapping(4, (p, v) -> p.setAppearances(parseAppearances(v))),
    new ColumnMapping(5, (p, v) -> p.setMinutes(parseIntegerStat(v))),
    new ColumnMapping(6, (p, v) -> p.setGoals(parseIntegerStat(v))),
    new ColumnMapping(7, (p, v) -> p.setAssists(parseIntegerStat(v))),
    new ColumnMapping(8, (p, v) -> p.setShotsOnTarget(parseDecimalStat(v))),
    new ColumnMapping(9, (p, v) -> p.setKeyPasses(parseDecimalStat(v))),
    new ColumnMapping(10, (p, v) -> p.setDribbles(parseDecimalStat(v))),
    new ColumnMapping(11, (p, v) -> p.setFaulted(parseDecimalStat(v))),
    new ColumnMapping(12, (p, v) -> p.setOffsides(parseDecimalStat(v))),
    new ColumnMapping(13, (p, v) -> p.setDispossesed(parseDecimalStat(v))),
    new ColumnMapping(14, (p, v) -> p.setTurnover(parseDecimalStat(v))),
    new ColumnMapping(15, (p, v) -> p.setRating(parseDecimalStat(v)))
);

// 3. Single generic extraction method
private PlayerDetailDTO extractPlayerFromRowWithMapping(WebElement row, List<ColumnMapping> mappings) {
    try {
        PlayerDetailDTO player = createPlayerFromRow(row);
        if (player == null) {
            return null;
        }

        List<WebElement> cells = row.findElements(By.tagName("td"));
        for (ColumnMapping mapping : mappings) {
            if (cells.size() > mapping.index()) {
                try {
                    String cellText = cells.get(mapping.index()).getText();
                    mapping.setter().accept(player, cellText);
                } catch (Exception e) {
                    // Continue to next cell
                }
            }
        }

        return player;
    } catch (Exception e) {
        return null;
    }
}

// 4. Replace the three methods
private PlayerDetailDTO extractSummaryPlayerFromRow(WebElement row) {
    return extractPlayerFromRowWithMapping(row, SUMMARY_MAPPINGS);
}

private PlayerDetailDTO extractDefensivePlayerFromRow(WebElement row) {
    return extractPlayerFromRowWithMapping(row, DEFENSIVE_MAPPINGS);
}

private PlayerDetailDTO extractOffensivePlayerFromRow(WebElement row) {
    return extractPlayerFromRowWithMapping(row, OFFENSIVE_MAPPINGS);
}
```

**Benefits:**
- Easy to add new stat columns - just add one line to appropriate mapping
- Column indices in one place, easy to verify against HTML structure
- Logic changes apply to all three extractors automatically
- Testable: can test mapping definitions separately from extraction

---

## Pattern 2: Two Player Data Extractors → Parameterized Generic

### Before (218 lines across 2 methods)
```java
// extractPlayerData - 118 lines (lines 1191-1309)
private PlayerDetailDTO extractPlayerData(WebElement row) {
    try {
        WebElement playerLink = row.findElement(By.cssSelector(CSS_PLAYER_LINK_SPAN));
        String name = playerLink.getText().trim();
        if (name.isEmpty() || name.length() < 2) {
            return null;
        }

        PlayerDetailDTO player = PlayerDetailDTO.builder().build();
        player.setName(name);

        // Extract team
        try {
            WebElement teamElement = row.findElement(By.cssSelector(CSS_PLAYER_META_TEAM_NAME));
            player.setTeam(teamElement.getText().trim().replaceAll(",\\s*$", EMPTY));
        } catch (NoSuchElementException e) {
            player.setTeam(EMPTY);
        }

        // Extract position - uses CSS_NESTED_PLAYER_META_DATA
        try {
            List<WebElement> positionSpans = row.findElements(By.cssSelector(CSS_NESTED_PLAYER_META_DATA));
            if (positionSpans.size() >= 2) {
                String position = positionSpans.get(1).getText().trim().replaceAll("^,\\s*", "");
                player.setPosition(position);
            } else {
                player.setPosition(EMPTY);
            }
        } catch (NoSuchElementException e) {
            player.setPosition(EMPTY);
        }

        List<WebElement> cells = row.findElements(By.tagName("td"));

        if (cells.size() > 2) {
            player.setAppearances(parseAppearances(cells.get(2).getText()));
        }
        if (cells.size() > 3) {
            try {
                String text = cells.get(3).getText().trim().replaceAll(REGEX_NON_NUMERIC, EMPTY);
                player.setMinutes(!text.isEmpty() ? Integer.parseInt(text) : 0);
            } catch (NumberFormatException e) {
                player.setMinutes(0);
            }
        }
        // ... repeated 8+ more times with different indices ...
        return player;
    } catch (Exception e) {
        return null;
    }
}

// extractPlayerDataFromRoster - 108 lines (lines 1058-1166)
// ALMOST IDENTICAL except:
// - Uses indices 4,5,6,7,8,9,13,14 instead of 2,3,4,5,6,7,11,12
// - Uses CSS_PLAYER_META_DATA instead of CSS_NESTED_PLAYER_META_DATA
// - Doesn't extract team
// - Has different null check patterns
```

### After (80 lines total - 138 line reduction)
```java
// 1. Configuration record
private record ExtractionConfig(
    int appearancesIndex,
    int minutesIndex,
    int goalsIndex,
    int assistsIndex,
    int yellowCardsIndex,
    int redCardsIndex,
    int playerOfMatchIndex,
    int ratingIndex,
    boolean includeTeam,
    boolean includeFullPosition
) {}

// 2. Static configurations
private static final ExtractionConfig LEAGUE_PAGE_CONFIG = new ExtractionConfig(
    2, 3, 4, 5, 6, 7, 11, 12, true, true
);

private static final ExtractionConfig ROSTER_PAGE_CONFIG = new ExtractionConfig(
    4, 5, 6, 7, 8, 9, 13, 14, false, false
);

// 3. Helper to safely set cell values
private void setCellValue(PlayerDetailDTO player, List<WebElement> cells, int index,
                         java.util.function.BiConsumer<PlayerDetailDTO, String> setter) {
    if (cells.size() > index) {
        try {
            setter.accept(player, cells.get(index).getText());
        } catch (Exception e) {
            // Skip this cell
        }
    }
}

// 4. Generic extraction method
private PlayerDetailDTO extractPlayerDataGeneric(WebElement row, ExtractionConfig config) {
    try {
        WebElement playerLink = row.findElement(By.cssSelector(CSS_PLAYER_LINK_SPAN));
        String name = playerLink.getText().trim();

        if (name.isEmpty() || name.length() < 2) {
            return null;
        }

        PlayerDetailDTO player = PlayerDetailDTO.builder().build();
        player.setName(name);

        // Extract team if needed
        if (config.includeTeam()) {
            try {
                WebElement teamElement = row.findElement(By.cssSelector(CSS_PLAYER_META_TEAM_NAME));
                player.setTeam(teamElement.getText().trim().replaceAll(",\\s*$", EMPTY));
            } catch (NoSuchElementException e) {
                player.setTeam(EMPTY);
            }
        }

        // Extract position
        try {
            String positionSelector = config.includeFullPosition() ? 
                CSS_NESTED_PLAYER_META_DATA : CSS_PLAYER_META_DATA;
            List<WebElement> positionSpans = row.findElements(By.cssSelector(positionSelector));
            if (positionSpans.size() >= 2) {
                String position = positionSpans.get(1).getText().trim().replaceAll("^,\\s*", "");
                player.setPosition(position);
            } else {
                player.setPosition(EMPTY);
            }
        } catch (NoSuchElementException e) {
            player.setPosition(EMPTY);
        }

        // Extract cells using configuration
        List<WebElement> cells = row.findElements(By.tagName("td"));
        
        setCellValue(player, cells, config.appearancesIndex(), 
            (p, v) -> p.setAppearances(parseAppearances(v)));
        setCellValue(player, cells, config.minutesIndex(), 
            (p, v) -> p.setMinutes(parseIntegerStat(v)));
        setCellValue(player, cells, config.goalsIndex(), 
            (p, v) -> p.setGoals(parseIntegerStat(v)));
        setCellValue(player, cells, config.assistsIndex(), 
            (p, v) -> p.setAssists(parseIntegerStat(v)));
        setCellValue(player, cells, config.yellowCardsIndex(), 
            (p, v) -> p.setYellowCards(parseIntegerStat(v)));
        setCellValue(player, cells, config.redCardsIndex(), 
            (p, v) -> p.setRedCards(parseIntegerStat(v)));
        setCellValue(player, cells, config.playerOfMatchIndex(), 
            (p, v) -> p.setPlayerOfTheMatch(parseIntegerStat(v)));
        setCellValue(player, cells, config.ratingIndex(), 
            (p, v) -> p.setRating(parseDecimalStat(v)));

        return player;
    } catch (Exception e) {
        return null;
    }
}

// 5. Replace both methods
private PlayerDetailDTO extractPlayerData(WebElement row) {
    return extractPlayerDataGeneric(row, LEAGUE_PAGE_CONFIG);
}

private PlayerDetailDTO extractPlayerDataFromRoster(WebElement row) {
    return extractPlayerDataGeneric(row, ROSTER_PAGE_CONFIG);
}
```

**Benefits:**
- To support a new page layout: just create a new ExtractionConfig
- All column index mappings visible in one place
- Easier to debug: can trace which config is being used
- Cell parsing logic centralized in setCellValue

---

## Pattern 3: Duplicate Driver Setup → Extracted Manager

### Before (63 lines across 2 methods)
```java
@Override
public List<PlayerDetailDTO> scrapeTeamPlayersByName(String teamName, String league) {
    String baseUrl = getBaseUrlByLeague(league);

    ChromeOptions options = createChromeOptions();
    WebDriver driver = createDriver(options);
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(12));
    List<PlayerDetailDTO> newPlayers = new ArrayList<>();

    try {
        driver.get(baseUrl);
        Thread.sleep(2000);

        closePopupIfPresent(driver, wait);
        selectTeamFromDropdown(driver, wait, teamName);

        newPlayers.addAll(scrapeCurrentTeamRoster(driver, wait, teamName, league));

        return newPlayers;
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("❌ Error scrapeando plantilla de " + teamName + ": " + e.getMessage(), e);

    } catch (Exception e) {
        throw new RuntimeException("❌ Error scrapeando plantilla de " + teamName + ": " + e.getMessage(), e);
    } finally {
        driver.quit();
    }
}

@Override
public List<PlayerDetailDTO> scrapeLeaguePlayersByStarterTeam(String starterTeam, String league) {
    String baseUrl = getBaseUrlByLeague(league);

    ChromeOptions options = createChromeOptions();
    WebDriver driver = createDriver(options);
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(12));
    List<PlayerDetailDTO> newPlayers = new ArrayList<>();

    try {
        driver.get(baseUrl);
        Thread.sleep(2000);

        closePopupIfPresent(driver, wait);
        selectTeamFromDropdown(driver, wait, starterTeam);

        List<String> teamNames = getTeamNamesFromDropdown(driver, wait);
        List<String> orderedTeamNames = orderTeamsStartingWith(teamNames, starterTeam);

        for (String currentTeamName : orderedTeamNames) {
            selectTeamFromDropdown(driver, wait, currentTeamName);
            newPlayers.addAll(scrapeCurrentTeamRoster(driver, wait, currentTeamName, league));
        }

        return newPlayers;
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("❌ Error scrapeando liga " + league + ": " + e.getMessage(), e);
    } catch (Exception e) {
        throw new RuntimeException("❌ Error scrapeando liga " + league + ": " + e.getMessage(), e);
    } finally {
        driver.quit();
    }
}
```

### After (35 lines total - 28 line reduction)
```java
// 1. Define functional interface for driver tasks
@FunctionalInterface
private interface DriverTask {
    List<PlayerDetailDTO> execute(WebDriver driver, WebDriverWait wait) throws InterruptedException;
}

// 2. Centralized driver session manager
private List<PlayerDetailDTO> executeWithDriver(String baseUrl, String errorContext, DriverTask task) {
    ChromeOptions options = createChromeOptions();
    WebDriver driver = createDriver(options);
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(12));
    
    try {
        driver.get(baseUrl);
        Thread.sleep(2000);
        closePopupIfPresent(driver, wait);
        return task.execute(driver, wait);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("❌ Error scrapeando " + errorContext + ": " + e.getMessage(), e);
    } catch (Exception e) {
        throw new RuntimeException("❌ Error scrapeando " + errorContext + ": " + e.getMessage(), e);
    } finally {
        driver.quit();
    }
}

// 3. Simplified methods using the manager
@Override
public List<PlayerDetailDTO> scrapeTeamPlayersByName(String teamName, String league) {
    return executeWithDriver(
        getBaseUrlByLeague(league),
        "plantilla de " + teamName,
        (driver, wait) -> {
            selectTeamFromDropdown(driver, wait, teamName);
            return scrapeCurrentTeamRoster(driver, wait, teamName, league);
        }
    );
}

@Override
public List<PlayerDetailDTO> scrapeLeaguePlayersByStarterTeam(String starterTeam, String league) {
    return executeWithDriver(
        getBaseUrlByLeague(league),
        "liga " + league,
        (driver, wait) -> {
            selectTeamFromDropdown(driver, wait, starterTeam);
            List<String> teamNames = getTeamNamesFromDropdown(driver, wait);
            List<String> orderedTeamNames = orderTeamsStartingWith(teamNames, starterTeam);
            
            List<PlayerDetailDTO> allPlayers = new ArrayList<>();
            for (String currentTeamName : orderedTeamNames) {
                selectTeamFromDropdown(driver, wait, currentTeamName);
                allPlayers.addAll(scrapeCurrentTeamRoster(driver, wait, currentTeamName, league));
            }
            return allPlayers;
        }
    );
}
```

**Benefits:**
- All driver lifecycle logic in one place
- New methods can reuse executeWithDriver without duplication
- Consistent error handling across all driver-based operations
- Easier to add driver pooling/reuse in future

---

## Pattern 4: Identical League Loops → Extracted Config & Method

### Before (64 lines across 2 methods)
```java
@Override
public void scrapeAllPlayersIfDatabaseEmpty() {
    long playerCount = playerRepository.count();

    if (playerCount > 0) {
        logger.info("⏭️ BD no está vacía. Saltando scraping automático. Jugadores en BD: {}", playerCount);
        return;
    }

    logger.info("🚀 BD vacía detectada. Iniciando scraping automático de todos los jugadores...");

    Map<String, String> ligas = new LinkedHashMap<>();
    ligas.put("Ligue 1", "Angers");
    ligas.put("LaLiga", "Athletic Club");
    ligas.put("Premier League", "Arsenal");
    ligas.put("Bundesliga", "Augsburg");
    ligas.put("Serie A", "AC Milan");

    int totalJugadores = 0;

    try {
        for (Map.Entry<String, String> liga : ligas.entrySet()) {
            logger.info("📊 Scrapeando {}...", liga.getKey());
            var jugadores = scrapeLeaguePlayersByStarterTeam(liga.getValue(), liga.getKey());
            totalJugadores += jugadores.size();
        }

        logger.info("✅ Scraping automático completado. Total: {} jugadores guardados", totalJugadores);
    } catch (Exception e) {
        logger.error("❌ Error en scraping automático: {}", e.getMessage());
    }
}

@Override
public void scrapeAllPlayersForce() {
    logger.info("🚀 Forzando scraping completo: limpiando BD y scrapeando todas las ligas...");

    // Limpiar la tabla de players antes de iniciar
    long count = playerRepository.count();
    if (count > 0) {
        playerRepository.deleteAll();
        logger.info("🧹 BD limpiada. Registros eliminados: {}", count);
    }

    // IDENTICAL CONFIGURATION REPEATED
    Map<String, String> ligas = new LinkedHashMap<>();
    ligas.put("Ligue 1", "Angers");
    ligas.put("LaLiga", "Athletic Club");
    ligas.put("Premier League", "Arsenal");
    ligas.put("Bundesliga", "Augsburg");
    ligas.put("Serie A", "AC Milan");

    // IDENTICAL SCRAPING LOOP
    int totalJugadores = 0;

    try {
        for (Map.Entry<String, String> liga : ligas.entrySet()) {
            logger.info("📊 Scrapeando {}...", liga.getKey());
            var jugadores = scrapeLeaguePlayersByStarterTeam(liga.getValue(), liga.getKey());
            totalJugadores += jugadores.size();
        }

        logger.info("✅ Scraping forzado completado. Total: {} jugadores guardados", totalJugadores);
    } catch (Exception e) {
        logger.error("❌ Error en scraping forzado: {}", e.getMessage());
    }
}
```

### After (30 lines total - 34 line reduction)
```java
// 1. Static configuration constant
private static final Map<String, String> LEAGUES_CONFIG = 
    Map.ofEntries(
        Map.entry("Ligue 1", "Angers"),
        Map.entry("LaLiga", "Athletic Club"),
        Map.entry("Premier League", "Arsenal"),
        Map.entry("Bundesliga", "Augsburg"),
        Map.entry("Serie A", "AC Milan")
    );

// 2. Extracted common scraping logic
private int scrapeAllLeagues() {
    int totalJugadores = 0;
    try {
        for (Map.Entry<String, String> liga : LEAGUES_CONFIG.entrySet()) {
            logger.info("📊 Scrapeando {}...", liga.getKey());
            var jugadores = scrapeLeaguePlayersByStarterTeam(liga.getValue(), liga.getKey());
            totalJugadores += jugadores.size();
        }
        logger.info("✅ Scraping completado. Total: {} jugadores guardados", totalJugadores);
    } catch (Exception e) {
        logger.error("❌ Error en scraping: {}", e.getMessage());
    }
    return totalJugadores;
}

// 3. Simplified public methods
@Override
public void scrapeAllPlayersIfDatabaseEmpty() {
    long playerCount = playerRepository.count();
    if (playerCount > 0) {
        logger.info("⏭️ BD no está vacía. Saltando scraping automático. Jugadores en BD: {}", playerCount);
        return;
    }
    logger.info("🚀 BD vacía detectada. Iniciando scraping automático...");
    scrapeAllLeagues();
}

@Override
public void scrapeAllPlayersForce() {
    logger.info("🚀 Forzando scraping completo: limpiando BD y scrapeando todas las ligas...");
    long count = playerRepository.count();
    if (count > 0) {
        playerRepository.deleteAll();
        logger.info("🧹 BD limpiada. Registros eliminados: {}", count);
    }
    scrapeAllLeagues();
}
```

**Benefits:**
- Leagues configuration in single place - easy to add/remove/reorder
- Scraping loop logic in one method
- Each public method focuses on its unique responsibility
- Logging messages consistent
- If league order needs to change, change in one place

---

## Pattern 5: Duplicate Player Persistence → Consolidated Method

### Before (52 lines across 2 methods)
```java
// persistScrapedPlayer - 11 lines
private void persistScrapedPlayer(PlayerDetailDTO player, List<PlayerDetailDTO> newPlayers) {
    List<Player> existingPlayers = playerRepository.findByNameIgnoreCaseAndTeamIgnoreCase(
            player.getName().trim(),
            player.getTeam().trim());

    if (existingPlayers.isEmpty()) {
        playerDesdeCero(player, playerRepository);
        newPlayers.add(player);
    } else {
        modificandoPlayer(player, existingPlayers, playerRepository);
    }
}

// processTeamPlayerRow - 27 lines
private void processTeamPlayerRow(WebElement row, String teamName, String league, List<PlayerDetailDTO> newPlayers) {
    try {
        if (shouldSkipRow(row)) {
            return;
        }

        PlayerDetailDTO player = extractPlayerDataFromRoster(row);
        if (player == null || player.getName() == null || player.getName().isBlank()) {
            return;
        }

        player.setTeam(teamName);
        player.setLeague(league);

        List<Player> existingPlayers = playerRepository.findByNameIgnoreCaseAndTeamIgnoreCase(
                player.getName().trim(),
                player.getTeam().trim());

        if (existingPlayers.isEmpty()) {
            playerDesdeCero(player, playerRepository);
            newPlayers.add(player);
        } else {
            modificandoPlayer(player, existingPlayers, playerRepository);
        }
    } catch (Exception e) {
        // Continuar con el siguiente jugador
    }
}
```

### After (25 lines total - 27 line reduction)
```java
// 1. Single consolidated persistence method
private void persistOrUpdatePlayer(PlayerDetailDTO player, List<PlayerDetailDTO> newPlayersTracker) {
    List<Player> existingPlayers = playerRepository.findByNameIgnoreCaseAndTeamIgnoreCase(
            player.getName().trim(),
            player.getTeam().trim());

    if (existingPlayers.isEmpty()) {
        playerDesdeCero(player, playerRepository);
        newPlayersTracker.add(player);
    } else {
        modificandoPlayer(player, existingPlayers, playerRepository);
    }
}

// 2. Remove persistScrapedPlayer entirely (update all call sites to use persistOrUpdatePlayer)

// 3. Simplified processTeamPlayerRow
private void processTeamPlayerRow(WebElement row, String teamName, String league, List<PlayerDetailDTO> newPlayers) {
    try {
        if (shouldSkipRow(row)) {
            return;
        }

        PlayerDetailDTO player = extractPlayerDataFromRoster(row);
        if (player == null || player.getName() == null || player.getName().isBlank()) {
            return;
        }

        player.setTeam(teamName);
        player.setLeague(league);
        persistOrUpdatePlayer(player, newPlayers);  // Single call
    } catch (Exception e) {
        // Continue with next player
    }
}
```

**Call site updates:**
```java
// Before (line 421):
persistScrapedPlayer(player, newPlayers);

// After:
persistOrUpdatePlayer(player, newPlayers);
```

**Benefits:**
- Single source of truth for player persistence logic
- Easier to audit: all player saves go through one method
- Consistent behavior across all code paths
- Reduced cognitive load: one method to understand

---

## Bonus: Popup Closing Simplification

### Before (43 lines)
```java
private void closePopupIfPresent(WebDriver driver, WebDriverWait wait) {
    try {
        try {
            WebElement acceptButton = null;

            try {
                acceptButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath(XPATH_ACCEPTAR_TODO)));
            } catch (TimeoutException e1) {
                try {
                    acceptButton = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath(XPATH_ACCEPT_ALL)));
                } catch (TimeoutException e2) {
                    try {
                        acceptButton = wait.until(ExpectedConditions.elementToBeClickable(
                            By.xpath(XPATH_ACCEPTAR)));
                    } catch (TimeoutException e3) {
                        try {
                            acceptButton = wait.until(ExpectedConditions.elementToBeClickable(
                                By.xpath(XPATH_ACCEPT)));
                        } catch (TimeoutException e4) {
                            acceptButton = wait.until(ExpectedConditions.elementToBeClickable(
                                By.cssSelector(CSS_COOKIE_ACCEPT_ALL)));
                        }
                    }
                }
            }

            if (acceptButton != null && acceptButton.isDisplayed()) {
                ((JavascriptExecutor) driver).executeScript(JS_CLICK_ELEMENT, acceptButton);
                Thread.sleep(2000);
            }
        } catch (TimeoutException e) {
            // No popup, continue
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } catch (Exception e) {
        // Ignore popup errors
    }
}
```

### After (15 lines)
```java
private void closePopupIfPresent(WebDriver driver, WebDriverWait wait) {
    try {
        WebElement acceptButton = findAcceptButton(driver, wait);
        if (acceptButton != null && acceptButton.isDisplayed()) {
            ((JavascriptExecutor) driver).executeScript(JS_CLICK_ELEMENT, acceptButton);
            Thread.sleep(2000);
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } catch (Exception e) {
        // Ignore popup errors
    }
}

private WebElement findAcceptButton(WebDriver driver, WebDriverWait wait) {
    List<By> selectors = List.of(
        By.xpath(XPATH_ACCEPTAR_TODO),
        By.xpath(XPATH_ACCEPT_ALL),
        By.xpath(XPATH_ACCEPTAR),
        By.xpath(XPATH_ACCEPT),
        By.cssSelector(CSS_COOKIE_ACCEPT_ALL)
    );
    
    for (By selector : selectors) {
        try {
            return wait.until(ExpectedConditions.elementToBeClickable(selector));
        } catch (TimeoutException e) {
            // Try next selector
        }
    }
    return null;
}
```

**Benefits:**
- Eliminates nested try-catch pyramid
- Button selectors clearly listed in sequence
- Easier to add new selector options
- Better readability

---

## Implementation Checklist

- [ ] Pattern 4 (League loops) - Easiest to implement, high impact
- [ ] Pattern 3 (Driver setup) - Simple, improves code organization  
- [ ] Pattern 5 (Player persistence) - Straightforward consolidation
- [ ] Pattern 2 (Player data extraction) - Medium complexity, major consolidation
- [ ] Pattern 1 (Three extractors) - Most complex, high payoff
- [ ] Bonus (Popup closing) - Nice-to-have cleanup

## Testing Strategy

For each refactoring:
1. **Run existing tests** to ensure backward compatibility
2. **Add unit tests** for new generic methods using different configurations
3. **Integration test** with actual Selenium to verify extraction still works
4. **Compare output** of old vs new methods on same data

## Files Modified

- `/src/main/java/com/desapp/futbolplayerstokens/service/impl/PlayerScraperServiceImpl.java`

## Estimated Time Impact

- Pattern 4: 30 minutes (trivial)
- Pattern 3: 45 minutes (simple, requires one interface + one method)
- Pattern 5: 20 minutes (straightforward)
- Pattern 2: 60 minutes (configuration objects + testing)
- Pattern 1: 90 minutes (mappings setup + testing)
- Bonus: 15 minutes

**Total: ~4 hours for complete refactoring with testing**

