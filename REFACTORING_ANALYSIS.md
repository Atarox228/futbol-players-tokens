# PlayerScraperServiceImpl Refactoring Analysis

## Executive Summary
Found 5 major duplicate code patterns that can be consolidated using generics, lambdas, and extracted methods. Estimated **~200 lines of code can be eliminated** while improving maintainability.

---

## Pattern 1: Three Extraction Methods with Different Column Indices
**Location:** Lines 834-980  
**Methods:**
- `extractSummaryPlayerFromRow()` (lines 834-881)
- `extractDefensivePlayerFromRow()` (lines 883-929)
- `extractOffensivePlayerFromRow()` (lines 931-980)

### Current Issues
All three methods follow identical structure:
1. Call `createPlayerFromRow(row)` 
2. Extract cells via `row.findElements(By.tagName("td"))`
3. Populate different stat fields with different column indices
4. Return player or null on exception

### Duplication Factor: ~150 lines (50 lines × 3 methods)

### Refactoring Solution: Use Lambda-Based Field Mapper
Create a generic extraction framework using functional interfaces:

```java
// Define a mapping between column index and player setter
private record ColumnMapping(int index, java.util.function.BiConsumer<PlayerDetailDTO, String> setter) {}

// Generic extraction method
private PlayerDetailDTO extractPlayerFromRowWithMapping(WebElement row, List<ColumnMapping> mappings) {
    try {
        PlayerDetailDTO player = createPlayerFromRow(row);
        if (player == null) {
            return null;
        }

        List<WebElement> cells = row.findElements(By.tagName("td"));
        for (ColumnMapping mapping : mappings) {
            if (cells.size() > mapping.index) {
                try {
                    String cellText = cells.get(mapping.index).getText();
                    mapping.setter.accept(player, cellText);
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

// Define mappings as static constants
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

// Replace all three methods with:
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

### Benefits
- Reduces code from 147 lines to ~60 lines
- Clear separation of column mappings from extraction logic
- Easy to modify column indices without touching extraction code
- Testable mapping configurations
- Single responsibility principle

---

## Pattern 2: Similar Player Data Extraction with Different Column Indices
**Location:** Lines 1058-1166 (extractPlayerDataFromRoster) & 1191-1309 (extractPlayerData)

### Current Issues
Both methods extract nearly identical data but use different column indices:
- `extractPlayerData`: Uses indices 2, 3, 4, 5, 6, 7, 11, 12
- `extractPlayerDataFromRoster`: Uses indices 4, 5, 6, 7, 8, 9, 13, 14

Both have ~110 lines of repetitive null checks and parsing.

### Duplication Factor: ~90 lines (45 lines × 2 methods + minor setup differences)

### Refactoring Solution: Parameterized Extraction Method

```java
// Define extraction configuration
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

private static final ExtractionConfig LEAGUE_PAGE_CONFIG = new ExtractionConfig(
    2, 3, 4, 5, 6, 7, 11, 12, true, true
);

private static final ExtractionConfig ROSTER_PAGE_CONFIG = new ExtractionConfig(
    4, 5, 6, 7, 8, 9, 13, 14, false, false
);

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
        if (config.includeTeam) {
            try {
                WebElement teamElement = row.findElement(By.cssSelector(CSS_PLAYER_META_TEAM_NAME));
                player.setTeam(teamElement.getText().trim().replaceAll(",\\s*$", EMPTY));
            } catch (NoSuchElementException e) {
                player.setTeam(EMPTY);
            }
        }

        // Extract position
        try {
            String positionSelector = config.includeFullPosition ? 
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

        // Extract cells with parameterized indices
        List<WebElement> cells = row.findElements(By.tagName("td"));
        
        setCellValue(player, cells, config.appearancesIndex, 
            (p, v) -> p.setAppearances(parseAppearances(v)));
        setCellValue(player, cells, config.minutesIndex, 
            (p, v) -> p.setMinutes(parseIntegerStat(v)));
        setCellValue(player, cells, config.goalsIndex, 
            (p, v) -> p.setGoals(parseIntegerStat(v)));
        setCellValue(player, cells, config.assistsIndex, 
            (p, v) -> p.setAssists(parseIntegerStat(v)));
        setCellValue(player, cells, config.yellowCardsIndex, 
            (p, v) -> p.setYellowCards(parseIntegerStat(v)));
        setCellValue(player, cells, config.redCardsIndex, 
            (p, v) -> p.setRedCards(parseIntegerStat(v)));
        setCellValue(player, cells, config.playerOfMatchIndex, 
            (p, v) -> p.setPlayerOfTheMatch(parseIntegerStat(v)));
        setCellValue(player, cells, config.ratingIndex, 
            (p, v) -> p.setRating(parseDecimalStat(v)));

        return player;
    } catch (Exception e) {
        return null;
    }
}

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

