package com.degen.backend.controller;

import com.degen.backend.dto.HandicapCalculatorDto;
import com.degen.backend.service.HandicapCalculatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/handicap")
@CrossOrigin(origins = "http://localhost:4200")
public class HandicapCalculatorController {

    @Autowired
    private HandicapCalculatorService handicapCalculatorService;

    @GetMapping("/player/{playerId}")
    public HandicapCalculatorDto calculateHandicap(@PathVariable Long playerId) {
        return handicapCalculatorService.calculateHandicap(playerId);
    }
}
