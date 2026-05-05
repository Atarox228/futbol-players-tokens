package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.modelo.Match;

import java.util.List;

public interface MatchScraperService {
    List<Match> scrapeMatchesOfToday();
}