// Replace both methods:
private PlayerDetailDTO extractPlayerData(WebElement row) {
    return extractPlayerDataGeneric(row, LEAGUE_PAGE_CONFIG);
}

private PlayerDetailDTO extractPlayerDataFromRoster(WebElement row) {
    return extractPlayerDataGeneric(row, ROSTER_PAGE_CONFIG);
}
```

### Benefits
- Reduces code from 218 lines to ~80 lines
- Configuration-driven extraction logic
- Easy to add new extraction configurations
- Centralized cell value extraction logic
- Better testability via configuration objects

---

## Pattern 3: Driver and Wait Setup/Teardown Duplication
**Location:** Lines 293-320 (scrapeTeamPlayersByName) & 323-355 (scrapeLeaguePlayersByStarterTeam)

### Current Issues
Both methods contain identical setup:
```java
String baseUrl = getBaseUrlByLeague(league);
ChromeOptions options = createChromeOptions();
WebDriver driver = createDriver(options);
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(12));
List<PlayerDetailDTO> newPlayers = new ArrayList<>();

try {
    driver.get(baseUrl);
    Thread.sleep(2000);
    closePopupIfPresent(driver, wait);
    // ... different logic ...
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    throw new RuntimeException(...);
} catch (Exception e) {
    throw new RuntimeException(...);
} finally {
    driver.quit();
}
```

### Duplication Factor: ~25 lines of boilerplate per method

### Refactoring Solution: Extract Driver Session Manager

```java
@FunctionalInterface
private interface DriverTask {
    List<PlayerDetailDTO> execute(WebDriver driver, WebDriverWait wait) throws InterruptedException;
}

private List<PlayerDetailDTO> executeWithDriver(String baseUrl, String league, DriverTask task) {
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
        throw new RuntimeException("❌ Error scrapeando " + league + ": " + e.getMessage(), e);
    } catch (Exception e) {
        throw new RuntimeException("❌ Error scrapeando " + league + ": " + e.getMessage(), e);
    } finally {
        driver.quit();
    }
}

// Replace both methods:
@Override
public List<PlayerDetailDTO> scrapeTeamPlayersByName(String teamName, String league) {
    return executeWithDriver(getBaseUrlByLeague(league), league, (driver, wait) -> 
        scrapeCurrentTeamRoster(driver, wait, teamName, league)
    );
}

@Override
public List<PlayerDetailDTO> scrapeLeaguePlayersByStarterTeam(String starterTeam, String league) {
    return executeWithDriver(getBaseUrlByLeague(league), league, (driver, wait) -> {
        selectTeamFromDropdown(driver, wait, starterTeam);
        List<String> teamNames = getTeamNamesFromDropdown(driver, wait);
        List<String> orderedTeamNames = orderTeamsStartingWith(teamNames, starterTeam);
        
        List<PlayerDetailDTO> allPlayers = new ArrayList<>();
        for (String currentTeamName : orderedTeamNames) {
            selectTeamFromDropdown(driver, wait, currentTeamName);
            allPlayers.addAll(scrapeCurrentTeamRoster(driver, wait, currentTeamName, league));
        }
        return allPlayers;
    });
}
```

### Benefits
- Reduces code by ~20 lines
- Centralized driver lifecycle management
- Consistent error handling
- Reusable for future driver-based operations
- Improved readability

---

## Pattern 4: Nearly Identical League Scraping Loops
**Location:** Lines 1491-1521 (scrapeAllPlayersIfDatabaseEmpty) & 1524-1554 (scrapeAllPlayersForce)

### Current Issues
Both methods contain:
- Identical `leagues` map with same data
- Identical loop over leagues calling `scrapeLeaguePlayersByStarterTeam`
- Identical logging statements
- Only difference: database clearing logic

### Duplication Factor: ~40 lines (nearly identical code)

### Refactoring Solution: Extract Common League Scraping

```java
private static final Map<String, String> LEAGUES_CONFIG = Map.ofEntries(
    Map.entry("Ligue 1", "Angers"),
    Map.entry("LaLiga", "Athletic Club"),
    Map.entry("Premier League", "Arsenal"),
    Map.entry("Bundesliga", "Augsburg"),
    Map.entry("Serie A", "AC Milan")
);

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

