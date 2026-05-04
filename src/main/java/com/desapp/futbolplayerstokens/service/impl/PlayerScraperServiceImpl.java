package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.PlayerDTO;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.service.PlayerScraperService;
import com.desapp.futbolplayerstokens.service.PlayerService;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
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
import java.text.Normalizer;
import java.util.Locale;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class PlayerScraperServiceImpl implements PlayerScraperService {

    private final PlayerRepository playerRepository;
    private final PlayerService playerService;

    public PlayerScraperServiceImpl(PlayerRepository playerRepository, PlayerService playerService) {
        this.playerRepository = playerRepository;
        this.playerService = playerService;
    }

    @Override
    public List<PlayerDTO> scrapeAllPlayers(String url, String league, java.util.function.Consumer<List<PlayerDTO>> onPageComplete) {
        return scrapeAllPlayers(url, league, onPageComplete, true);
    }

    @Override
    public List<PlayerDTO> scrapeAllPlayers(String url, String league, java.util.function.Consumer<List<PlayerDTO>> onPageComplete, boolean clearTable) {
        // Limpiar tabla de players antes de scrapear (solo si clearTable es true)
        if (clearTable) {
            long count = playerRepository.count();
            if (count > 0) {
                System.out.println("\n🗑️ Limpiando tabla de players... Eliminando " + count + " jugadores");
                playerRepository.deleteAll();
                System.out.println("✓ Tabla limpiada\n");
            }
        }

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-web-resources");

        WebDriver driver;
        try {
            // Try to connect to remote Selenium server (for Docker)
            driver = new RemoteWebDriver(new URL("http://localhost:4444"), options);
        } catch (Exception e) {
            // Fallback to local ChromeDriver
            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver(options);
        }
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        List<PlayerDTO> allPlayers = new ArrayList<>();

        try {
            driver.get(url);

            // Esperar a que cargue la página inicial (más tiempo en headless)
            Thread.sleep(4000);

            // Verificar si hay un error 502 o similar
            try {
                WebElement errorElement = driver.findElement(By.xpath("//*[contains(text(), '502') or contains(text(), 'Bad Gateway') or contains(text(), '503') or contains(text(), 'Service Unavailable')]"));
                throw new RuntimeException("❌ Error HTTP detectado en la página: " + errorElement.getText());
            } catch (NoSuchElementException e) {
                // No hay error, continuar
            }

            // Detectar y cerrar popup de cookies/consentimiento
            closePopupIfPresent(driver, wait);

            // Seleccionar "Todos los jugadores" en la tabla de ligas
            selectAllPlayersInLeague(driver, wait);

            boolean hasNextButton = true;
            int pageCount = 0;

            while (hasNextButton) {
                pageCount++;
                System.out.println("========================================");
                System.out.println("Scrapeando página " + pageCount);
                System.out.println("========================================");

                // Esperar a que cargue la tabla con timeout corto
                try {
                    wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                        By.cssSelector("tbody tr")));
                } catch (TimeoutException e) {
                    throw new RuntimeException("❌ La tabla no cargó. Posible error 502 o servidor caído.");
                }

                // Pequeño delay adicional para asegurar que los datos se renderizaron
                Thread.sleep(1000);

                // Extraer jugadores de la página actual
                List<WebElement> rows = driver.findElements(By.cssSelector("tbody tr"));
                int validPlayersInPage = 0;

                for (WebElement row : rows) {
                    try {
                        // Omitir jugadores que ya no pertenecen a la liga
                        String rowClass = row.getAttribute("class");
                        if (rowClass != null) {
                            // Normalizar espacios y verificar si contiene not-current-player
                            String normalizedClass = rowClass.replaceAll("\\s+", " ").trim();
                            if (normalizedClass.contains("not-current-player")) {
                                System.out.println("⏭️ Omitiendo jugador que ya no pertenece a la liga");
                                continue;
                            }
                        }

                        PlayerDTO player = extractPlayerData(row);
                        if (player != null && !player.getName().isEmpty()) {
                            player.setLeague(league);
                            allPlayers.add(player);
                            validPlayersInPage++;
                            System.out.println("✓ " + player.getName() + " - Rating: " + player.getRating());
                        }
                    } catch (Exception e) {
                        System.err.println("Error extrayendo jugador: " + e.getMessage());
                    }
                }

                System.out.println("Jugadores válidos en esta página: " + validPlayersInPage);

                // Persistir los jugadores de esta página
                if (validPlayersInPage > 0) {
                    List<PlayerDTO> playersThisPage = allPlayers.subList(
                        Math.max(0, allPlayers.size() - validPlayersInPage),
                        allPlayers.size()
                    );
                    onPageComplete.accept(new ArrayList<>(playersThisPage));
                }

                // Buscar y hacer click en el botón "Siguiente"
                try {
                    WebElement nextButton = findNextButton(driver);

                    if (nextButton != null) {
                        // Verificar si está deshabilitado
                        String disabledAttr = nextButton.getAttribute("disabled");
                        String ariaDisabled = nextButton.getAttribute("aria-disabled");
                        String classAttr = nextButton.getAttribute("class");

                        boolean isDisabled = disabledAttr != null ||
                                           "true".equals(ariaDisabled) ||
                                           (classAttr != null && classAttr.contains("disabled"));

                        if (!isDisabled && nextButton.isDisplayed()) {
                            // Scroll hasta el botón y hacer click
                            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", nextButton);
                            Thread.sleep(500);

                            System.out.println("Haciendo click en 'Siguiente'...");
                            nextButton.click();

                            // Esperar a que carguen completamente los nuevos datos
                            Thread.sleep(1500);
                        } else {
                            System.out.println("Botón 'Siguiente' deshabilitado o no visible. Fin del scraping.");
                            hasNextButton = false;
                        }
                    } else {
                        System.out.println("No se encontró botón 'Siguiente'. Fin del scraping.");
                        hasNextButton = false;
                    }

                } catch (NoSuchElementException e) {
                    System.out.println("No se encontró botón 'Siguiente'. Fin del scraping.");
                    hasNextButton = false;
                } catch (Exception e) {
                    System.err.println("Error al hacer click en siguiente: " + e.getMessage());
                    hasNextButton = false;
                }
            }

        } catch (Exception e) {
            System.err.println("Error durante el scraping: " + e.getMessage());
            throw new RuntimeException("❌ Error durante el scraping: " + e.getMessage(), e);
        } finally {
            System.out.println("\nCerrando navegador...");
            driver.quit();
        }

        System.out.println("\n========================================");
        System.out.println("TOTAL JUGADORES SCRAPEADOS: " + allPlayers.size());
        System.out.println("========================================");
        return allPlayers;
    }

    @Override
    public List<PlayerDTO> scrapeTeamPlayersByName(String teamName, String league) {
        String baseUrl = getBaseUrlByLeague(league);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-web-resources");

        WebDriver driver;
        try {
            // Try to connect to remote Selenium server (for Docker)
            driver = new RemoteWebDriver(new URL("http://localhost:4444"), options);
        } catch (Exception e) {
            // Fallback to local ChromeDriver
            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver(options);
        }
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(12));
        List<PlayerDTO> newPlayers = new ArrayList<>();
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
                    String rowClass = row.getAttribute("class");
                    if (rowClass != null) {
                        String normalizedClass = rowClass.replaceAll("\\s+", " ").trim();
                        // Solo incluir jugadores activos (sin not-current-player)
                        if (normalizedClass.contains("not-current-player")) {
                            System.out.println("⏭️ Omitiendo jugador inactivo");
                            continue;
                        }
                    }

                    PlayerDTO player = extractPlayerDataFromRoster(row);
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
                        newPlayers.add(player);
                        addedCount++;
                        System.out.println("✅ NUEVO AGREGADO: " + player.getName());
                    } else {
                        // Jugador existe - actualizar estadísticas
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
                        updatedCount++;
                        System.out.println("📝 ACTUALIZADO: " + player.getName());
                    }

                } catch (Exception e) {
                    System.err.println("Error extrayendo jugador de plantilla: " + e.getMessage());
                }
            }

            int totalScraped = addedCount + updatedCount;
            System.out.println("\n📋 Plantilla scrapeada para " + teamName + ": " + totalScraped + " jugadores procesados");
            System.out.println("✅ Nuevos agregados: " + addedCount);
            System.out.println("📝 Actualizados: " + updatedCount);
            return newPlayers;
        } catch (Exception e) {
            throw new RuntimeException("❌ Error scrapeando plantilla de " + teamName + ": " + e.getMessage(), e);
        } finally {
            driver.quit();
        }
    }

    @Override
    public List<PlayerDTO> scrapeNewPlayersOnly(String url, String league, java.util.function.Consumer<List<PlayerDTO>> onPageComplete) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-web-resources");

        WebDriver driver;
        try {
            // Try to connect to remote Selenium server (for Docker)
            driver = new RemoteWebDriver(new URL("http://localhost:4444"), options);
        } catch (Exception e) {
            // Fallback to local ChromeDriver
            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver(options);
        }
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        List<PlayerDTO> allNewPlayers = new ArrayList<>();

        try {
            driver.get(url);

            // Esperar a que cargue la página inicial (más tiempo en headless)
            Thread.sleep(4000);

            // Verificar si hay un error 502 o similar
            try {
                WebElement errorElement = driver.findElement(By.xpath("//*[contains(text(), '502') or contains(text(), 'Bad Gateway') or contains(text(), '503') or contains(text(), 'Service Unavailable')]"));
                throw new RuntimeException("❌ Error HTTP detectado en la página: " + errorElement.getText());
            } catch (NoSuchElementException e) {
                // No hay error, continuar
            }

            // Detectar y cerrar popup de cookies/consentimiento
            closePopupIfPresent(driver, wait);

            // Seleccionar "Todos los jugadores" en la tabla de ligas
            selectAllPlayersInLeague(driver, wait);

            boolean hasNextButton = true;
            int pageCount = 0;

            while (hasNextButton) {
                pageCount++;
                System.out.println("========================================");
                System.out.println("Scrapeando página " + pageCount);
                System.out.println("========================================");

                // Esperar a que cargue la tabla con timeout corto
                try {
                    wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                        By.cssSelector("tbody tr")));
                } catch (TimeoutException e) {
                    throw new RuntimeException("❌ La tabla no cargó. Posible error 502 o servidor caído.");
                }

                // Pequeño delay adicional para asegurar que los datos se renderizaron
                Thread.sleep(1000);

                // Extraer jugadores de la página actual
                List<WebElement> rows = driver.findElements(By.cssSelector("tbody tr"));
                int newPlayersInPage = 0;
                List<PlayerDTO> newPlayersThisPage = new ArrayList<>();

                for (WebElement row : rows) {
                    try {
                        // Omitir jugadores que ya no pertenecen a la liga
                        String rowClass = row.getAttribute("class");
                        if (rowClass != null) {
                            // Normalizar espacios y verificar si contiene not-current-player
                            String normalizedClass = rowClass.replaceAll("\\s+", " ").trim();
                            if (normalizedClass.contains("not-current-player")) {
                                System.out.println("⏭️ Omitiendo jugador que ya no pertenece a la liga");
                                continue;
                            }
                        }

                        PlayerDTO player = extractPlayerData(row);
                        if (player != null && !player.getName().isEmpty()) {
                            player.setLeague(league);

                            // Verificar si el jugador ya existe
                            if (!playerRepository.findByNameIgnoreCaseAndTeamIgnoreCase(
                                    player.getName().trim(),
                                    player.getTeam().trim())
                                    .isEmpty()) {
                                System.out.println("⚠️ Jugador " + player.getName() + " (" + player.getTeam() + ") ya existe, omitiendo");
                                continue;
                            }

                            allNewPlayers.add(player);
                            newPlayersThisPage.add(player);
                            newPlayersInPage++;
                            System.out.println("✓ NUEVO - " + player.getName() + " (" + player.getTeam() + ") - Rating: " + player.getRating());
                        }
                    } catch (Exception e) {
                        System.err.println("Error extrayendo jugador: " + e.getMessage());
                    }
                }

                System.out.println("Jugadores nuevos en esta página: " + newPlayersInPage);

                // Persistir solo los jugadores nuevos de esta página
                if (newPlayersInPage > 0) {
                    onPageComplete.accept(newPlayersThisPage);
                }

                // Buscar y hacer click en el botón "Siguiente"
                try {
                    WebElement nextButton = findNextButton(driver);

                    if (nextButton != null) {
                        // Verificar si está deshabilitado
                        String disabledAttr = nextButton.getAttribute("disabled");
                        String ariaDisabled = nextButton.getAttribute("aria-disabled");
                        String classAttr = nextButton.getAttribute("class");

                        boolean isDisabled = disabledAttr != null ||
                                           "true".equals(ariaDisabled) ||
                                           (classAttr != null && classAttr.contains("disabled"));

                        if (!isDisabled && nextButton.isDisplayed()) {
                            // Scroll hasta el botón y hacer click
                            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", nextButton);
                            Thread.sleep(500);

                            System.out.println("Haciendo click en 'Siguiente'...");
                            nextButton.click();

                            // Esperar a que carguen completamente los nuevos datos
                            Thread.sleep(1500);
                        } else {
                            System.out.println("Botón 'Siguiente' deshabilitado o no visible. Fin del scraping.");
                            hasNextButton = false;
                        }
                    } else {
                        System.out.println("No se encontró botón 'Siguiente'. Fin del scraping.");
                        hasNextButton = false;
                    }

                } catch (NoSuchElementException e) {
                    System.out.println("No se encontró botón 'Siguiente'. Fin del scraping.");
                    hasNextButton = false;
                } catch (Exception e) {
                    System.err.println("Error al hacer click en siguiente: " + e.getMessage());
                    hasNextButton = false;
                }
            }

        } catch (Exception e) {
            System.err.println("Error durante el scraping: " + e.getMessage());
            throw new RuntimeException("❌ Error durante el scraping: " + e.getMessage(), e);
        } finally {
            System.out.println("\nCerrando navegador...");
            driver.quit();
        }

        System.out.println("\n========================================");
        System.out.println("TOTAL JUGADORES NUEVOS SCRAPEADOS: " + allNewPlayers.size());
        System.out.println("========================================");
        return allNewPlayers;
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
                    int first = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
                    int second = Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
                    return first + second;
                }
            } catch (NumberFormatException e) {
                // Fallback a extracción simple
            }
        }

        // Extracción simple de números
        String numericOnly = text.replaceAll("[^0-9]", "");
        try {
            return !numericOnly.isEmpty() ? Integer.parseInt(numericOnly) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private List<WebElement> findSquadRowsFromTable(WebDriver driver, WebDriverWait wait) {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("top-player-stats-summary-grid")));
            Thread.sleep(500);
            WebElement table = driver.findElement(By.id("top-player-stats-summary-grid"));
            List<WebElement> rows = table.findElements(By.cssSelector("tbody tr"));
            if (rows.isEmpty()) {
                throw new RuntimeException("La tabla de plantilla no contiene filas");
            }
            System.out.println("✓ Tabla de plantilla detectada. Filas encontradas: " + rows.size());
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

    private PlayerDTO extractPlayerDataFromRoster(WebElement row) {
        try {
            // Buscar el nombre dentro del span dentro del a.player-link
            WebElement playerLink = row.findElement(By.cssSelector("a.player-link span"));
            String name = playerLink.getText().trim();

            if (name.isEmpty() || name.length() < 2) {
                return null;
            }

            PlayerDTO player = PlayerDTO.builder().build();
            player.setName(name);

            // Extraer la posición del span player-meta-data
            try {
                List<WebElement> metaDataSpans = row.findElements(By.cssSelector("span.player-meta-data"));
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
                    String text = cells.get(5).getText().trim().replaceAll("[^0-9]", "");
                    player.setMinutes(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setMinutes(0);
                }
            }

            // Índice 6: Goles
            if (cells.size() > 6) {
                try {
                    String text = cells.get(6).getText().trim().replaceAll("[^0-9]", "");
                    player.setGoals(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setGoals(0);
                }
            }

            // Índice 7: Asist (Asistencias)
            if (cells.size() > 7) {
                try {
                    String text = cells.get(7).getText().trim().replaceAll("[^0-9]", "");
                    player.setAssists(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setAssists(0);
                }
            }

            // Índice 8: Amar (Tarjetas Amarillas)
            if (cells.size() > 8) {
                try {
                    String text = cells.get(8).getText().trim().replaceAll("[^0-9]", "");
                    player.setYellowCards(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setYellowCards(0);
                }
            }

            // Índice 9: Roja (Tarjetas Rojas)
            if (cells.size() > 9) {
                try {
                    String text = cells.get(9).getText().trim().replaceAll("[^0-9]", "");
                    player.setRedCards(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setRedCards(0);
                }
            }

            // Índice 13: JdelP (Jugador del Partido)
            if (cells.size() > 13) {
                try {
                    String text = cells.get(13).getText().trim().replaceAll("[^0-9]", "");
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
        } catch (NoSuchElementException e) {
            return null;
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
            System.out.println("✓ Tabla de plantilla detectada. Filas encontradas: " + rows.size());
            return rows;
        } catch (TimeoutException e) {
            throw new RuntimeException("No se encontró la tabla de plantilla (Plantilla/Squad) en la página");
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrumpido al esperar la tabla de plantilla", ie);
        }
    }

    private PlayerDTO extractPlayerData(WebElement row) {
        try {
            // Buscar el nombre dentro del span dentro del a.player-link
            WebElement playerLink = row.findElement(By.cssSelector("a.player-link span"));
            String name = playerLink.getText().trim();

            if (name.isEmpty() || name.length() < 2) {
                return null;
            }

            PlayerDTO player = PlayerDTO.builder().build();
            player.setName(name);

            // Obtener equipo desde a.player-meta-data span.team-name
            try {
                WebElement teamElement = row.findElement(By.cssSelector("a.player-meta-data span.team-name"));
                player.setTeam(teamElement.getText().trim().replaceAll(",\\s*$", ""));
            } catch (NoSuchElementException e) {
                player.setTeam("");
            }

            // Obtener posición desde span.player-meta-data (el segundo dentro del span que sigue al <a>)
            try {
                List<WebElement> positionSpans = row.findElements(By.cssSelector("span > span.player-meta-data"));
                if (positionSpans.size() >= 2) {
                    // El segundo span contiene las posiciones (ej: ",  MP(CID),DL  ")
                    String position = positionSpans.get(1).getText().trim().replaceAll("^,\\s*", "");
                    player.setPosition(position);
                } else {
                    player.setPosition("");
                }
            } catch (NoSuchElementException e) {
                player.setPosition("");
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
                    String text = cells.get(3).getText().trim().replaceAll("[^0-9]", "");
                    player.setMinutes(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setMinutes(0);
                }
            }

            // Columna 4: Goles (Goles)
            if (cells.size() > 4) {
                try {
                    String text = cells.get(4).getText().trim().replaceAll("[^0-9]", "");
                    player.setGoals(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setGoals(0);
                }
            }

            // Columna 5: Asistencias (Asist)
            if (cells.size() > 5) {
                try {
                    String text = cells.get(5).getText().trim().replaceAll("[^0-9]", "");
                    player.setAssists(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setAssists(0);
                }
            }

            // Columna 6: Tarjetas Amarillas (Amar)
            if (cells.size() > 6) {
                try {
                    String text = cells.get(6).getText().trim().replaceAll("[^0-9]", "");
                    player.setYellowCards(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setYellowCards(0);
                }
            }

            // Columna 7: Tarjetas Rojas (Roja)
            if (cells.size() > 7) {
                try {
                    String text = cells.get(7).getText().trim().replaceAll("[^0-9]", "");
                    player.setRedCards(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setRedCards(0);
                }
            }

            // Columna 11: Jugador del Partido (JdelP)
            if (cells.size() > 11) {
                try {
                    String text = cells.get(11).getText().trim().replaceAll("[^0-9]", "");
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
        } catch (NoSuchElementException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void closePopupIfPresent(WebDriver driver, WebDriverWait wait) {
        try {
            System.out.println("🔍 Buscando popup de consentimiento...");

            // Intenta encontrar y hacer click en botones comunes de aceptación
            try {
                // Buscar botón "Aceptar todo", "Accept all", "Aceptar", etc.
                WebElement acceptButton = null;

                try {
                    acceptButton = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//*[contains(text(), 'Aceptar todo')]")));
                } catch (TimeoutException e1) {
                    try {
                        acceptButton = wait.until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//*[contains(text(), 'Accept all')]")));
                    } catch (TimeoutException e2) {
                        try {
                            acceptButton = wait.until(ExpectedConditions.elementToBeClickable(
                                By.xpath("//*[contains(text(), 'Aceptar')]")));
                        } catch (TimeoutException e3) {
                            try {
                                acceptButton = wait.until(ExpectedConditions.elementToBeClickable(
                                    By.xpath("//*[contains(text(), 'Accept')]")));
                            } catch (TimeoutException e4) {
                                acceptButton = wait.until(ExpectedConditions.elementToBeClickable(
                                    By.cssSelector("[data-testid='cookie-accept-all']")));
                            }
                        }
                    }
                }

                if (acceptButton != null && acceptButton.isDisplayed()) {
                    System.out.println("✓ Popup encontrado. Haciendo click en 'Aceptar todo'...");
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", acceptButton);
                    Thread.sleep(2000);
                    System.out.println("✓ Popup cerrado");
                }
            } catch (TimeoutException e) {
                System.out.println("ℹ️ No se encontró popup de consentimiento");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error al intentar cerrar popup: " + e.getMessage());
        }
    }

    private void selectAllPlayersInLeague(WebDriver driver, WebDriverWait wait) {
        try {
            System.out.println("🔍 Buscando botón 'Todos los jugadores'...");

            try {
                // Esperar y hacer click en el botón de todos los jugadores
                WebElement allPlayersButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//a[@class='option' or contains(@class, 'option')][contains(text(), 'Todos los jugadores')]")
                ));

                System.out.println("✓ Botón encontrado. Haciendo click...");
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", allPlayersButton);
                System.out.println("✓ Botón 'Todos los jugadores' clickeado");
            } catch (TimeoutException e) {
                System.out.println("⚠️ No se encontró botón 'Todos los jugadores', continuando...");
            }

            // Esperar a que se carguen los datos
            Thread.sleep(2000);
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("tbody tr")));
            System.out.println("✓ Datos cargados después de seleccionar 'Todos los jugadores'");

        } catch (TimeoutException e) {
            System.out.println("⚠️ Timeout esperando datos, continuando...");
        } catch (Exception e) {
            System.out.println("⚠️ Error al intentar seleccionar 'Todos los jugadores': " + e.getMessage());
        }
    }

    private void selectTeamFromDropdown(WebDriver driver, WebDriverWait wait, String teamName) {
        String target = normalize(teamName);

        try {
            // Buscar todos los selects EXCEPTO el locale-select
            List<WebElement> selects = wait.until(d -> d.findElements(By.tagName("select")));

            for (WebElement selectElement : selects) {
                try {
                    // Saltar el select de idioma
                    String selectId = selectElement.getAttribute("id");
                    if ("locale-select".equals(selectId)) {
                        System.out.println("⏭️ Saltando select de idioma (locale-select)");
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
                            wait.until(d -> !d.getCurrentUrl().equals(previousUrl) || d.findElements(By.cssSelector("tbody tr")).size() > 0);
                            Thread.sleep(1200);
                            System.out.println("✓ Equipo seleccionado en dropdown: " + optionText);
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

    private WebElement findNextButton(WebDriver driver) throws NoSuchElementException {
        // Intenta encontrar el botón "siguiente" de varias formas
        WebElement nextButton = null;

        // Primero intenta por ID (la forma más específica)
        try {
            nextButton = driver.findElement(By.id("next"));
            System.out.println("✓ Botón 'Siguiente' encontrado (ID: next)");
            return nextButton;
        } catch (NoSuchElementException e1) {
        }

        // Intenta por clase y atributo
        try {
            nextButton = driver.findElement(By.xpath("//a[@class='option  clickable'][@id='next']"));
            System.out.println("✓ Botón 'Siguiente' encontrado (XPath con clases)");
            return nextButton;
        } catch (NoSuchElementException e2) {
        }

        try {
            nextButton = driver.findElement(By.xpath("//a[contains(text(), 'siguiente')]"));
            System.out.println("✓ Botón 'Siguiente' encontrado (XPath por texto)");
            return nextButton;
        } catch (NoSuchElementException e3) {
        }

        try {
            nextButton = driver.findElement(By.xpath("//button[contains(text(), 'Siguiente')]"));
            System.out.println("✓ Botón 'Siguiente' encontrado (button)");
            return nextButton;
        } catch (NoSuchElementException e4) {
        }

        throw new NoSuchElementException("No se encontró botón 'Siguiente' con ningún selector");
    }
}
