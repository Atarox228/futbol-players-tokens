package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.PlayerDTO;
import com.desapp.futbolplayerstokens.service.PlayerScraperService;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.TimeoutException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlayerScraperServiceImpl implements PlayerScraperService {

    @Override
    public List<PlayerDTO> scrapeAllPlayers(String url, String league, java.util.function.Consumer<List<PlayerDTO>> onPageComplete) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        // Sin headless para ver en tiempo real

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        List<PlayerDTO> allPlayers = new ArrayList<>();

        try {
            driver.get(url);

            // Esperar a que cargue la página inicial
            Thread.sleep(2000);

            // Verificar si hay un error 502 o similar
            try {
                WebElement errorElement = driver.findElement(By.xpath("//*[contains(text(), '502') or contains(text(), 'Bad Gateway') or contains(text(), '503') or contains(text(), 'Service Unavailable')]"));
                throw new RuntimeException("❌ Error HTTP detectado en la página: " + errorElement.getText());
            } catch (NoSuchElementException e) {
                // No hay error, continuar
            }

            // Detectar y cerrar popup de cookies/consentimiento
            closePopupIfPresent(driver, wait);

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
                    e.printStackTrace();
                    hasNextButton = false;
                }
            }

        } catch (Exception e) {
            System.err.println("Error durante el scraping: " + e.getMessage());
            e.printStackTrace();
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
                try {
                    String text = cells.get(2).getText().trim().replaceAll("[^0-9]", "");
                    player.setAppearances(!text.isEmpty() ? Integer.parseInt(text) : 0);
                } catch (NumberFormatException e) {
                    player.setAppearances(0);
                }
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

            // Última columna: Rating
            if (cells.size() > 0) {
                String ratingText = cells.get(cells.size() - 1).getText().trim();
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
                    acceptButton = driver.findElement(By.xpath("//*[contains(text(), 'Aceptar todo')]"));
                } catch (NoSuchElementException e1) {
                    try {
                        acceptButton = driver.findElement(By.xpath("//*[contains(text(), 'Accept all')]"));
                    } catch (NoSuchElementException e2) {
                        try {
                            acceptButton = driver.findElement(By.xpath("//*[contains(text(), 'Aceptar')]"));
                        } catch (NoSuchElementException e3) {
                            try {
                                acceptButton = driver.findElement(By.xpath("//*[contains(text(), 'Accept')]"));
                            } catch (NoSuchElementException e4) {
                                acceptButton = driver.findElement(By.cssSelector("[data-testid='cookie-accept-all']"));
                            }
                        }
                    }
                }

                if (acceptButton != null && acceptButton.isDisplayed()) {
                    System.out.println("✓ Popup encontrado. Haciendo click en 'Aceptar todo'...");
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", acceptButton);
                    Thread.sleep(1000);
                    System.out.println("✓ Popup cerrado");
                }
            } catch (NoSuchElementException e) {
                System.out.println("ℹ️ No se encontró popup de consentimiento");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error al intentar cerrar popup: " + e.getMessage());
        }
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
