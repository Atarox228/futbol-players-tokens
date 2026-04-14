package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.controller.dto.PlayerDTO;

public interface PlayerService {
    PlayerDTO getPlayerById(Long id);
}
