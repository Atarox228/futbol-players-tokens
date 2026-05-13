package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.PlayerDetailDTO;
import com.desapp.futbolplayerstokens.exception.ScrapingException;
import com.desapp.futbolplayerstokens.modelo.LeagueConstant;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.service.PlayerScraperService;
import com.desapp.futbolplayerstokens.service.PlayerService;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jspecify.annotations.NonNull;
import org.openqa.selenium.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.text.Normalizer;
import java.util.Locale;
import java.net.URL;

@Service
public class PlayerScraperServiceImpl implements PlayerScraperService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerScraperServiceImpl.class);

    private static final String SELENIUM_REMOTE_URL = "http://localhost:4444";

    private static final String CHROME_ARG_NO_SANDBOX = "--no-sandbox";
    private static final String CHROME_ARG_DISABLE_DEV_SHM = "--disable-dev-shm-usage";
    private static final String CHROME_ARG_DISABLE_GPU = "--disable-gpu";
    private static final String CHROME_ARG_WINDOW_SIZE = "--window-size=1920,1080";
    private static final String CHROME_ARG_DISABLE_AUTOMATION = "--disable-blink-features=AutomationControlled";
    private static final String CHROME_ARG_DISABLE_WEB_RESOURCES = "--disable-web-resources";

    private static final String CSS_TBODY_TR = "tbody tr";
    private static final String CSS_PLAYER_LINK_SPAN = "a.player-link span";
    private static final String CSS_PLAYER_META_DATA = "span.player-meta-data";
    private static final String CSS_PLAYER_META_TEAM_NAME = "a.player-meta-data span.team-name";
    private static final String CSS_NESTED_PLAYER_META_DATA = "span > span.player-meta-data";
    private static final String CSS_COOKIE_ACCEPT_ALL = "[data-testid='cookie-accept-all']";
    private static final String CSS_SELECT_TAG = "select";

    private static final String XPATH_HTTP_ERROR = "//*[contains(text(), '502') or contains(text(), 'Bad Gateway') or contains(text(), '503') or contains(text(), 'Service Unavailable')]";
    private static final String XPATH_ACCEPTAR_TODO = "//*[contains(text(), 'Aceptar todo')]";
    private static final String XPATH_ACCEPT_ALL = "//*[contains(text(), 'Accept all')]";
    private static final String XPATH_ACCEPTAR = "//*[contains(text(), 'Aceptar')]";
    private static final String XPATH_ACCEPT = "//*[contains(text(), 'Accept')]";
    private static final String XPATH_ALL_PLAYERS = "//a[@class='option' or contains(@class, 'option')][contains(text(), 'Todos los jugadores')]";
    private static final String XPATH_NEXT_OPTION = "//a[@class='option  clickable'][@id='next']";
    private static final String XPATH_NEXT_LOWER = "//a[contains(text(), 'siguiente')]";
    private static final String XPATH_NEXT_UPPER = "//button[contains(text(), 'Siguiente')]";

    private static final String ID_TOP_PLAYER_STATS_SUMMARY_GRID = "top-player-stats-summary-grid";
    private static final String ID_NEXT = "next";
    private static final String ID_LOCALE_SELECT = "locale-select";
    private static final String TEAM_SQUAD_SUMMARY_SECTION = "team-squad-stats-summary";
    private static final String TEAM_SQUAD_DEFENSIVE_SECTION = "team-squad-stats-defensive";
    private static final String TEAM_SQUAD_OFFENSIVE_SECTION = "team-squad-stats-offensive";

    private static final String ATTR_CLASS = "class";
    private static final String ATTR_DISABLED = "disabled";
    private static final String ATTR_ARIA_DISABLED = "aria-disabled";
    private static final String ATTR_ID = "id";

    private static final String VALUE_TRUE = "true";
    private static final String CLASS_NOT_CURRENT_PLAYER = "not-current-player";
    private static final String CLASS_DISABLED = "disabled";

    private static final String SPACE = " ";
    private static final String REGEX_MULTIPLE_SPACES = "\\s+";
    private static final String REGEX_NON_NUMERIC = "\\D";
    private static final String REGEX_LEADING_COMMA_SPACE = "^,\\s*";
    private static final String EMPTY = "";

    private static final String JS_SCROLL_INTO_VIEW = "arguments[0].scrollIntoView(true);";
    private static final String JS_CLICK_ELEMENT = "arguments[0].click();";

    // Timing constants
    private static final class Timings {
        static final Duration MAIN_PAGE_LOAD = Duration.ofSeconds(15);
        static final Duration TEAM_SELECTION = Duration.ofSeconds(12);
        static final long INITIAL_PAGE_LOAD_MS = 2000;
        static final long POST_CLICK_DELAY_MS = 500;
        static final long POST_PAGINATION_DELAY_MS = 1500;
        static final long POST_POPUP_DELAY_MS = 2000;
        static final long POST_TEAM_SELECT_DELAY_MS = 1200;
        static final long TABLE_LOAD_DELAY_MS = 1000;
    }

    // Column indices for different table layouts
    private enum StatsColumnLayout {
        SUMMARY(4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14),
        DEFENSIVE(4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14),
        OFFENSIVE(4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14),
        LEAGUE_PAGE(2, 3, 4, 5, 6, 7, 11, 12),
        ROSTER(4, 5, 6, 7, 8, 9, 13, 14);

        private final int[] indices;
        StatsColumnLayout(int... indices) { this.indices = indices; }
        int getIndex(int position) { return position < indices.length ? indices[position] : -1; }
    }

    private static final String ERROR_HTTP_DETECTED = "❌ Error HTTP detectado en la página: ";
    private static final String ERROR_TABLE_NOT_LOADED = "❌ La tabla no cargó. Posible error 502 o servidor caído.";
    private static final String ERROR_DURING_SCRAPING = "❌ Error durante el scraping: ";

    private final PlayerRepository playerRepository;
    private final PlayerService playerService;

    public PlayerScraperServiceImpl(PlayerRepository playerRepository, PlayerService playerService) {
        this.playerRepository = playerRepository;
        this.playerService = playerService;
    }

    @Override
    public List<PlayerDetailDTO> scrapeAllPlayers(String url, String league, java.util.function.Consumer<List<PlayerDetailDTO>> onPageComplete) {
        return scrapeAllPlayers(url, league, onPageComplete, true);
    }

    private void clearDatabaseIfRequested(boolean clearTable) {
        if (clearTable) {
            long count = playerRepository.count();
            if (count > 0) {
                playerRepository.deleteAll();
            }
        }
    }

    private void scrapePaginatedPages(WebDriver driver, WebDriverWait wait, String league,
                                      List<PlayerDetailDTO> allPlayers,
                                      java.util.function.Consumer<List<PlayerDetailDTO>> onPageComplete) throws InterruptedException {
        boolean hasNextButton = true;

        while (hasNextButton) {
            List<WebElement> rows = loadCurrentPageRows(wait, driver);
            List<PlayerDetailDTO> playersThisPage = processPageRows(rows, league, allPlayers);

            if (!playersThisPage.isEmpty()) {
                onPageComplete.accept(playersThisPage);
            }

            hasNextButton = isHasNextButton(driver, hasNextButton);
        }
    }

    private List<PlayerDetailDTO> processPageRows(List<WebElement> rows, String league, List<PlayerDetailDTO> allPlayers) {
        List<PlayerDetailDTO> playersThisPage = new ArrayList<>();

        for (WebElement row : rows) {
            PlayerDetailDTO player = processPlayerRow(row, league);
            if (player != null) {
                allPlayers.add(player);
                playersThisPage.add(player);
            }
        }

        return playersThisPage;
    }

    private PlayerDetailDTO processPlayerRow(WebElement row, String league) {
        try {
            if (shouldSkipRow(row)) {
                return null;
            }

            PlayerDetailDTO player = extractPlayerData(row);
            if (player != null && !player.getName().isEmpty()) {
                player.setLeague(league);
                return player;
            }
        } catch (Exception e) {
            // Continuar con el siguiente jugador
        }
        return null;
    }

    private boolean shouldSkipRow(WebElement row) {
        String rowClass = row.getAttribute(ATTR_CLASS);
        if (rowClass != null) {
            String normalizedClass = rowClass.replaceAll(REGEX_MULTIPLE_SPACES, SPACE).trim();
            return normalizedClass.contains(CLASS_NOT_CURRENT_PLAYER);
        }
        return false;
    }

    @Override
    public List<PlayerDetailDTO> scrapeAllPlayers(String url, String league, java.util.function.Consumer<List<PlayerDetailDTO>> onPageComplete, boolean clearTable) {
        clearDatabaseIfRequested(clearTable);

        ChromeOptions options = createChromeOptions();
        WebDriver driver = createDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Timings.MAIN_PAGE_LOAD);
        List<PlayerDetailDTO> allPlayers = new ArrayList<>();

        try {
            prepareLeaguePlayersPage(url, driver, wait);
            scrapePaginatedPages(driver, wait, league, allPlayers, onPageComplete);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScrapingException(ERROR_DURING_SCRAPING + e.getMessage(), e);
        } catch (Exception e) {
            throw new ScrapingException(ERROR_DURING_SCRAPING + e.getMessage(), e);
        } finally {
            driver.quit();
            logger.info("✓ Scraping finalizado. Total jugadores: {}", allPlayers.size());
        }

        return allPlayers;
    }

    private boolean isHasNextButton(WebDriver driver, boolean hasNextButton) {
        try {
            WebElement nextButton = findNextButton(driver);

            if (nextButton != null) {
                // Verificar si está deshabilitado
                String disabledAttr = nextButton.getAttribute(ATTR_DISABLED);
                String ariaDisabled = nextButton.getAttribute(ATTR_ARIA_DISABLED);
                String classAttr = nextButton.getAttribute(ATTR_CLASS);

                boolean isDisabled = disabledAttr != null ||
                                   VALUE_TRUE.equals(ariaDisabled) ||
                                   (classAttr != null && classAttr.contains(CLASS_DISABLED));

                if (!isDisabled && nextButton.isDisplayed()) {
                    // Scroll hasta el botón y hacer click
                    ((JavascriptExecutor) driver).executeScript(JS_SCROLL_INTO_VIEW, nextButton);
                    Thread.sleep(Timings.POST_CLICK_DELAY_MS);

                    nextButton.click();

                    // Esperar a que carguen completamente los nuevos datos
                    Thread.sleep(Timings.POST_PAGINATION_DELAY_MS);
                } else {
                    hasNextButton = false;
                }
            } else {
                hasNextButton = false;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 🔑 preservar interrupción
            hasNextButton = false;
        } catch (Exception e) {
            hasNextButton = false;
        }
        return hasNextButton;
    }

    private void prepareLeaguePlayersPage(String url, WebDriver driver, WebDriverWait wait) throws InterruptedException {
        driver.get(url);

        // Esperar a que cargue la página inicial
        Thread.sleep(Timings.INITIAL_PAGE_LOAD_MS);

        // Detectar y cerrar popup de cookies/consentimiento
        closePopupIfPresent(driver, wait);

        // Seleccionar "Todos los jugadores" en la tabla de ligas
        selectAllPlayersInLeague(driver, wait);

        ensurePlayersTableLoaded(driver, wait);
    }

    private void ensurePlayersTableLoaded(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        try {
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(CSS_TBODY_TR)));
            Thread.sleep(Timings.TABLE_LOAD_DELAY_MS);
        } catch (TimeoutException e) {
            if (isLikelyHttpErrorPage(driver.getTitle(), driver.getPageSource())) {
                throw new ScrapingException(ERROR_HTTP_DETECTED + extractHttpErrorDetails(driver));
            }

            throw new ScrapingException(ERROR_TABLE_NOT_LOADED);
        }
    }

    static boolean isLikelyHttpErrorPage(String pageTitle, String pageSource) {
        String normalizedTitle = pageTitle == null ? EMPTY : pageTitle.toLowerCase(Locale.ROOT);
        String normalizedSource = pageSource == null ? EMPTY : pageSource.toLowerCase(Locale.ROOT);

        return normalizedTitle.contains("bad gateway")
            || normalizedTitle.contains("service unavailable")
            || normalizedTitle.contains("502")
            || normalizedTitle.contains("503")
            || normalizedSource.contains("bad gateway")
            || normalizedSource.contains("service unavailable");
    }

    private String extractHttpErrorDetails(WebDriver driver) {
        String title = driver.getTitle();
        if (title != null && !title.isBlank()) {
            return title.trim();
        }

        try {
            String bodyText = driver.findElement(By.tagName("body")).getText().trim();
            if (!bodyText.isBlank()) {
                return bodyText.lines().findFirst().orElse(bodyText);
            }
        } catch (Exception e) {
            // Ignorar y devolver detalle genérico
        }

        return "sin detalle";
    }

    @Override
    public List<PlayerDetailDTO> scrapeTeamPlayersByName(String teamName, String league) {
        String baseUrl = getBaseUrlByLeague(league);

        ChromeOptions options = createChromeOptions();
        WebDriver driver = createDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Timings.TEAM_SELECTION);
        List<PlayerDetailDTO> newPlayers = new ArrayList<>();

        try {
            driver.get(baseUrl);
            Thread.sleep(Timings.INITIAL_PAGE_LOAD_MS);

            closePopupIfPresent(driver, wait);
            selectTeamFromDropdown(driver, wait, teamName);

            newPlayers.addAll(scrapeCurrentTeamRoster(driver, wait, teamName, league));

            return newPlayers;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScrapingException("❌ Error scrapeando plantilla de " + teamName + ": " + e.getMessage(), e);

        } catch (Exception e) {
            throw new ScrapingException("❌ Error scrapeando plantilla de " + teamName + ": " + e.getMessage(), e);
        } finally {
            driver.quit();
        }
    }

    @Override
    public List<PlayerDetailDTO> scrapeLeaguePlayersByStarterTeam(String starterTeam, String league) {
        String baseUrl = getBaseUrlByLeague(league);

        ChromeOptions options = createChromeOptions();
        WebDriver driver = createDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Timings.TEAM_SELECTION);
        List<PlayerDetailDTO> newPlayers = new ArrayList<>();

        try {
            driver.get(baseUrl);
            Thread.sleep(Timings.INITIAL_PAGE_LOAD_MS);

            closePopupIfPresent(driver, wait);
            selectTeamFromDropdown(driver, wait, starterTeam);

            List<String> teamNames = getTeamNamesFromDropdown(wait);
            List<String> orderedTeamNames = orderTeamsStartingWith(teamNames, starterTeam);

            for (String currentTeamName : orderedTeamNames) {
                selectTeamFromDropdown(driver, wait, currentTeamName);
                newPlayers.addAll(scrapeCurrentTeamRoster(driver, wait, currentTeamName, league));
            }

            return newPlayers;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScrapingException("❌ Error scrapeando liga " + league + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ScrapingException("❌ Error scrapeando liga " + league + ": " + e.getMessage(), e);
        } finally {
            driver.quit();
        }
    }

    private List<String> getTeamNamesFromDropdown(WebDriverWait wait) {
        try {
            List<WebElement> selects = wait.until(d -> d.findElements(By.tagName(CSS_SELECT_TAG)));

            for (WebElement selectElement : selects) {
                String selectId = selectElement.getAttribute(ATTR_ID);
                if (ID_LOCALE_SELECT.equals(selectId)) {
                    continue;
                }

                Select select = new Select(selectElement);
                List<String> teamNames = new ArrayList<>();
                for (WebElement option : select.getOptions()) {
                    String optionText = option.getText() == null ? EMPTY : option.getText().trim();
                    if (!optionText.isBlank()) {
                        teamNames.add(optionText);
                    }
                }

                if (!teamNames.isEmpty()) {
                    return teamNames;
                }
            }
        } catch (Exception e) {
            throw new ScrapingException("No se pudo leer el selector de equipos: " + e.getMessage(), e);
        }

        throw new ScrapingException("No se encontraron equipos en el selector de la página");
    }

    private List<String> orderTeamsStartingWith(List<String> teamNames, String teamName) {
        List<String> orderedTeamNames = new ArrayList<>();
        if (teamNames == null || teamNames.isEmpty()) {
            return orderedTeamNames;
        }

        String target = normalize(teamName);

        for (String currentName : teamNames) {
            if (normalize(currentName).equals(target)) {
                orderedTeamNames.add(currentName);
                break;
            }
        }

        for (String currentName : teamNames) {
            if (orderedTeamNames.stream().noneMatch(existing -> normalize(existing).equals(normalize(currentName)))) {
                orderedTeamNames.add(currentName);
            }
        }

        return orderedTeamNames;
    }

    private List<PlayerDetailDTO> scrapeCurrentTeamRoster(WebDriver driver, WebDriverWait wait, String teamName, String league)
            throws InterruptedException {
        Map<String, PlayerDetailDTO> playersByName = extractSectionPlayers(driver, wait, TEAM_SQUAD_SUMMARY_SECTION, this::extractSummaryPlayerFromRow);
        mergeSectionPlayers(playersByName, extractSectionPlayers(driver, wait, TEAM_SQUAD_DEFENSIVE_SECTION, this::extractDefensivePlayerFromRow));
        mergeSectionPlayers(playersByName, extractSectionPlayers(driver, wait, TEAM_SQUAD_OFFENSIVE_SECTION, this::extractOffensivePlayerFromRow));

        List<PlayerDetailDTO> newPlayers = new ArrayList<>();
        for (PlayerDetailDTO player : playersByName.values()) {
            player.setTeam(teamName);
            player.setLeague(league);
            persistScrapedPlayer(player, newPlayers);
        }

        return newPlayers;
    }

    private Map<String, PlayerDetailDTO> extractSectionPlayers(WebDriver driver,
                                                               WebDriverWait wait,
                                                               String sectionId,
                                                               java.util.function.Function<WebElement, PlayerDetailDTO> rowExtractor)
            throws InterruptedException {
        activateTeamStatsSection(driver, wait, sectionId);

        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id(sectionId)));
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("#" + sectionId + " tbody tr")));
            Thread.sleep(Timings.POST_CLICK_DELAY_MS);
        } catch (TimeoutException e) {
            throw new ScrapingException("No se encontró la sección de plantilla: " + sectionId);
        }

        List<WebElement> rows = driver.findElements(By.cssSelector("#" + sectionId + " tbody tr"));
        Map<String, PlayerDetailDTO> playersByName = new LinkedHashMap<>();

        for (WebElement row : rows) {
            try {
                if (!shouldSkipRow(row)) {
                    PlayerDetailDTO player = rowExtractor.apply(row);
                    if (player != null && player.getName() != null && !player.getName().isBlank()) {
                        playersByName.put(normalize(player.getName()), player);
                    }
                }
            } catch (Exception e) {
                // Continuar con el siguiente jugador
            }
        }

        return playersByName;
    }

    private void mergeSectionPlayers(Map<String, PlayerDetailDTO> basePlayers, Map<String, PlayerDetailDTO> extraPlayers) {
        for (Map.Entry<String, PlayerDetailDTO> entry : extraPlayers.entrySet()) {
            PlayerDetailDTO basePlayer = basePlayers.get(entry.getKey());
            if (basePlayer == null) {
                basePlayers.put(entry.getKey(), entry.getValue());
                continue;
            }

            mergePlayerStats(basePlayer, entry.getValue());
        }
    }

    private void mergePlayerStats(PlayerDetailDTO target, PlayerDetailDTO source) {
        if (source == null) {
            return;
        }

        if (source.getPosition() != null && !source.getPosition().isBlank()) {
            target.setPosition(source.getPosition());
        }
        if (source.getRating() != null) {
            target.setRating(source.getRating());
        }

        mergeAppearanceStats(target, source);
        mergeOffensiveStats(target, source);
        mergeDefensiveStats(target, source);
        mergeCardStats(target, source);
        mergeOtherStats(target, source);
    }

    private void mergeAppearanceStats(PlayerDetailDTO target, PlayerDetailDTO source) {
        if (source.getAppearances() != null) {
            target.setAppearances(source.getAppearances());
        }
        if (source.getMinutes() != null) {
            target.setMinutes(source.getMinutes());
        }
    }

    private void mergeOffensiveStats(PlayerDetailDTO target, PlayerDetailDTO source) {
        if (source.getGoals() != null) {
            target.setGoals(source.getGoals());
        }
        if (source.getAssists() != null) {
            target.setAssists(source.getAssists());
        }
        if (source.getShotsOnTarget() != null) {
            target.setShotsOnTarget(source.getShotsOnTarget());
        }
        if (source.getKeyPasses() != null) {
            target.setKeyPasses(source.getKeyPasses());
        }
        if (source.getDribbles() != null) {
            target.setDribbles(source.getDribbles());
        }
    }

    private void mergeDefensiveStats(PlayerDetailDTO target, PlayerDetailDTO source) {
        if (source.getTackles() != null) {
            target.setTackles(source.getTackles());
        }
        if (source.getInterceptions() != null) {
            target.setInterceptions(source.getInterceptions());
        }
        if (source.getBlocks() != null) {
            target.setBlocks(source.getBlocks());
        }
        if (source.getClears() != null) {
            target.setClears(source.getClears());
        }
        if (source.getDribbled() != null) {
            target.setDribbled(source.getDribbled());
        }
        if (source.getFaults() != null) {
            target.setFaults(source.getFaults());
        }
        if (source.getOffsidesGiven() != null) {
            target.setOffsidesGiven(source.getOffsidesGiven());
        }
    }

    private void mergeCardStats(PlayerDetailDTO target, PlayerDetailDTO source) {
        if (source.getYellowCards() != null) {
            target.setYellowCards(source.getYellowCards());
        }
        if (source.getRedCards() != null) {
            target.setRedCards(source.getRedCards());
        }
    }

    private void mergeOtherStats(PlayerDetailDTO target, PlayerDetailDTO source) {
        if (source.getAerialWon() != null) {
            target.setAerialWon(source.getAerialWon());
        }
        if (source.getPlayerOfTheMatch() != null) {
            target.setPlayerOfTheMatch(source.getPlayerOfTheMatch());
        }
        if (source.getOwnGoals() != null) {
            target.setOwnGoals(source.getOwnGoals());
        }
        if (source.getFaulted() != null) {
            target.setFaulted(source.getFaulted());
        }
        if (source.getOffsides() != null) {
            target.setOffsides(source.getOffsides());
        }
        if (source.getDispossesed() != null) {
            target.setDispossesed(source.getDispossesed());
        }
        if (source.getTurnover() != null) {
            target.setTurnover(source.getTurnover());
        }
        if (source.getPassAccuracy() != null) {
            target.setPassAccuracy(source.getPassAccuracy());
        }
    }

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

    private void activateTeamStatsSection(WebDriver driver, WebDriverWait wait, String sectionId) throws InterruptedException {
        String sectionHref = "a[href='#" + sectionId + "']";
        try {
            WebElement sectionLink = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(sectionHref)));
            ((JavascriptExecutor) driver).executeScript(JS_CLICK_ELEMENT, sectionLink);
            Thread.sleep(Timings.POST_CLICK_DELAY_MS);
        } catch (TimeoutException e) {
            // Si la pestaña ya está activa o no necesita click, continuar
        }
    }

    static void modificandoPlayer(PlayerDetailDTO player, List<Player> existingPlayers, PlayerRepository playerRepository) {
        for (Player existingPlayer : existingPlayers) {
            existingPlayer.setRating(player.getRating());
            existingPlayer.setAppearances(player.getAppearances());
            existingPlayer.setMinutes(player.getMinutes());
            existingPlayer.setGoals(player.getGoals());
            existingPlayer.setAssists(player.getAssists());
            existingPlayer.setShotsOnTarget(player.getShotsOnTarget());
            // passPrecision removed; passAccuracy is used instead
            existingPlayer.setAerialWon(player.getAerialWon());
            existingPlayer.setFaults(player.getFaults());
            existingPlayer.setOffsidesGiven(player.getOffsidesGiven());
            existingPlayer.setClears(player.getClears());
            existingPlayer.setDribbled(player.getDribbled());
            existingPlayer.setTackles(player.getTackles());
            existingPlayer.setInterceptions(player.getInterceptions());
            existingPlayer.setBlocks(player.getBlocks());
            existingPlayer.setOwnGoals(player.getOwnGoals());
            existingPlayer.setKeyPasses(player.getKeyPasses());
            existingPlayer.setDribbles(player.getDribbles());
            existingPlayer.setFaulted(player.getFaulted());
            existingPlayer.setOffsides(player.getOffsides());
            existingPlayer.setDispossesed(player.getDispossesed());
            existingPlayer.setTurnover(player.getTurnover());
            existingPlayer.setPassAccuracy(player.getPassAccuracy());
            existingPlayer.setYellowCards(player.getYellowCards());
            existingPlayer.setRedCards(player.getRedCards());
            existingPlayer.setPlayerOfTheMatch(player.getPlayerOfTheMatch());
            existingPlayer.setLastModifiedAt(LocalDateTime.now());
        }
        playerRepository.saveAll(existingPlayers);
    }

    static void playerDesdeCero(PlayerDetailDTO player, PlayerRepository playerRepository) {
        Player newPlayer = Player.builder()
            .name(player.getName())
            .rating(player.getRating())
            .team(player.getTeam())
            .league(player.getLeague())
            .position(player.getPosition())
            .appearances(player.getAppearances())
            .minutes(player.getMinutes())
            .goals(player.getGoals())
            .assists(player.getAssists())
            .shotsOnTarget(player.getShotsOnTarget())
            .passAccuracy(player.getPassAccuracy())
            .aerialWon(player.getAerialWon())
            .faults(player.getFaults())
            .offsidesGiven(player.getOffsidesGiven())
            .clears(player.getClears())
            .dribbled(player.getDribbled())
            .tackles(player.getTackles())
            .interceptions(player.getInterceptions())
            .blocks(player.getBlocks())
            .ownGoals(player.getOwnGoals())
            .keyPasses(player.getKeyPasses())
            .dribbles(player.getDribbles())
            .faulted(player.getFaulted())
            .offsides(player.getOffsides())
            .dispossesed(player.getDispossesed())
            .turnover(player.getTurnover())
            .passAccuracy(player.getPassAccuracy())
            .yellowCards(player.getYellowCards())
            .redCards(player.getRedCards())
            .playerOfTheMatch(player.getPlayerOfTheMatch())
            .build();

        playerRepository.save(newPlayer);
    }

    @Override
    public List<PlayerDetailDTO> scrapeNewPlayersOnly(String url, String league, java.util.function.Consumer<List<PlayerDetailDTO>> onPageComplete) {
        ChromeOptions options = createChromeOptions();
        WebDriver driver = createDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Timings.MAIN_PAGE_LOAD);

        List<PlayerDetailDTO> allNewPlayers = new ArrayList<>();

        try {
            prepareLeaguePlayersPage(url, driver, wait);

            boolean hasNextButton = true;

            while (hasNextButton) {
                List<WebElement> rows = loadCurrentPageRows(wait, driver);
                List<PlayerDetailDTO> newPlayersThisPage = processNewPlayerRows(rows, league, allNewPlayers);

                if (!newPlayersThisPage.isEmpty()) {
                    onPageComplete.accept(newPlayersThisPage);
                }

                hasNextButton = isHasNextButton(driver, hasNextButton);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScrapingException(ERROR_DURING_SCRAPING + e.getMessage(), e);

        } catch (Exception e) {
            throw new ScrapingException(ERROR_DURING_SCRAPING + e.getMessage(), e);
        } finally {
            driver.quit();
        }

        return allNewPlayers;
    }

    private List<PlayerDetailDTO> processNewPlayerRows(List<WebElement> rows, String league, List<PlayerDetailDTO> allNewPlayers) {
        List<PlayerDetailDTO> newPlayersThisPage = new ArrayList<>();

        for (WebElement row : rows) {
            try {
                if (shouldSkipRow(row)) {
                    continue;
                }

                PlayerDetailDTO player = extractPlayerData(row);
                if (player == null || player.getName().isEmpty()) {
                    continue;
                }

                player.setLeague(league);

                if (!playerRepository.findByNameIgnoreCaseAndTeamIgnoreCase(
                        player.getName().trim(),
                        player.getTeam().trim())
                        .isEmpty()) {
                    continue;
                }

                allNewPlayers.add(player);
                newPlayersThisPage.add(player);
            } catch (Exception e) {
                // Continuar con el siguiente jugador
            }
        }

        return newPlayersThisPage;
    }

    private @NonNull List<WebElement> loadCurrentPageRows(WebDriverWait wait, WebDriver driver) throws InterruptedException {
        try {
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector(CSS_TBODY_TR)));
        } catch (TimeoutException e) {
            throw new ScrapingException(ERROR_TABLE_NOT_LOADED);
        }

        // Pequeño delay adicional para asegurar que los datos se renderizaron
        Thread.sleep(Timings.TABLE_LOAD_DELAY_MS);

        // Extraer jugadores de la página actual
        List<WebElement> rows = driver.findElements(By.cssSelector(CSS_TBODY_TR));
        return rows;
    }

    private String getBaseUrlByLeague(String league) {
        return switch(league) {
            case LeagueConstant.LALIGA -> "https://es.whoscored.com/teams/53/show/espa%C3%B1a-athletic-club";
            case LeagueConstant.PREMIER_LEAGUE -> "https://es.whoscored.com/teams/13/show/inglaterra-arsenal";
            case LeagueConstant.LIGUE_1 -> "https://es.whoscored.com/teams/614/show/francia-angers";
            case LeagueConstant.BUNDESLIGA -> "https://es.whoscored.com/teams/1730/show/alemania-augsburg";
            case LeagueConstant.SERIE_A -> "https://es.whoscored.com/teams/80/show/italia-ac-milan";
            default -> "https://es.whoscored.com/teams/65/show/espa%C3%B1a-barcelona";
        };
    }

    private int parseAppearances(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        text = text.trim();

        // Detectar formato "15(11)" - suma de dos números
        if (text.contains("(") && text.contains(")")) {
            try {
                String[] parts = text.split("[()]");
                if (parts.length >= 2) {
                    int first = Integer.parseInt(parts[0].replaceAll(REGEX_NON_NUMERIC, EMPTY));
                    int second = Integer.parseInt(parts[1].replaceAll(REGEX_NON_NUMERIC, EMPTY));
                    return first + second;
                }
            } catch (NumberFormatException e) {
                // Fallback a extracción simple
            }
        }

        // Extracción simple de números
        String numericOnly = text.replaceAll(REGEX_NON_NUMERIC, EMPTY);
        try {
            return !numericOnly.isEmpty() ? Integer.parseInt(numericOnly) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private List<WebElement> findSquadRowsFromTable(WebDriver driver, WebDriverWait wait) {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id(ID_TOP_PLAYER_STATS_SUMMARY_GRID)));
            Thread.sleep(Timings.POST_CLICK_DELAY_MS);
            WebElement table = driver.findElement(By.id(ID_TOP_PLAYER_STATS_SUMMARY_GRID));
            List<WebElement> rows = table.findElements(By.cssSelector(CSS_TBODY_TR));
            if (rows.isEmpty()) {
                throw new ScrapingException("La tabla de plantilla no contiene filas");
            }
            return rows;
        } catch (TimeoutException e) {
            throw new ScrapingException("No se encontró la tabla de plantilla (top-player-stats-summary-grid) en la página");
        } catch (NoSuchElementException e) {
            throw new ScrapingException("No se encontró la tabla de plantilla (top-player-stats-summary-grid) en la página");
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ScrapingException("Interrumpido al esperar la tabla de plantilla", ie);
        }
    }

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
            if (cells.size() > 6) {
                player.setGoals(parseIntegerStat(cells.get(6).getText()));
            }
            if (cells.size() > 7) {
                player.setAssists(parseIntegerStat(cells.get(7).getText()));
            }
            if (cells.size() > 8) {
                player.setYellowCards(parseIntegerStat(cells.get(8).getText()));
            }
            if (cells.size() > 9) {
                player.setRedCards(parseIntegerStat(cells.get(9).getText()));
            }
            if (cells.size() > 10) {
                player.setShotsOnTarget(parseDecimalStat(cells.get(10).getText()));
            }
            if (cells.size() > 11) {
                Double passAcc = parseDecimalStat(cells.get(11).getText());
                player.setPassAccuracy(passAcc);
            }
            if (cells.size() > 12) {
                player.setAerialWon(parseDecimalStat(cells.get(12).getText()));
            }
            if (cells.size() > 13) {
                player.setPlayerOfTheMatch(parseIntegerStat(cells.get(13).getText()));
            }
            if (cells.size() > 14) {
                player.setRating(parseDecimalStat(cells.get(14).getText()));
            }

            return player;
        } catch (Exception e) {
            return null;
        }
    }

    private PlayerDetailDTO extractDefensivePlayerFromRow(WebElement row) {
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
            if (cells.size() > 6) {
                player.setTackles(parseDecimalStat(cells.get(6).getText()));
            }
            if (cells.size() > 7) {
                player.setInterceptions(parseDecimalStat(cells.get(7).getText()));
            }
            if (cells.size() > 8) {
                player.setFaults(parseDecimalStat(cells.get(8).getText()));
            }
            if (cells.size() > 9) {
                player.setOffsidesGiven(parseDecimalStat(cells.get(9).getText()));
            }
            if (cells.size() > 10) {
                player.setClears(parseDecimalStat(cells.get(10).getText()));
            }
            if (cells.size() > 11) {
                player.setDribbled(parseDecimalStat(cells.get(11).getText()));
            }
            if (cells.size() > 12) {
                player.setBlocks(parseDecimalStat(cells.get(12).getText()));
            }
            if (cells.size() > 13) {
                player.setOwnGoals(parseIntegerStat(cells.get(13).getText()));
            }
            if (cells.size() > 14) {
                player.setRating(parseDecimalStat(cells.get(14).getText()));
            }

            return player;
        } catch (Exception e) {
            return null;
        }
    }

    private PlayerDetailDTO extractOffensivePlayerFromRow(WebElement row) {
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
            if (cells.size() > 6) {
                player.setGoals(parseIntegerStat(cells.get(6).getText()));
            }
            if (cells.size() > 7) {
                player.setAssists(parseIntegerStat(cells.get(7).getText()));
            }
            if (cells.size() > 8) {
                player.setShotsOnTarget(parseDecimalStat(cells.get(8).getText()));
            }
            if (cells.size() > 9) {
                player.setKeyPasses(parseDecimalStat(cells.get(9).getText()));
            }
            if (cells.size() > 10) {
                player.setDribbles(parseDecimalStat(cells.get(10).getText()));
            }
            if (cells.size() > 11) {
                player.setFaulted(parseDecimalStat(cells.get(11).getText()));
            }
            if (cells.size() > 12) {
                player.setOffsides(parseDecimalStat(cells.get(12).getText()));
            }
            if (cells.size() > 13) {
                player.setDispossesed(parseDecimalStat(cells.get(13).getText()));
            }
            if (cells.size() > 14) {
                player.setTurnover(parseDecimalStat(cells.get(14).getText()));
            }
            if (cells.size() > 15) {
                player.setRating(parseDecimalStat(cells.get(15).getText()));
            }

            return player;
        } catch (Exception e) {
            return null;
        }
    }

    private PlayerDetailDTO createPlayerFromRow(WebElement row) {
        try {
            WebElement playerLink = row.findElement(By.cssSelector(CSS_PLAYER_LINK_SPAN));
            String name = playerLink.getText().trim();

            if (name.isEmpty() || name.length() < 2) {
                return null;
            }

            PlayerDetailDTO player = PlayerDetailDTO.builder().build();
            player.setName(name);
            extractPlayerPosition(row, player);

            return player;
        } catch (Exception e) {
            return null;
        }
    }

    private void extractPlayerPosition(WebElement row, PlayerDetailDTO player) {
        try {
            List<WebElement> positionSpans = row.findElements(By.cssSelector(CSS_NESTED_PLAYER_META_DATA));
            if (positionSpans.size() >= 2) {
                String position = positionSpans.get(1).getText().trim().replaceAll(REGEX_LEADING_COMMA_SPACE, EMPTY);
                player.setPosition(position);
            }
        } catch (NoSuchElementException e) {
            player.setPosition(EMPTY);
        }
    }

    static Integer parseIntegerStat(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        String trimmed = text.trim();
        if (trimmed.contains("(") && trimmed.contains(")")) {
            try {
                String[] parts = trimmed.split("[()]");
                if (parts.length >= 2) {
                    int first = Integer.parseInt(parts[0].replaceAll(REGEX_NON_NUMERIC, EMPTY));
                    int second = Integer.parseInt(parts[1].replaceAll(REGEX_NON_NUMERIC, EMPTY));
                    return first + second;
                }
            } catch (NumberFormatException e) {
                // Fallback a extracción simple
            }
        }

        String numericOnly = trimmed.replaceAll(REGEX_NON_NUMERIC, EMPTY);
        if (numericOnly.isBlank()) {
            return 0;
        }

        try {
            return Integer.parseInt(numericOnly);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static Double parseDecimalStat(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }

        String normalized = text.trim().replace("%", EMPTY).replace(",", ".").replaceAll("[^0-9.\\-]", EMPTY);
        if (normalized.isBlank() || ".".equals(normalized) || "-".equals(normalized)) {
            return 0.0;
        }

        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private PlayerDetailDTO extractPlayerDataFromRoster(WebElement row) {
        try {
            // Buscar el nombre dentro del span dentro del a.player-link
            WebElement playerLink = row.findElement(By.cssSelector(CSS_PLAYER_LINK_SPAN));
            String name = playerLink.getText().trim();

            if (name.isEmpty() || name.length() < 2) {
                return null;
            }

            PlayerDetailDTO player = PlayerDetailDTO.builder().build();
            player.setName(name);

            // Extraer la posición del span player-meta-data
            try {
                List<WebElement> metaDataSpans = row.findElements(By.cssSelector(CSS_PLAYER_META_DATA));
                if (metaDataSpans.size() >= 2) {
                    // El segundo span contiene la posición (ej: ",  ME(C)  ")
                    String position = metaDataSpans.get(1).getText().trim().replaceAll(REGEX_LEADING_COMMA_SPACE, EMPTY);
                    player.setPosition(position);
                }
            } catch (Exception e) {
                // Si no se puede extraer la posición, continuar sin ella
            }

            // Obtener datos de las columnas - Según los headers de la tabla
            List<WebElement> cells = row.findElements(By.tagName("td"));

            // Índice 4: Jgdos (Partidos Jugados)
            if (cells.size() > 4) {
                player.setAppearances(parseAppearances(cells.get(4).getText()));
            }

            // Índice 5: Mins (Minutos)
            if (cells.size() > 5) {
                try {
                    String text = cells.get(5).getText().trim().replaceAll(REGEX_NON_NUMERIC, EMPTY);
                    player.setMinutes(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setMinutes(0);
                }
            }

            // Índice 6: Goles
            if (cells.size() > 6) {
                try {
                    String text = cells.get(6).getText().trim().replaceAll(REGEX_NON_NUMERIC, EMPTY);
                    player.setGoals(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setGoals(0);
                }
            }

            // Índice 7: Asist (Asistencias)
            if (cells.size() > 7) {
                try {
                    String text = cells.get(7).getText().trim().replaceAll(REGEX_NON_NUMERIC, EMPTY);
                    player.setAssists(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setAssists(0);
                }
            }

            // Índice 8: Amar (Tarjetas Amarillas)
            if (cells.size() > 8) {
                try {
                    String text = cells.get(8).getText().trim().replaceAll(REGEX_NON_NUMERIC, EMPTY);
                    player.setYellowCards(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setYellowCards(0);
                }
            }

            // Índice 9: Roja (Tarjetas Rojas)
            if (cells.size() > 9) {
                try {
                    String text = cells.get(9).getText().trim().replaceAll(REGEX_NON_NUMERIC, EMPTY);
                    player.setRedCards(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setRedCards(0);
                }
            }

            // Índice 13: JdelP (Jugador del Partido)
            if (cells.size() > 13) {
                try {
                    String text = cells.get(13).getText().trim().replaceAll(REGEX_NON_NUMERIC, EMPTY);
                    player.setPlayerOfTheMatch(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setPlayerOfTheMatch(0);
                }
            }

            // Índice 14: Rating
            if (cells.size() > 14) {
                String ratingText = cells.get(14).getText().trim();
                try {
                    Double rating = Double.parseDouble(ratingText);
                    player.setRating(rating);
                } catch (NumberFormatException e) {
                    player.setRating(0.0);
                }
            }

            return player;
        } catch (Exception e) {
            return null;
        }
    }


    private List<WebElement> findSquadRows(WebDriver driver, WebDriverWait wait) {
        String squadRowsXPath = "(//*[self::h1 or self::h2 or self::h3 or self::h4 or self::span or self::div]" +
            "[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÚÜ', 'abcdefghijklmnopqrstuvwxyzáéíóúü'), 'plantilla')" +
            " or contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'squad')]" +
            "/following::table[1]//tbody/tr)";

        try {
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath(squadRowsXPath)));
            Thread.sleep(1000);
            List<WebElement> rows = driver.findElements(By.xpath(squadRowsXPath));
            if (rows.isEmpty()) {
                throw new ScrapingException("La tabla de plantilla no contiene filas");
            }
            return rows;
        } catch (TimeoutException e) {
            throw new ScrapingException("No se encontró la tabla de plantilla (Plantilla/Squad) en la página");
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ScrapingException("Interrumpido al esperar la tabla de plantilla", ie);
        }
    }

    private PlayerDetailDTO extractPlayerData(WebElement row) {
        try {
            // Buscar el nombre dentro del span dentro del a.player-link
            WebElement playerLink = row.findElement(By.cssSelector(CSS_PLAYER_LINK_SPAN));
            String name = playerLink.getText().trim();

            if (name.isEmpty() || name.length() < 2) {
                return null;
            }

            PlayerDetailDTO player = PlayerDetailDTO.builder().build();
            player.setName(name);

            // Obtener equipo desde a.player-meta-data span.team-name
            try {
                WebElement teamElement = row.findElement(By.cssSelector(CSS_PLAYER_META_TEAM_NAME));
                player.setTeam(teamElement.getText().trim().replaceAll(",\\s*$", EMPTY));
            } catch (NoSuchElementException e) {
                player.setTeam(EMPTY);
            }

            // Obtener posición desde span.player-meta-data (el segundo dentro del span que sigue al <a>)
            try {
                List<WebElement> positionSpans = row.findElements(By.cssSelector(CSS_NESTED_PLAYER_META_DATA));
                if (positionSpans.size() >= 2) {
                    // El segundo span contiene las posiciones (ej: ",  MP(CID),DL  ")
                    String position = positionSpans.get(1).getText().trim().replaceAll(REGEX_LEADING_COMMA_SPACE, EMPTY);
                    player.setPosition(position);
                } else {
                    player.setPosition(EMPTY);
                }
            } catch (NoSuchElementException e) {
                player.setPosition(EMPTY);
            }

            // Obtener datos de las columnas
            List<WebElement> cells = row.findElements(By.tagName("td"));

            // Columna 2: Partidos Jugados (Jgdos)
            if (cells.size() > 2) {
                player.setAppearances(parseAppearances(cells.get(2).getText()));
            }

            // Columna 3: Minutos (Mins)
            if (cells.size() > 3) {
                try {
                    String text = cells.get(3).getText().trim().replaceAll(REGEX_NON_NUMERIC, EMPTY);
                    player.setMinutes(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setMinutes(0);
                }
            }

            // Columna 4: Goles (Goles)
            if (cells.size() > 4) {
                try {
                    String text = cells.get(4).getText().trim().replaceAll(REGEX_NON_NUMERIC, EMPTY);
                    player.setGoals(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setGoals(0);
                }
            }

            // Columna 5: Asistencias (Asist)
            if (cells.size() > 5) {
                try {
                    String text = cells.get(5).getText().trim().replaceAll(REGEX_NON_NUMERIC, EMPTY);
                    player.setAssists(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setAssists(0);
                }
            }

            // Columna 6: Tarjetas Amarillas (Amar)
            if (cells.size() > 6) {
                try {
                    String text = cells.get(6).getText().trim().replaceAll(REGEX_NON_NUMERIC, EMPTY);
                    player.setYellowCards(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setYellowCards(0);
                }
            }

            // Columna 7: Tarjetas Rojas (Roja)
            if (cells.size() > 7) {
                try {
                    String text = cells.get(7).getText().trim().replaceAll(REGEX_NON_NUMERIC, EMPTY);
                    player.setRedCards(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setRedCards(0);
                }
            }

            // Columna 11: Jugador del Partido (JdelP)
            if (cells.size() > 11) {
                try {
                    String text = cells.get(11).getText().trim().replaceAll(REGEX_NON_NUMERIC, EMPTY);
                    player.setPlayerOfTheMatch(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setPlayerOfTheMatch(0);
                }
            }

            // Columna 12: Rating
            if (cells.size() > 12) {
                String ratingText = cells.get(12).getText().trim();
                try {
                    Double rating = Double.parseDouble(ratingText);
                    player.setRating(rating);
                } catch (NumberFormatException e) {
                    player.setRating(0.0);
                }
            }

            return player;
        } catch (Exception e) {
            return null;
        }
    }

    private void closePopupIfPresent(WebDriver driver, WebDriverWait wait) {
        try {
            // Intenta encontrar y hacer click en botones comunes de aceptación
            try {
                // Buscar botón "Aceptar todo", "Accept all", "Aceptar", etc.
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
                    Thread.sleep(Timings.POST_POPUP_DELAY_MS);
                }
            } catch (TimeoutException e) {
                // No hay popup, continuar
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

        } catch (Exception e) {
            // Ignorar errores con popup
        }
    }

    private void selectAllPlayersInLeague(WebDriver driver, WebDriverWait wait) {
        try {
            try {
                // Esperar y hacer click en el botón de todos los jugadores
                WebElement allPlayersButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath(XPATH_ALL_PLAYERS)
                ));

                ((JavascriptExecutor) driver).executeScript(JS_CLICK_ELEMENT, allPlayersButton);
            } catch (TimeoutException e) {
                // Botón no encontrado, continuar
            }

            // Esperar a que se carguen los datos
            Thread.sleep(2000);
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(CSS_TBODY_TR)));

        } catch (TimeoutException e) {
            // Timeout, continuar
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // Interrumpida, continuar
        } catch (Exception e) {
            // Error, continuar
        }
    }

    private void selectTeamFromDropdown(WebDriver driver, WebDriverWait wait, String teamName) {
        String target = normalize(teamName);

        try {
            // Buscar todos los selects EXCEPTO el locale-select
            List<WebElement> selects = wait.until(d -> d.findElements(By.tagName(CSS_SELECT_TAG)));

            for (WebElement selectElement : selects) {
                try {
                    // Saltar el select de idioma
                    String selectId = selectElement.getAttribute(ATTR_ID);
                    if (ID_LOCALE_SELECT.equals(selectId)) {
                        continue;
                    }

                    Select select = new Select(selectElement);
                    List<WebElement> options = select.getOptions();
                    for (WebElement option : options) {
                        String optionText = option.getText().trim();
                        String normalizedOption = normalize(optionText);
                        if (normalizedOption.equals(target) || normalizedOption.contains(target) || target.contains(normalizedOption)) {
                            String previousUrl = driver.getCurrentUrl();
                            select.selectByVisibleText(optionText);

                            // Esperar cambio de URL o recarga de datos tras cambiar equipo.
                            wait.until(d -> !d.getCurrentUrl().equals(previousUrl) || d.findElements(By.cssSelector(CSS_TBODY_TR)).size() > 0);
                            Thread.sleep(1200);
                            return;
                        }
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ScrapingException("Interrumpido al seleccionar equipo", ie);
                } catch (Exception e) {
                    // Probar siguiente select
                }
            }
        } catch (Exception e) {
            throw new ScrapingException("Error al intentar seleccionar equipo: " + e.getMessage(), e);
        }

        throw new ScrapingException("No se encontró el equipo '" + teamName + "' en el selector de la página");
    }

    private String normalize(String text) {
        String normalized = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .trim();
        return normalized;
    }

    private ChromeOptions createChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(CHROME_ARG_NO_SANDBOX);
        options.addArguments(CHROME_ARG_DISABLE_DEV_SHM);
        options.addArguments(CHROME_ARG_DISABLE_GPU);
        options.addArguments(CHROME_ARG_WINDOW_SIZE);
        options.addArguments(CHROME_ARG_DISABLE_AUTOMATION);
        options.addArguments(CHROME_ARG_DISABLE_WEB_RESOURCES);
        return options;
    }

    private WebDriver createDriver(ChromeOptions options) {
        try {
            // Try to connect to remote Selenium server (for Docker)
            return new RemoteWebDriver(new URL(SELENIUM_REMOTE_URL), options);
        } catch (Exception e) {
            // Fallback to local ChromeDriver
            WebDriverManager.chromedriver().setup();
            return new ChromeDriver(options);
        }
    }

    private WebElement findNextButton(WebDriver driver) throws NoSuchElementException {
        // Intenta encontrar el botón "siguiente" de varias formas
        WebElement nextButton = null;

        // Primero intenta por ID (la forma más específica)
        try {
            nextButton = driver.findElement(By.id(ID_NEXT));
            return nextButton;
        } catch (NoSuchElementException e1) {
        }

        // Intenta por clase y atributo
        try {
            nextButton = driver.findElement(By.xpath(XPATH_NEXT_OPTION));
            return nextButton;
        } catch (NoSuchElementException e2) {
        }

        try {
            nextButton = driver.findElement(By.xpath(XPATH_NEXT_LOWER));
            return nextButton;
        } catch (NoSuchElementException e3) {
        }

        try {
            nextButton = driver.findElement(By.xpath(XPATH_NEXT_UPPER));
            return nextButton;
        } catch (NoSuchElementException e4) {
        }

        throw new NoSuchElementException("No se encontró botón 'Siguiente' con ningún selector");
    }

    @Override
    public void scrapeAllPlayersIfDatabaseEmpty() {
        long playerCount = playerRepository.count();

        if (playerCount > 0) {
            logger.info("⏭️ BD no está vacía. Saltando scraping automático. Jugadores en BD: {}", playerCount);
            return;
        }

        logger.info("🚀 BD vacía detectada. Iniciando scraping automático de todos los jugadores...");

        Map<String, String> ligas = new LinkedHashMap<>();
        ligas.put(LeagueConstant.LIGUE_1, "Angers");
        ligas.put(LeagueConstant.LALIGA, "Athletic Club");
        ligas.put(LeagueConstant.PREMIER_LEAGUE, "Arsenal");
        ligas.put(LeagueConstant.BUNDESLIGA, "Augsburg");
        ligas.put(LeagueConstant.SERIE_A, "AC Milan");

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

        Map<String, String> ligas = new LinkedHashMap<>();
        ligas.put(LeagueConstant.LIGUE_1, "Angers");
        ligas.put(LeagueConstant.LALIGA, "Athletic Club");
        ligas.put(LeagueConstant.PREMIER_LEAGUE, "Arsenal");
        ligas.put(LeagueConstant.BUNDESLIGA, "Augsburg");
        ligas.put(LeagueConstant.SERIE_A, "AC Milan");

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
}
