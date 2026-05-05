package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.modelo.Match;

import java.util.List;
import java.util.Optional;

public interface MatchService {
    Match createMatch(Match match);
    Optional<Match> getMatchById(Long id);
    List<Match> getAllMatches();
    List<Match> getMatchesByTeamId(Long teamId);
    Match updateMatch(Long id, Match match);
    void deleteMatch(Long id);
}
