package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.controller.dto.QuoteDTO;

import java.util.List;

public interface QuoteService {
    List<QuoteDTO> getQuotesByPlayerId(Long playerId);
}