### Benefits
- Reduces code by ~30 lines
- Single source of truth for leagues configuration
- Improved maintainability when adding/removing leagues
- DRY principle applied
- Easier to test

---

## Pattern 5: Duplicate Player Persistence Logic
**Location:** Lines 564-575 (persistScrapedPlayer) & 588-615 (processTeamPlayerRow)

### Current Issues
Both methods contain nearly identical logic:
1. Find existing player by name and team
2. If not exists: create new player
3. If exists: update existing player

The `processTeamPlayerRow` method just wraps `persistScrapedPlayer` with additional logic for setting team/league.

### Duplication Factor: ~25 lines of near-identical persistence logic

### Refactoring Solution: Consolidate Player Persistence

```java
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

// Remove persistScrapedPlayer - it's now redundant
// Replace its calls with persistOrUpdatePlayer

// Simplify processTeamPlayerRow:
private void processTeamPlayerRow(WebElement row, String teamName, String league, 
                                 List<PlayerDetailDTO> newPlayers) {
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
        persistOrUpdatePlayer(player, newPlayers);
    } catch (Exception e) {
        // Continue with next player
    }
}
```

### Benefits
- Reduces code by ~15 lines
- Single method for player persistence
- Consistent behavior across all persistence paths
- Easier to audit and test player storage logic
- Reduced duplication in merge/creation logic

---

## Additional Consolidation: Popup Closing
**Location:** Lines 1311-1354 (closePopupIfPresent)

### Minor Issue
The nested try-catch chain for finding accept buttons can be simplified:

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

This reduces the 40+ line nested try-catch to ~15 lines.

---

## Summary of Refactoring Recommendations

| Pattern | Location | Current Lines | Refactored Lines | Savings |
|---------|----------|----------------|-----------------|---------|
| 1. Three extractors | 834-980 | 147 | 60 | 87 lines |
| 2. Player data extraction | 1058-1309 | 218 | 80 | 138 lines |
| 3. Driver setup/teardown | 293-355 | 63 | 35 | 28 lines |
| 4. League scraping loops | 1491-1554 | 64 | 30 | 34 lines |
| 5. Player persistence | 564-615 | 52 | 25 | 27 lines |
| **Bonus**: Popup closing | 1311-1354 | 43 | 15 | 28 lines |
| **TOTAL** | | **587** | **245** | **~342 lines (58% reduction)** |

---

## Implementation Priority

### Phase 1 (High Impact, Low Risk)
1. **Pattern 4** (League scraping loops) - Simple extraction, clear benefits
2. **Pattern 3** (Driver setup/teardown) - Improves all driver-based methods
3. **Pattern 5** (Player persistence) - Consolidates core logic

### Phase 2 (Medium Impact, Medium Complexity)
1. **Pattern 2** (Player data extraction) - Large consolidation, needs configuration objects
2. **Bonus** (Popup closing) - Cleaner exception handling

### Phase 3 (High Impact, Higher Complexity)
1. **Pattern 1** (Three extractors) - Requires record type and lambda mapping

---

## Implementation Notes

- Use Java records for configuration objects (ExtractionConfig, ColumnMapping)
- Use functional interfaces for strategy patterns (DriverTask)
- Maintain backward compatibility - all existing method signatures should remain
- Update tests to verify consolidated methods work identically to originals
- Consider extracting column indices to configuration files for easier maintenance
- Add Javadoc to generic methods explaining configuration expectations

