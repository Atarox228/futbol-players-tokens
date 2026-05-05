package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.controller.dto.ValuationContext;
import com.desapp.futbolplayerstokens.controller.dto.ValuationResult;

public interface Strategy {
    ValuationResult evaluate(ValuationContext valuationContext);
}

