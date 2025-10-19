package com.example.betaware.service;

import com.example.betaware.dto.ApostaDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface IApostaService {
    ApostaDTO criarAposta(ApostaDTO apostaDTO, String username);
    List<ApostaDTO> listarApostasPorUsuario(String username);
    List<ApostaDTO> listarApostasPorPeriodo(LocalDateTime inicio, LocalDateTime fim);
    List<ApostaDTO> listarApostasPorUsuarioEPeriodo(String username, LocalDateTime inicio, LocalDateTime fim);
}