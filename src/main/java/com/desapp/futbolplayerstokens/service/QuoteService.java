package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.controller.dto.QuoteDTO;
import com.desapp.futbolplayerstokens.modelo.QuoteTrigger;

import java.util.List;

public interface QuoteService {
    List<QuoteDTO> getQuotesByPlayerId(Long playerId);

    QuoteDTO getCurrentQuote(Long playerId);

    void recalculateAll(QuoteTrigger trigger);

    void recalculatePlayers(List<Long> playerIds, QuoteTrigger trigger);
}

