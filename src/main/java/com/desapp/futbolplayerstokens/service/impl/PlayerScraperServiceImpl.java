package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.PlayerDetailDTO;
import com.desapp.futbolplayerstokens.exception.ScrapingException;
import com.desapp.futbolplayerstokens.modelo.LeagueConstant;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.Portfolio;
import com.desapp.futbolplayerstokens.modelo.TeamEnum;
import com.desapp.futbolplayerstokens.modelo.User;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.repository.PortfolioRepository;
import com.desapp.futbolplayerstokens.repository.UserRepository;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.text.Normalizer;
import java.util.Locale;
import java.net.URI;
import java.net.URL;

@Service
public class PlayerScraperServiceImpl implements PlayerScraperService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerScraperServiceImpl.class);

    private static final String SELENIUM_REMOTE_URL = "http://localhost:4444";

    private static final String CHROME_ARG_NO_SANDBOX = "--no-sandbox";
    private static final String CHROME_ARG_DISABLE_DEV_SHM = "--disable-dev-shm-usage";
    private static final String CHROME_ARG_DISABLE_GPU = "--disable-gpu";
    private static final String CHROME_ARG_WINDOW_SIZE = "--window-size=1366,768";
    private static final String CHROME_ARG_DISABLE_AUTOMATION = "--disable-blink-features=AutomationControlled";
    private static final String CHROME_ARG_DISABLE_WEB_RESOURCES = "--disable-web-resources";

    private static final String CHROME_ARG_DISABLE_EXTENSIONS = "--disable-extensions";
    private static final String CHROME_ARG_DISABLE_BACKGROUND_NETWORKING = "--disable-background-networking";
    private static final String CHROME_ARG_DISABLE_SYNC = "--disable-sync";
    private static final String CHROME_ARG_DISABLE_TRANSLATE = "--disable-translate";
    private static final String CHROME_ARG_DISABLE_DEFAULT_APPS = "--disable-default-apps";
    private static final String CHROME_ARG_DISABLE_NOTIFICATIONS = "--disable-notifications";
    private static final String CHROME_ARG_DISABLE_BG_TIMER_THROTTLING = "--disable-background-timer-throttling";
    private static final String CHROME_ARG_DISABLE_COMPONENT_UPDATE = "--disable-component-update";
    private static final String CHROME_ARG_DISABLE_BREAKPAD = "--disable-breakpad";
    private static final String CHROME_ARG_NO_FIRST_RUN = "--no-first-run";
    private static final String CHROME_ARG_NO_DEFAULT_BROWSER_CHECK = "--no-default-browser-check";
    private static final String CHROME_ARG_DISABLE_FEATURES = "--disable-features=TranslateUI,ChromeWhatsNewUI,IsolateOrigins,site-per-process";
    private static final String CHROME_ARG_BLINK_SETTINGS = "--blink-settings=imagesEnabled=false";

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

    // Non-team options to filter out from dropdown (e.g., "All Players" view)
    private static final String[] NON_TEAM_DROPDOWN_OPTIONS = {
        "todos los jugadores", "all players", "all players in the league",
        "todos los equipos", "all teams"
    };

    // Teams skipped during dropdown loop and scraped separately via direct URL
    private static final String[] SKIP_TEAM_NAMES = {
        "Liverpool", "Crystal Palace"
    };

    private static final Map<String, String> TEAM_OVERRIDE_URLS = Map.of(
        "Liverpool", "https://es.whoscored.com/teams/26/show/inglaterra-liverpool",
        "Crystal Palace", "https://es.whoscored.com/teams/162/show/inglaterra-crystal-palace"
    );

    private static final String WORLD_CUP_STARTER_TEAM = "Mexico";

    private static final Map<String, String> WORLD_CUP_TEAM_URLS = new LinkedHashMap<>();
    static {
        WORLD_CUP_TEAM_URLS.put(WORLD_CUP_STARTER_TEAM, "https://es.whoscored.com/teams/972/show/international-mexico");
        WORLD_CUP_TEAM_URLS.put("South Africa", "https://es.whoscored.com/teams/485/show/sud%C3%A1frica-south-africa");
        WORLD_CUP_TEAM_URLS.put("South Korea", "https://es.whoscored.com/teams/1159/show/international-republic-of-korea");
        WORLD_CUP_TEAM_URLS.put("Czechia", "https://es.whoscored.com/teams/332/show/rep-checa-czechia");
        WORLD_CUP_TEAM_URLS.put("Canada", "https://es.whoscored.com/teams/1160/show/international-canada");
        WORLD_CUP_TEAM_URLS.put("Bosnia-Herzegovina", "https://es.whoscored.com/teams/768/show/international-bosnia-and-herzegovina");
        WORLD_CUP_TEAM_URLS.put("United States", "https://es.whoscored.com/teams/461/show/usa-usa");
        WORLD_CUP_TEAM_URLS.put("Paraguay", "https://es.whoscored.com/teams/417/show/paraguay-paraguay");
        WORLD_CUP_TEAM_URLS.put("Brazil", "https://es.whoscored.com/teams/409/show/brasil-brazil");
        WORLD_CUP_TEAM_URLS.put("Qatar", "https://es.whoscored.com/teams/2379/show/qatar-qatar");
        WORLD_CUP_TEAM_URLS.put("Scotland", "https://es.whoscored.com/teams/424/show/escocia-scotland");
        WORLD_CUP_TEAM_URLS.put("Morocco", "https://es.whoscored.com/teams/495/show/marruecos-morocco");
        WORLD_CUP_TEAM_URLS.put("Switzerland", "https://es.whoscored.com/teams/423/show/suiza-switzerland");
        WORLD_CUP_TEAM_URLS.put("Haiti", "https://es.whoscored.com/teams/2693/show/hait%C3%AD-haiti");
        WORLD_CUP_TEAM_URLS.put("Curaçao", "https://es.whoscored.com/teams/10649/show/indefinido-curacao");
        WORLD_CUP_TEAM_URLS.put("Tunisia", "https://es.whoscored.com/teams/959/show/t%C3%BAnez-tunisia");
        WORLD_CUP_TEAM_URLS.put("Ecuador", "https://es.whoscored.com/teams/419/show/ecuador-ecuador");
        WORLD_CUP_TEAM_URLS.put("Germany", "https://es.whoscored.com/teams/336/show/alemania-germany");
        WORLD_CUP_TEAM_URLS.put("Australia", "https://es.whoscored.com/teams/328/show/australia-australia");
        WORLD_CUP_TEAM_URLS.put("Sweden", "https://es.whoscored.com/teams/344/show/suecia-sweden");
        WORLD_CUP_TEAM_URLS.put("Ivory Coast", "https://es.whoscored.com/teams/973/show/costa-de-marfil-ivory-coast");
        WORLD_CUP_TEAM_URLS.put("Spain", "https://es.whoscored.com/teams/338/show/espa%C3%B1a-spain");
        WORLD_CUP_TEAM_URLS.put("Cape Verde Islands", "https://es.whoscored.com/teams/2555/show/cabo-verde-cabo-verde");
        WORLD_CUP_TEAM_URLS.put("Belgium", "https://es.whoscored.com/teams/339/show/b%C3%A9lgica-belgium");
        WORLD_CUP_TEAM_URLS.put("Netherlands", "https://es.whoscored.com/teams/335/show/holanda-netherlands");
        WORLD_CUP_TEAM_URLS.put("Uruguay", "https://es.whoscored.com/teams/967/show/uruguay-uruguay");
        WORLD_CUP_TEAM_URLS.put("Egypt", "https://es.whoscored.com/teams/944/show/egipto-egypt");
        WORLD_CUP_TEAM_URLS.put("Saudi Arabia", "https://es.whoscored.com/teams/494/show/arabia-saud%C3%AD-saudi-arabia");
        WORLD_CUP_TEAM_URLS.put("Turkey", "https://es.whoscored.com/teams/333/show/turqu%C3%ADa-turkiye");
        WORLD_CUP_TEAM_URLS.put("Japan", "https://es.whoscored.com/teams/986/show/japan-japan");
        WORLD_CUP_TEAM_URLS.put("Iran", "https://es.whoscored.com/teams/1293/show/ir%C3%A1n-iran");
        WORLD_CUP_TEAM_URLS.put("New Zealand", "https://es.whoscored.com/teams/1918/show/international-new-zealand");
        WORLD_CUP_TEAM_URLS.put("France", "https://es.whoscored.com/teams/341/show/international-france");
        WORLD_CUP_TEAM_URLS.put("Senegal", "https://es.whoscored.com/teams/957/show/international-senegal");
        WORLD_CUP_TEAM_URLS.put("Iraq", "https://es.whoscored.com/teams/2374/show/international-iraq");
        WORLD_CUP_TEAM_URLS.put("Norway", "https://es.whoscored.com/teams/334/show/international-norway");
        WORLD_CUP_TEAM_URLS.put("Argentina", "https://es.whoscored.com/teams/346/show/internacional-argentina");
        WORLD_CUP_TEAM_URLS.put("Algeria", "https://es.whoscored.com/teams/966/show/internacional-algeria");
        WORLD_CUP_TEAM_URLS.put("Austria", "https://es.whoscored.com/teams/324/show/internacional-austria");
        WORLD_CUP_TEAM_URLS.put("Jordan", "https://es.whoscored.com/teams/1489/show/internacional-jordan");
        WORLD_CUP_TEAM_URLS.put("Portugal", "https://es.whoscored.com/teams/340/show/internacional-portugal");
        WORLD_CUP_TEAM_URLS.put("DR Congo", "https://es.whoscored.com/teams/960/show/internacional-dr-congo");
        WORLD_CUP_TEAM_URLS.put("Uzbekistan", "https://es.whoscored.com/teams/1563/show/internacional-uzbekistan");
        WORLD_CUP_TEAM_URLS.put("Colombia", "https://es.whoscored.com/teams/408/show/internacional-colombia");
        WORLD_CUP_TEAM_URLS.put("Ghana", "https://es.whoscored.com/teams/965/show/internacional-ghana");
        WORLD_CUP_TEAM_URLS.put("Panama", "https://es.whoscored.com/teams/2694/show/internacional-panama");
        WORLD_CUP_TEAM_URLS.put("England", "https://es.whoscored.com/teams/345/show/internacional-england");
        WORLD_CUP_TEAM_URLS.put("Croatia", "https://es.whoscored.com/teams/337/show/internacional-croatia");
    }

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

    private static final int NAVIGATION_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 15000;

    private final PlayerRepository playerRepository;
    private final PlayerService playerService;
    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;

    public PlayerScraperServiceImpl(PlayerRepository playerRepository, PlayerService playerService,
                                    UserRepository userRepository, PortfolioRepository portfolioRepository) {
        this.playerRepository = playerRepository;
        this.playerService = playerService;
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
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

    private void navigateWithRetry(WebDriver driver, WebDriverWait wait, String url) throws InterruptedException {
        int attempt = 0;
        while (attempt < NAVIGATION_RETRIES) {
            attempt++;
            driver.get(url);
            Thread.sleep(Timings.INITIAL_PAGE_LOAD_MS);

            if (!isLikelyHttpErrorPage(driver.getTitle(), driver.getPageSource())) {
                return;
            }

            logger.warn("⚠️ Error 502 detectado al cargar {}. Intento {}/{}. Reintentando en 15s...", url, attempt, NAVIGATION_RETRIES);
            if (attempt < NAVIGATION_RETRIES) {
                Thread.sleep(RETRY_DELAY_MS);
            }
        }
        throw new ScrapingException(ERROR_HTTP_DETECTED + extractHttpErrorDetails(driver));
    }

    private void prepareLeaguePlayersPage(String url, WebDriver driver, WebDriverWait wait) throws InterruptedException {
        navigateWithRetry(driver, wait, url);

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
        ChromeOptions options = createChromeOptions();
        WebDriver driver = createDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Timings.TEAM_SELECTION);
        List<PlayerDetailDTO> newPlayers = new ArrayList<>();

        try {
            if (LeagueConstant.WORLD_CUP.equals(league)) {
                String directUrl = WORLD_CUP_TEAM_URLS.get(teamName);
                if (directUrl == null) {
                    throw new ScrapingException("No hay URL directa para '" + teamName + "' en World Cup. Agregala al mapa WORLD_CUP_TEAM_URLS.");
                }
                navigateWithRetry(driver, wait, directUrl);
                closePopupIfPresent(driver, wait);
                waitForTeamPageTitle(driver, wait, teamName);
            } else {
                String baseUrl = getBaseUrlByLeague(league);
                navigateWithRetry(driver, wait, baseUrl);
                closePopupIfPresent(driver, wait);
                selectTeamFromDropdown(driver, wait, teamName);
            }

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
        ChromeOptions options = createChromeOptions();
        WebDriver driver = createDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Timings.TEAM_SELECTION);
        List<PlayerDetailDTO> newPlayers = new ArrayList<>();

        try {
            if (LeagueConstant.WORLD_CUP.equals(league)) {
                for (Map.Entry<String, String> entry : WORLD_CUP_TEAM_URLS.entrySet()) {
                    String teamName = entry.getKey();
                    String directUrl = entry.getValue();
                    logger.info("➡️ Navegando a {} ({})", teamName, directUrl);
                    navigateWithRetry(driver, wait, directUrl);
                    Thread.sleep(Timings.POST_POPUP_DELAY_MS);
                    closePopupIfPresent(driver, wait);
                    waitForTeamPageTitle(driver, wait, teamName);
                    newPlayers.addAll(scrapeCurrentTeamRoster(driver, wait, teamName, league));
                }
                return newPlayers;
            }

            String baseUrl = getBaseUrlByLeague(league);
            driver.get(baseUrl);
            Thread.sleep(Timings.INITIAL_PAGE_LOAD_MS);

            closePopupIfPresent(driver, wait);
            selectTeamFromDropdown(driver, wait, starterTeam);

            List<String> teamNames = getTeamNamesFromDropdown(wait);
            List<String> orderedTeamNames = orderTeamsStartingWith(teamNames, starterTeam);

            // Filter out teams that redirect to wrong competition via dropdown
            List<String> dropdownTeams = new ArrayList<>();
            List<String> skipTeams = new ArrayList<>();
            for (String name : orderedTeamNames) {
                if (isSkipTeam(name)) {
                    skipTeams.add(name);
                } else {
                    dropdownTeams.add(name);
                }
            }

            // Scrape all teams via dropdown first
            for (String currentTeamName : dropdownTeams) {
                selectTeamFromDropdown(driver, wait, currentTeamName);
                newPlayers.addAll(scrapeCurrentTeamRoster(driver, wait, currentTeamName, league));
            }

            // Scrape skip teams via direct URL navigation
            for (String currentTeamName : skipTeams) {
                String overrideUrl = TEAM_OVERRIDE_URLS.get(currentTeamName);
                if (overrideUrl == null) {
                    logger.warn("⚠️ No hay URL override para {}. Saltando.", currentTeamName);
                    continue;
                }
                logger.info("➡️ Navegando directo a {} ({})", currentTeamName, overrideUrl);
                navigateWithRetry(driver, wait, overrideUrl);
                Thread.sleep(Timings.POST_POPUP_DELAY_MS);
                closePopupIfPresent(driver, wait);
                waitForTeamPageTitle(driver, wait, currentTeamName);
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
                        if (!optionText.isBlank() && isTeamOption(optionText)) {
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
        String canonicalTeamName = resolveTeamName(teamName);
        for (PlayerDetailDTO player : playersByName.values()) {
            player.setTeam(canonicalTeamName);
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
        if (source.getAltPosition() != null && !source.getAltPosition().isBlank()) {
            target.setAltPosition(source.getAltPosition());
        }
        if (source.getRating() != null) {
            target.setRating(source.getRating());
        }

        copyIfNotNull(source.getAppearances(), target::setAppearances);
        copyIfNotNull(source.getMinutes(), target::setMinutes);
        copyIfNotNull(source.getGoals(), target::setGoals);
        copyIfNotNull(source.getAssists(), target::setAssists);
        copyIfNotNull(source.getShotsOnTarget(), target::setShotsOnTarget);
        copyIfNotNull(source.getKeyPasses(), target::setKeyPasses);
        copyIfNotNull(source.getDribbles(), target::setDribbles);
        copyIfNotNull(source.getTackles(), target::setTackles);
        copyIfNotNull(source.getInterceptions(), target::setInterceptions);
        copyIfNotNull(source.getBlocks(), target::setBlocks);
        copyIfNotNull(source.getClears(), target::setClears);
        copyIfNotNull(source.getDribbled(), target::setDribbled);
        copyIfNotNull(source.getFaults(), target::setFaults);
        copyIfNotNull(source.getOffsidesGiven(), target::setOffsidesGiven);
        copyIfNotNull(source.getYellowCards(), target::setYellowCards);
        copyIfNotNull(source.getRedCards(), target::setRedCards);
        copyIfNotNull(source.getAerialWon(), target::setAerialWon);
        copyIfNotNull(source.getPlayerOfTheMatch(), target::setPlayerOfTheMatch);
        copyIfNotNull(source.getOwnGoals(), target::setOwnGoals);
        copyIfNotNull(source.getFaulted(), target::setFaulted);
        copyIfNotNull(source.getOffsides(), target::setOffsides);
        copyIfNotNull(source.getDispossesed(), target::setDispossesed);
        copyIfNotNull(source.getTurnover(), target::setTurnover);
        copyIfNotNull(source.getPassAccuracy(), target::setPassAccuracy);
    }

    private <T> void copyIfNotNull(T value, java.util.function.Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private void persistScrapedPlayer(PlayerDetailDTO player, List<PlayerDetailDTO> newPlayers) {
        List<Player> existingPlayers = playerRepository.findByNameIgnoreCaseAndTeamIgnoreCase(
                player.getName().trim(),
                player.getTeam().trim());

        if (existingPlayers.isEmpty()) {
            Player saved = playerDesdeCero(player, playerRepository);
            seedSuperuserPortfolioForPlayer(saved);
            newPlayers.add(player);
        } else {
            modificandoPlayer(player, existingPlayers, playerRepository);
        }
    }

    private void seedSuperuserPortfolioForPlayer(Player player) {
        User superuser = userRepository.findByUsername("superuser").orElse(null);
        if (superuser == null) return;
        if (portfolioRepository.findByUserAndPlayer(superuser, player).isPresent()) return;
        Portfolio portfolio = Portfolio.builder()
                .user(superuser)
                .player(player)
                .tokenQty(player.getTotalTokens())
                .avgBuyPrice(BigDecimal.ZERO)
                .build();
        portfolioRepository.save(portfolio);
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
            if (player.getPosition() != null && !player.getPosition().isBlank()) {
                existingPlayer.setPosition(player.getPosition());
            }
            if (player.getAltPosition() != null && !player.getAltPosition().isBlank()) {
                existingPlayer.setAltPosition(player.getAltPosition());
            }
            existingPlayer.setLastModifiedAt(LocalDateTime.now());
        }
        playerRepository.saveAll(existingPlayers);
    }

    static Player playerDesdeCero(PlayerDetailDTO player, PlayerRepository playerRepository) {
        Player newPlayer = Player.builder()
            .name(player.getName())
            .rating(player.getRating())
            .team(player.getTeam())
            .league(player.getLeague())
            .position(player.getPosition())
            .altPosition(player.getAltPosition())
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

        return playerRepository.save(newPlayer);
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
            case LeagueConstant.WORLD_CUP -> throw new IllegalArgumentException("World Cup usa URLs directas, no getBaseUrlByLeague");
            default -> "https://es.whoscored.com/teams/65/show/espa%C3%B1a-barcelona";
        };
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
                player.setAppearances(parseIntegerStat(cells.get(4).getText()));
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
                player.setAppearances(parseIntegerStat(cells.get(4).getText()));
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
                player.setAppearances(parseIntegerStat(cells.get(4).getText()));
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
                String rawPosition = positionSpans.get(1).getText().trim().replaceAll(REGEX_LEADING_COMMA_SPACE, EMPTY);
                applyParsedPositions(rawPosition, player);
            }
        } catch (NoSuchElementException e) {
            player.setPosition(EMPTY);
        }
    }

    static void applyParsedPositions(String raw, PlayerDetailDTO player) {
        if (raw == null || raw.isBlank()) {
            player.setPosition(EMPTY);
            player.setAltPosition(null);
            return;
        }
        java.util.Set<String> rawBases = new java.util.LinkedHashSet<>();
        java.util.Set<String> result = new java.util.LinkedHashSet<>();
        String[] parts = raw.split(",");
        for (String part : parts) {
            String base = part.trim();
            int paren = base.indexOf('(');
            if (paren != -1) {
                base = base.substring(0, paren).trim();
            }
            if (base.isEmpty()) continue;
            rawBases.add(base.toUpperCase(java.util.Locale.ROOT));
            String mapped = mapPositionBase(base);
            if (mapped != null) {
                result.add(mapped);
            }
        }
        if (rawBases.contains("DM") && !result.contains("Defender")) {
            result.add("Defender");
        }
        if (rawBases.contains("AM") && !result.contains("Forward")) {
            result.add("Forward");
        }
        if (result.isEmpty()) {
            player.setPosition(EMPTY);
            player.setAltPosition(null);
            return;
        }
        java.util.Iterator<String> it = result.iterator();
        player.setPosition(it.next());
        if (it.hasNext()) {
            StringBuilder sb = new StringBuilder(it.next());
            while (it.hasNext()) {
                sb.append(", ").append(it.next());
            }
            player.setAltPosition(sb.toString());
        } else {
            player.setAltPosition(null);
        }
    }

    private static String mapPositionBase(String base) {
        if (base == null || base.isBlank()) return null;
        String u = base.toUpperCase(java.util.Locale.ROOT).trim();
        if (u.equals("DF") || u.equals("D") || u.equals("DEFENDER")) return "Defender";
        if (u.equals("ME") || u.equals("MP") || u.equals("MC") || u.equals("M") || u.equals("AM") || u.equals("DM") || u.equals("MIDFIELDER")) return "Midfielder";
        if (u.equals("FW") || u.equals("DL") || u.equals("FORWARD")) return "Forward";
        if (u.equals("POR") || u.equals("GOALKEEPER")) return "Goalkeeper";
        return null;
    }

    private Integer parseCellAsInteger(List<WebElement> cells, int index) {
        if (cells.size() > index) {
            try {
                String text = cells.get(index).getText().trim().replaceAll(REGEX_NON_NUMERIC, EMPTY);
                return !text.isEmpty() ? Integer.parseInt(text) : 0;
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
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
            WebElement playerLink = row.findElement(By.cssSelector(CSS_PLAYER_LINK_SPAN));
            String name = playerLink.getText().trim();

            if (name.isEmpty() || name.length() < 2) {
                return null;
            }

            PlayerDetailDTO player = PlayerDetailDTO.builder().build();
            player.setName(name);

            try {
                List<WebElement> metaDataSpans = row.findElements(By.cssSelector(CSS_PLAYER_META_DATA));
                if (metaDataSpans.size() >= 2) {
                    String rawPosition = metaDataSpans.get(1).getText().trim().replaceAll(REGEX_LEADING_COMMA_SPACE, EMPTY);
                    applyParsedPositions(rawPosition, player);
                }
            } catch (Exception e) {
                // Si no se puede extraer la posición, continuar sin ella
            }

            List<WebElement> cells = row.findElements(By.tagName("td"));
            player.setAppearances(parseIntegerStat(cells.size() > 4 ? cells.get(4).getText() : "0"));
            player.setMinutes(parseCellAsInteger(cells, 5));
            player.setGoals(parseCellAsInteger(cells, 6));
            player.setAssists(parseCellAsInteger(cells, 7));
            player.setYellowCards(parseCellAsInteger(cells, 8));
            player.setRedCards(parseCellAsInteger(cells, 9));
            player.setPlayerOfTheMatch(parseCellAsInteger(cells, 13));

            if (cells.size() > 14) {
                String ratingText = cells.get(14).getText().trim();
                try {
                    player.setRating(Double.parseDouble(ratingText));
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
            WebElement playerLink = row.findElement(By.cssSelector(CSS_PLAYER_LINK_SPAN));
            String name = playerLink.getText().trim();

            if (name.isEmpty() || name.length() < 2) {
                return null;
            }

            PlayerDetailDTO player = PlayerDetailDTO.builder().build();
            player.setName(name);

            try {
                WebElement teamElement = row.findElement(By.cssSelector(CSS_PLAYER_META_TEAM_NAME));
                String rawTeam = teamElement.getText().trim().replaceAll(",\\s*$", EMPTY);
                player.setTeam(resolveTeamName(rawTeam));
            } catch (NoSuchElementException e) {
                player.setTeam(EMPTY);
            }

            try {
                List<WebElement> positionSpans = row.findElements(By.cssSelector(CSS_NESTED_PLAYER_META_DATA));
                if (positionSpans.size() >= 2) {
                    String rawPosition = positionSpans.get(1).getText().trim().replaceAll(REGEX_LEADING_COMMA_SPACE, EMPTY);
                    applyParsedPositions(rawPosition, player);
                } else {
                    player.setPosition(EMPTY);
                }
            } catch (NoSuchElementException e) {
                player.setPosition(EMPTY);
            }

            List<WebElement> cells = row.findElements(By.tagName("td"));
            player.setAppearances(parseIntegerStat(cells.size() > 2 ? cells.get(2).getText() : "0"));
            player.setMinutes(parseCellAsInteger(cells, 3));
            player.setGoals(parseCellAsInteger(cells, 4));
            player.setAssists(parseCellAsInteger(cells, 5));
            player.setYellowCards(parseCellAsInteger(cells, 6));
            player.setRedCards(parseCellAsInteger(cells, 7));
            player.setPlayerOfTheMatch(parseCellAsInteger(cells, 11));

            if (cells.size() > 12) {
                String ratingText = cells.get(12).getText().trim();
                try {
                    player.setRating(Double.parseDouble(ratingText));
                } catch (NumberFormatException e) {
                    player.setRating(0.0);
                }
            }

            return player;
        } catch (Exception e) {
            return null;
        }
    }

    private WebElement findElementWithFallback(WebDriverWait wait, String... xpaths) {
        for (String xpath : xpaths) {
            try {
                return wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
            } catch (TimeoutException e) {
                // Intentar siguiente
            }
        }
        return null;
    }

    private void closePopupIfPresent(WebDriver driver, WebDriverWait wait) {
        try {
            WebElement acceptButton = findElementWithFallback(wait,
                XPATH_ACCEPTAR_TODO, XPATH_ACCEPT_ALL, XPATH_ACCEPTAR, XPATH_ACCEPT);

            if (acceptButton == null) {
                try {
                    acceptButton = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector(CSS_COOKIE_ACCEPT_ALL)));
                } catch (TimeoutException e) {
                    return;
                }
            }

            if (acceptButton != null && acceptButton.isDisplayed()) {
                ((JavascriptExecutor) driver).executeScript(JS_CLICK_ELEMENT, acceptButton);
                Thread.sleep(Timings.POST_POPUP_DELAY_MS);
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
            List<WebElement> selects = wait.until(d -> d.findElements(By.tagName(CSS_SELECT_TAG)));

            for (WebElement selectElement : selects) {
                try {
                    String selectId = selectElement.getAttribute(ATTR_ID);
                    if (ID_LOCALE_SELECT.equals(selectId)) {
                        continue;
                    }

                    Select select = new Select(selectElement);
                    List<WebElement> options = select.getOptions();

                    String currentlySelected = select.getFirstSelectedOption().getText().trim();
                    if (normalize(currentlySelected).equals(target)) {
                        return;
                    }

                    for (WebElement option : options) {
                        String optionText = option.getText().trim();
                        String normalizedOption = normalize(optionText);
                        if (normalizedOption.equals(target) || normalizedOption.contains(target) || target.contains(normalizedOption)) {
                            String squadBefore = getSquadSectionPreview(driver);

                            select.selectByVisibleText(optionText);

                            if (!waitForSquadContentChange(driver, wait, squadBefore)) {
                                logger.warn("⚠️ La tabla de plantilla no cambió después de seleccionar {}. " +
                                    "Se reintentará.", teamName);

                                select.selectByVisibleText(optionText);
                                Thread.sleep(2000);
                                if (!waitForSquadContentChange(driver, wait, squadBefore)) {
                                    throw new ScrapingException(
                                        "No se pudo cambiar al equipo '" + teamName + "' - la tabla no se actualizó");
                                }
                            }

                            Thread.sleep(Timings.POST_TEAM_SELECT_DELAY_MS);
                            return;
                        }
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ScrapingException("Interrumpido al seleccionar equipo", ie);
                } catch (ScrapingException e) {
                    throw e;
                } catch (Exception e) {
                    // Probar siguiente select
                }
            }
        } catch (ScrapingException e) {
            throw e;
        } catch (Exception e) {
            throw new ScrapingException("Error al intentar seleccionar equipo: " + e.getMessage(), e);
        }

        throw new ScrapingException("No se encontró el equipo '" + teamName + "' en el selector de la página");
    }

    private String getSquadSectionPreview(WebDriver driver) {
        try {
            WebElement section = driver.findElement(By.id(TEAM_SQUAD_SUMMARY_SECTION));
            String text = section.getText();
            return text != null && text.length() > 100 ? text.substring(0, 100) : text;
        } catch (Exception e) {
            return "";
        }
    }

    private boolean waitForSquadContentChange(WebDriver driver, WebDriverWait wait, String previousPreview) {
        if (previousPreview == null || previousPreview.isEmpty()) {
            return true;
        }

        try {
            wait.withTimeout(Duration.ofSeconds(8))
                .pollingEvery(Duration.ofMillis(300))
                .until(d -> {
                    String current = getSquadSectionPreview(d);
                    return !current.equals(previousPreview) && !current.isEmpty();
                });
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private boolean waitForTeamPageTitle(WebDriver driver, WebDriverWait wait, String expectedTeamName) {
        try {
            wait.withTimeout(Duration.ofSeconds(12))
                .pollingEvery(Duration.ofMillis(300))
                .until(d -> {
                    String title = d.getTitle();
                    return title != null && normalize(title).contains(normalize(expectedTeamName));
                });
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private String normalize(String text) {
        String normalized = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .trim();
        return normalized;
    }

    private boolean isSkipTeam(String teamName) {
        String normalized = normalize(teamName);
        for (String skip : SKIP_TEAM_NAMES) {
            if (normalize(skip).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTeamOption(String optionText) {
        String normalized = normalize(optionText);
        if (normalized.isEmpty()) {
            return false;
        }
        for (String nonTeam : NON_TEAM_DROPDOWN_OPTIONS) {
            if (normalized.contains(nonTeam)) {
                return false;
            }
        }
        return true;
    }

    private String resolveTeamName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return rawName;
        }
        String normalized = normalize(rawName);
        for (TeamEnum team : TeamEnum.values()) {
            if (normalize(team.getName()).equals(normalized)) {
                return team.getName();
            }
        }
        // Fallback: return the original if no match found
        return rawName;
    }

    private ChromeOptions createChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(CHROME_ARG_NO_SANDBOX);
        options.addArguments(CHROME_ARG_DISABLE_DEV_SHM);
        options.addArguments(CHROME_ARG_DISABLE_GPU);
        options.addArguments(CHROME_ARG_WINDOW_SIZE);
        options.addArguments(CHROME_ARG_DISABLE_AUTOMATION);
        options.addArguments(CHROME_ARG_DISABLE_WEB_RESOURCES);
        options.addArguments(CHROME_ARG_DISABLE_EXTENSIONS);
        options.addArguments(CHROME_ARG_DISABLE_BACKGROUND_NETWORKING);
        options.addArguments(CHROME_ARG_DISABLE_SYNC);
        options.addArguments(CHROME_ARG_DISABLE_TRANSLATE);
        options.addArguments(CHROME_ARG_DISABLE_DEFAULT_APPS);
        options.addArguments(CHROME_ARG_DISABLE_NOTIFICATIONS);
        options.addArguments(CHROME_ARG_DISABLE_BG_TIMER_THROTTLING);
        options.addArguments(CHROME_ARG_DISABLE_COMPONENT_UPDATE);
        options.addArguments(CHROME_ARG_DISABLE_BREAKPAD);
        options.addArguments(CHROME_ARG_NO_FIRST_RUN);
        options.addArguments(CHROME_ARG_NO_DEFAULT_BROWSER_CHECK);
        options.addArguments(CHROME_ARG_DISABLE_FEATURES);
        options.addArguments(CHROME_ARG_BLINK_SETTINGS);
        return options;
    }

    private WebDriver createDriver(ChromeOptions options) {
        try {
            // Try to connect to remote Selenium server (for Docker)
            return new RemoteWebDriver(URI.create(SELENIUM_REMOTE_URL).toURL(), options);
        } catch (Exception e) {
            // Fallback to local ChromeDriver
            WebDriverManager.chromedriver().setup();
            return new ChromeDriver(options);
        }
    }

    private WebElement findNextButton(WebDriver driver) throws NoSuchElementException {
        String[] selectors = {ID_NEXT, XPATH_NEXT_OPTION, XPATH_NEXT_LOWER, XPATH_NEXT_UPPER};

        for (String selector : selectors) {
            try {
                if (selector.equals(ID_NEXT)) {
                    return driver.findElement(By.id(selector));
                } else {
                    return driver.findElement(By.xpath(selector));
                }
            } catch (NoSuchElementException e) {
                // Continuar con siguiente selector
            }
        }

        throw new NoSuchElementException("No se encontró botón 'Siguiente' con ningún selector");
    }

    private void scrapeAllLeagues(Map<String, String> ligas, String logPrefix) {
        int totalJugadores = 0;
        try {
            for (Map.Entry<String, String> liga : ligas.entrySet()) {
                logger.info("📊 Scrapeando {}...", liga.getKey());
                var jugadores = scrapeLeaguePlayersByStarterTeam(liga.getValue(), liga.getKey());
                totalJugadores += jugadores.size();
            }
            logger.info("✅ {}: completado. Total: {} jugadores guardados", logPrefix, totalJugadores);
        } catch (Exception e) {
            logger.error("❌ Error en {}: {}", logPrefix, e.getMessage());
        }
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
        ligas.put(LeagueConstant.WORLD_CUP, WORLD_CUP_STARTER_TEAM);

        scrapeAllLeagues(ligas, "Scraping automático");
    }

    @Override
    public void scrapeAllPlayersForce() {
        logger.info("🚀 Forzando scraping completo: limpiando BD y scrapeando todas las ligas...");

        long count = playerRepository.count();
        if (count > 0) {
            playerRepository.deleteAll();
            logger.info("🧹 BD limpiada. Registros eliminados: {}", count);
        }

        Map<String, String> ligas = new LinkedHashMap<>();
        ligas.put(LeagueConstant.WORLD_CUP, WORLD_CUP_STARTER_TEAM);

        scrapeAllLeagues(ligas, "Scraping forzado");

        int deleted = playerRepository.deleteDuplicatesByNameAndTeam();
        if (deleted > 0) {
            logger.info("🧹 Duplicados eliminados después del scraping: {}", deleted);
        }
    }
}
