package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.PlayerDetailDTO;
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
import org.openqa.selenium.TimeoutException;
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
    private static final String EMPTY = "";

    private static final String JS_SCROLL_INTO_VIEW = "arguments[0].scrollIntoView(true);";
    private static final String JS_CLICK_ELEMENT = "arguments[0].click();";

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

    @Override
    public List<PlayerDetailDTO> scrapeAllPlayers(String url, String league, java.util.function.Consumer<List<PlayerDetailDTO>> onPageComplete, boolean clearTable) {
        if (clearTable) {
            long count = playerRepository.count();
            if (count > 0) {
                playerRepository.deleteAll();
            }
        }

        ChromeOptions options = createChromeOptions();
        WebDriver driver = createDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        List<PlayerDetailDTO> allPlayers = new ArrayList<>();

        try {
            prepareLeaguePlayersPage(url, driver, wait);

            boolean hasNextButton = true;
            int pageCount = 0;

            while (hasNextButton) {
                pageCount++;

                // Esperar a que cargue la tabla con timeout corto
                List<WebElement> rows = loadCurrentPageRows(wait, driver);
                int validPlayersInPage = 0;

                for (WebElement row : rows) {
                    try {
                        // Omitir jugadores que ya no pertenecen a la liga
                        String rowClass = row.getAttribute(ATTR_CLASS);
                        if (rowClass != null) {
                            // Normalizar espacios y verificar si contiene not-current-player
                            String normalizedClass = rowClass.replaceAll(REGEX_MULTIPLE_SPACES, SPACE).trim();
                            if (normalizedClass.contains(CLASS_NOT_CURRENT_PLAYER)) {
                                continue;
                            }
                        }

                        PlayerDetailDTO player = extractPlayerData(row);
                        if (player != null && !player.getName().isEmpty()) {
                            player.setLeague(league);
                            allPlayers.add(player);
                            validPlayersInPage++;
                        }
                    } catch (Exception e) {
                        // Continuar con el siguiente jugador
                    }
                }

                // Persistir los jugadores de esta página
                if (validPlayersInPage > 0) {
                    List<PlayerDetailDTO> playersThisPage = allPlayers.subList(
                        Math.max(0, allPlayers.size() - validPlayersInPage),
                        allPlayers.size()
                    );
                    onPageComplete.accept(new ArrayList<>(playersThisPage));
                }

                // Buscar y hacer click en el botón "Siguiente"
                hasNextButton = isHasNextButton(driver, hasNextButton);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 🔑 RESTAURA la interrupción
            throw new RuntimeException(ERROR_DURING_SCRAPING + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException(ERROR_DURING_SCRAPING + e.getMessage(), e);
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
                    Thread.sleep(500);

                    nextButton.click();

                    // Esperar a que carguen completamente los nuevos datos
                    Thread.sleep(1500);
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

        // Esperar a que cargue la página inicial (más tiempo en headless)
        Thread.sleep(4000);

        // Verificar si hay un error 502 o similar
        try {
            WebElement errorElement = driver.findElement(By.xpath(XPATH_HTTP_ERROR));
            throw new RuntimeException(ERROR_HTTP_DETECTED + errorElement.getText());
        } catch (NoSuchElementException e) {
            // No hay error, continuar
        }

        // Detectar y cerrar popup de cookies/consentimiento
        closePopupIfPresent(driver, wait);

        // Seleccionar "Todos los jugadores" en la tabla de ligas
        selectAllPlayersInLeague(driver, wait);
    }

    @Override
    public List<PlayerDetailDTO> scrapeTeamPlayersByName(String teamName, String league) {
        String baseUrl = getBaseUrlByLeague(league);

        ChromeOptions options = createChromeOptions();
        WebDriver driver = createDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(12));
        List<PlayerDetailDTO> newPlayers = new ArrayList<>();
        int addedCount = 0;
        int updatedCount = 0;

        try {
            driver.get(baseUrl);
            Thread.sleep(2000);

            closePopupIfPresent(driver, wait);
            selectTeamFromDropdown(driver, wait, teamName);

            // Buscar la tabla específica con id="top-player-stats-summary-grid"
            List<WebElement> rows = findSquadRowsFromTable(driver, wait);
            for (WebElement row : rows) {
                try {
                    String rowClass = row.getAttribute(ATTR_CLASS);
                    if (rowClass != null) {
                        String normalizedClass = rowClass.replaceAll(REGEX_MULTIPLE_SPACES, SPACE).trim();
                        // Solo incluir jugadores activos (sin not-current-player)
                        if (normalizedClass.contains(CLASS_NOT_CURRENT_PLAYER)) {
                            continue;
                        }
                    }

                    PlayerDetailDTO player = extractPlayerDataFromRoster(row);
                    if (player == null || player.getName() == null || player.getName().isBlank()) {
                        continue;
                    }

                    // Forzar nombre de equipo objetivo para upsert por nombre + equipo.
                    player.setTeam(teamName);
                    player.setLeague(league);

                    // Verificar si el jugador ya existe
                    List<Player> existingPlayers = playerRepository.findByNameIgnoreCaseAndTeamIgnoreCase(
                            player.getName().trim(),
                            player.getTeam().trim());

                    if (existingPlayers.isEmpty()) {
                        // Jugador nuevo - agregarlo
                        playerDesdeCero(player, playerRepository);
                        newPlayers.add(player);
                        addedCount++;
                    } else {
                        // Jugador existe - actualizar estadísticas
                        modificandoPlayer(player, existingPlayers, playerRepository);
                        updatedCount++;
                    }

                } catch (Exception e) {
                    // Continuar con el siguiente jugador
                }
            }

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

    static void modificandoPlayer(PlayerDetailDTO player, List<Player> existingPlayers, PlayerRepository playerRepository) {
        for (Player existingPlayer : existingPlayers) {
            existingPlayer.setRating(player.getRating());
            existingPlayer.setAppearances(player.getAppearances());
            existingPlayer.setMinutes(player.getMinutes());
            existingPlayer.setGoals(player.getGoals());
            existingPlayer.setAssists(player.getAssists());
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
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        List<PlayerDetailDTO> allNewPlayers = new ArrayList<>();

        try {
            prepareLeaguePlayersPage(url, driver, wait);

            boolean hasNextButton = true;
            int pageCount = 0;

            while (hasNextButton) {
                pageCount++;

                // Esperar a que cargue la tabla con timeout corto
                List<WebElement> rows = loadCurrentPageRows(wait, driver);
                int newPlayersInPage = 0;
                List<PlayerDetailDTO> newPlayersThisPage = new ArrayList<>();

                for (WebElement row : rows) {
                    try {
                        // Omitir jugadores que ya no pertenecen a la liga
                        String rowClass = row.getAttribute(ATTR_CLASS);
                        if (rowClass != null) {
                            // Normalizar espacios y verificar si contiene not-current-player
                            String normalizedClass = rowClass.replaceAll(REGEX_MULTIPLE_SPACES, SPACE).trim();
                            if (normalizedClass.contains(CLASS_NOT_CURRENT_PLAYER)) {
                                continue;
                            }
                        }

                        PlayerDetailDTO player = extractPlayerData(row);
                        if (player != null && !player.getName().isEmpty()) {
                            player.setLeague(league);

                            // Verificar si el jugador ya existe
                            if (!playerRepository.findByNameIgnoreCaseAndTeamIgnoreCase(
                                    player.getName().trim(),
                                    player.getTeam().trim())
                                    .isEmpty()) {
                                continue;
                            }

                            allNewPlayers.add(player);
                            newPlayersThisPage.add(player);
                            newPlayersInPage++;
                        }
                    } catch (Exception e) {
                        // Continuar con el siguiente jugador
                    }
                }

                // Persistir solo los jugadores nuevos de esta página
                if (newPlayersInPage > 0) {
                    onPageComplete.accept(newPlayersThisPage);
                }

                // Buscar y hacer click en el botón "Siguiente"
                hasNextButton = isHasNextButton(driver, hasNextButton);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ERROR_DURING_SCRAPING + e.getMessage(), e);

        } catch (Exception e) {
            throw new RuntimeException(ERROR_DURING_SCRAPING + e.getMessage(), e);
        } finally {
            driver.quit();
        }

        return allNewPlayers;
    }

    private @NonNull List<WebElement> loadCurrentPageRows(WebDriverWait wait, WebDriver driver) throws InterruptedException {
        try {
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector(CSS_TBODY_TR)));
        } catch (TimeoutException e) {
            throw new RuntimeException(ERROR_TABLE_NOT_LOADED);
        }

        // Pequeño delay adicional para asegurar que los datos se renderizaron
        Thread.sleep(1000);

        // Extraer jugadores de la página actual
        List<WebElement> rows = driver.findElements(By.cssSelector(CSS_TBODY_TR));
        return rows;
    }

    private String getBaseUrlByLeague(String league) {
        return switch(league) {
            case "LaLiga" -> "https://es.whoscored.com/teams/65/show/espa%C3%B1a-barcelona";
            case "Premier League" -> "https://es.whoscored.com/teams/167/show/inglaterra-manchester-city";
            case "Ligue 1" -> "https://es.whoscored.com/teams/304/show/francia-paris-saint-germain";
            case "Bundesliga" -> "https://es.whoscored.com/teams/796/show/alemania-union-berlin";
            case "Serie A" -> "https://es.whoscored.com/teams/75/show/italia-inter";
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
            Thread.sleep(500);
            WebElement table = driver.findElement(By.id(ID_TOP_PLAYER_STATS_SUMMARY_GRID));
            List<WebElement> rows = table.findElements(By.cssSelector(CSS_TBODY_TR));
            if (rows.isEmpty()) {
                throw new RuntimeException("La tabla de plantilla no contiene filas");
            }
            return rows;
        } catch (TimeoutException e) {
            throw new RuntimeException("No se encontró la tabla de plantilla (top-player-stats-summary-grid) en la página");
        } catch (NoSuchElementException e) {
            throw new RuntimeException("No se encontró la tabla de plantilla (top-player-stats-summary-grid) en la página");
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrumpido al esperar la tabla de plantilla", ie);
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
                    String position = metaDataSpans.get(1).getText().trim().replaceAll("^,\\s*", "");
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
                throw new RuntimeException("La tabla de plantilla no contiene filas");
            }
            return rows;
        } catch (TimeoutException e) {
            throw new RuntimeException("No se encontró la tabla de plantilla (Plantilla/Squad) en la página");
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrumpido al esperar la tabla de plantilla", ie);
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
                    String position = positionSpans.get(1).getText().trim().replaceAll("^,\\s*", "");
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
                    Thread.sleep(2000);
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
                    throw new RuntimeException("Interrumpido al seleccionar equipo", ie);
                } catch (Exception e) {
                    // Probar siguiente select
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al intentar seleccionar equipo: " + e.getMessage(), e);
        }

        throw new RuntimeException("No se encontró el equipo '" + teamName + "' en el selector de la página");
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
        ligas.put("LaLiga", "https://es.whoscored.com/regions/206/tournaments/4/seasons/10803/stages/24622/playerstatistics/espa%C3%B1a-laliga-2025-2026");
        ligas.put("Premier League", "https://es.whoscored.com/regions/252/tournaments/2/seasons/10743/stages/24533/playerstatistics/inglaterra-premier-league-2025-2026");
        ligas.put("Bundesliga", "https://es.whoscored.com/regions/81/tournaments/3/seasons/10720/stages/24478/playerstatistics/alemania-bundesliga-2025-2026");
        ligas.put("Serie A", "https://es.whoscored.com/regions/108/tournaments/5/seasons/10732/stages/24500/playerstatistics/italia-serie-a-2025-2026");
        ligas.put("Ligue 1", "https://es.whoscored.com/regions/74/tournaments/22/seasons/10792/stages/24609/playerstatistics/francia-ligue-1-2025-2026");

        int totalJugadores = 0;
        int[] totalGuardados = {0};
        boolean isFirstLeague = true;

        try {
            for (Map.Entry<String, String> liga : ligas.entrySet()) {
                logger.info("📊 Scrapeando {}...", liga.getKey());
                var jugadores = scrapeAllPlayers(
                    liga.getValue(),
                    liga.getKey(),
                    playersPage -> {
                        playerService.saveAllPlayers(playersPage);
                        totalGuardados[0] += playersPage.size();
                    },
                    isFirstLeague
                );
                totalJugadores += jugadores.size();
                isFirstLeague = false;
            }

            logger.info("✅ Scraping automático completado. Total: {} jugadores guardados", totalJugadores);
        } catch (Exception e) {
            logger.error("❌ Error en scraping automático: {}", e.getMessage());
        }
    }
}
