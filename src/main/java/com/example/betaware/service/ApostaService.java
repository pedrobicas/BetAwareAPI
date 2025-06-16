package com.example.betaware.service;

import com.example.betaware.dto.ApostaDTO;
import com.example.betaware.model.Aposta;
import com.example.betaware.model.Usuario;
import com.example.betaware.repository.ApostaRepository;
import com.example.betaware.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApostaService {

    @Autowired
    private ApostaRepository apostaRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Transactional
    public ApostaDTO criarAposta(ApostaDTO apostaDTO, String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Aposta aposta = new Aposta();
        aposta.setCategoria(apostaDTO.getCategoria());
        aposta.setJogo(apostaDTO.getJogo());
        aposta.setValor(apostaDTO.getValor());
        aposta.setResultado(apostaDTO.getResultado());
        aposta.setData(apostaDTO.getData());
        aposta.setUsuario(usuario);

        aposta = apostaRepository.save(aposta);
        return converterParaDTO(aposta);
    }

    public List<ApostaDTO> listarApostasPorUsuario(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return apostaRepository.findByUsuario(usuario).stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    public List<ApostaDTO> listarApostasPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return apostaRepository.findByDataBetween(inicio, fim).stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    public List<ApostaDTO> listarApostasPorUsuarioEPeriodo(String username, LocalDateTime inicio, LocalDateTime fim) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return apostaRepository.findByUsuarioAndDataBetween(usuario, inicio, fim).stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    private ApostaDTO converterParaDTO(Aposta aposta) {
        ApostaDTO dto = new ApostaDTO();
        dto.setId(aposta.getId());
        dto.setCategoria(aposta.getCategoria());
        dto.setJogo(aposta.getJogo());
        dto.setValor(aposta.getValor());
        dto.setResultado(aposta.getResultado());
        dto.setData(aposta.getData());
        dto.setUsername(aposta.getUsuario().getUsername());
        return dto;
    }
} 