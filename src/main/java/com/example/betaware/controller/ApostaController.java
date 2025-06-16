package com.example.betaware.controller;

import com.example.betaware.dto.ApostaDTO;
import com.example.betaware.service.ApostaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/v1/apostas")
@Tag(name = "Apostas", description = "APIs de gerenciamento de apostas")
public class ApostaController {

    @Autowired
    private ApostaService apostaService;

    @PostMapping
    @Operation(summary = "Criar aposta", description = "Cria uma nova aposta para o usuário autenticado")
    public ResponseEntity<ApostaDTO> criarAposta(
            @Valid @RequestBody ApostaDTO apostaDTO,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(apostaService.criarAposta(apostaDTO, userDetails.getUsername()));
    }

    @GetMapping
    @Operation(summary = "Listar apostas", description = "Lista todas as apostas do usuário autenticado")
    public ResponseEntity<List<ApostaDTO>> listarApostas(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(apostaService.listarApostasPorUsuario(userDetails.getUsername()));
    }

    @GetMapping("/periodo")
    @Operation(summary = "Listar apostas por período", description = "Lista todas as apostas dentro de um período")
    public ResponseEntity<List<ApostaDTO>> listarApostasPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return ResponseEntity.ok(apostaService.listarApostasPorPeriodo(inicio, fim));
    }

    @GetMapping("/usuario/periodo")
    @Operation(summary = "Listar apostas do usuário por período", description = "Lista todas as apostas do usuário dentro de um período")
    public ResponseEntity<List<ApostaDTO>> listarApostasPorUsuarioEPeriodo(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return ResponseEntity.ok(apostaService.listarApostasPorUsuarioEPeriodo(userDetails.getUsername(), inicio, fim));
    }
} 