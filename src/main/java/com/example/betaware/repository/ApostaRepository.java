package com.example.betaware.repository;

import com.example.betaware.model.Aposta;
import com.example.betaware.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApostaRepository extends JpaRepository<Aposta, Long> {
    List<Aposta> findByUsuario(Usuario usuario);
    List<Aposta> findByUsuarioAndDataBetween(Usuario usuario, LocalDateTime inicio, LocalDateTime fim);
    List<Aposta> findByDataBetween(LocalDateTime inicio, LocalDateTime fim);
} 