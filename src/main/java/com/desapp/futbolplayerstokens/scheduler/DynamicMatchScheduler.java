package com.desapp.futbolplayerstokens.scheduler;

import com.desapp.futbolplayerstokens.modelo.Match;
import com.desapp.futbolplayerstokens.repository.MatchRepository;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class DynamicMatchScheduler {

    private final TaskScheduler taskScheduler;
    private final MatchRepository matchRepository;

    // Guardar referencias a los schedulers activos para poder cancelarlos después
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> scheduledMatches = new ConcurrentHashMap<>();

    public DynamicMatchScheduler(TaskScheduler taskScheduler, MatchRepository matchRepository) {
        this.taskScheduler = taskScheduler;
        this.matchRepository = matchRepository;
    }

    /**
     * Programa schedulers para cada partido del día
     */
    public void scheduleMatchesForToday() {
        System.out.println("\n📅 Programando schedulers para partidos de hoy...");

        // Cancelar schedulers anteriores
        cancelAllSchedules();

        // Obtener todos los partidos
        var allMatches = matchRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        int scheduled = 0;

        for (Match match : allMatches) {
            // Solo programar si el partido es hoy y aún no ha ocurrido
            if (match.getMatchTime() != null &&
                match.getMatchTime().isAfter(now)) {

                scheduleMatch(match);
                scheduled++;
            }
        }

        System.out.println("✓ Se programaron " + scheduled + " partidos para hoy\n");
    }

    /**
     * Programa un partido individual
     */
    private void scheduleMatch(Match match) {
        long matchId = match.getId();
        LocalDateTime matchTime = match.getMatchTime();

        ScheduledFuture<?> future = taskScheduler.schedule(
            () -> executeMatchTask(match),
            matchTime.atZone(ZoneId.systemDefault()).toInstant()
        );

        scheduledMatches.put(matchId, future);
        System.out.println("  ⏱️ Partido ID " + matchId + " programado para " + matchTime);
    }

    /**
     * Tarea que se ejecuta cuando llega la hora del partido
     */
    private void executeMatchTask(Match match) {
        String time = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("\n🎯 [" + time + "] ⚽ PARTIDO ID " + match.getId() + " - " + match.getMatchTime());
        System.out.println("   Equipos: " + match.getTeam1Id() + " vs " + match.getTeam2Id());
        System.out.println("   → Aquí puedes agregar lógica (notificaciones, estadísticas, etc.)\n");

        // Remover del mapa de activos
        scheduledMatches.remove(match.getId());
    }

    /**
     * Cancela todos los schedulers activos
     */
    public void cancelAllSchedules() {
        for (ScheduledFuture<?> future : scheduledMatches.values()) {
            future.cancel(false);
        }
        scheduledMatches.clear();
        System.out.println("🧹 Schedulers anteriores cancelados");
    }

    /**
     * Obtiene cuántos partidos están programados
     */
    public int getScheduledMatchCount() {
        return scheduledMatches.size();
    }
}
