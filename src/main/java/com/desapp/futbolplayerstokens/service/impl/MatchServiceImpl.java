package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.exception.ResourceNotFoundException;
import com.desapp.futbolplayerstokens.modelo.Match;
import com.desapp.futbolplayerstokens.repository.MatchRepository;
import com.desapp.futbolplayerstokens.service.MatchService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MatchServiceImpl implements MatchService {

    private final MatchRepository matchRepository;

    public MatchServiceImpl(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    @Override
    public Match createMatch(Match match) {
        return matchRepository.save(match);
    }

    @Override
    public Optional<Match> getMatchById(Long id) {
        return matchRepository.findById(id);
    }

    @Override
    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }

    @Override
    public List<Match> getMatchesByTeamId(Long teamId) {
        return matchRepository.findByTeam1IdOrTeam2Id(teamId, teamId);
    }

    @Override
    public Match updateMatch(Long id, Match match) {
        return matchRepository.findById(id)
                .map(existingMatch -> {
                    existingMatch.setFootballDataMatchId(match.getFootballDataMatchId());
                    existingMatch.setTeam1Id(match.getTeam1Id());
                    existingMatch.setTeam2Id(match.getTeam2Id());
                    existingMatch.setMatchTime(match.getMatchTime());
                    return matchRepository.save(existingMatch);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + id));
    }

    @Override
    public void deleteMatch(Long id) {
        if (!matchRepository.existsById(id)) {
            throw new ResourceNotFoundException("Match not found with id: " + id);
        }
        matchRepository.deleteById(id);
    }
}
