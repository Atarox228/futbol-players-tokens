package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.QuoteDTO;
import com.desapp.futbolplayerstokens.modelo.QuoteTrigger;
import com.desapp.futbolplayerstokens.service.QuoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quotes")
public class QuoteControllerREST {

    private final QuoteService quoteService;

    public QuoteControllerREST(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @PostMapping("/recalculate")
    public ResponseEntity<String> recalculateAll() {
        quoteService.recalculateAll(QuoteTrigger.MANUAL);
        return ResponseEntity.ok("Recalculation triggered for all players");
    }

    @GetMapping("/player/{id:[0-9]+}/current")
    public ResponseEntity<QuoteDTO> getCurrentQuote(@PathVariable Long id) {
        QuoteDTO dto = quoteService.getCurrentQuote(id);
        return ResponseEntity.ok(dto);
    }
}

