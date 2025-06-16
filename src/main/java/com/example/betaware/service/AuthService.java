package com.example.betaware.service;

import com.example.betaware.dto.JwtResponse;
import com.example.betaware.dto.LoginRequest;
import com.example.betaware.dto.RegisterRequest;
import com.example.betaware.model.Usuario;
import com.example.betaware.model.enums.Perfil;
import com.example.betaware.repository.UsuarioRepository;
import com.example.betaware.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtTokenProvider tokenProvider;

    public JwtResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getSenha())
        );

        Usuario usuario = (Usuario) authentication.getPrincipal();
        String token = tokenProvider.generateToken(authentication);

        JwtResponse jwtResponse = new JwtResponse();
        jwtResponse.setNome(usuario.getNome());
        jwtResponse.setToken(token);
        jwtResponse.setUsername(usuario.getUsername());
        jwtResponse.setPerfil(usuario.getPerfil().name());
        return jwtResponse;
    }

    @Transactional
    public void register(RegisterRequest registerRequest) {
        if (usuarioRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Username já está em uso");
        }

        if (usuarioRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email já está em uso");
        }

        if (usuarioRepository.existsByCpf(registerRequest.getCpf())) {
            throw new RuntimeException("CPF já está em uso");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(registerRequest.getUsername());
        usuario.setNome(registerRequest.getNome());
        usuario.setCpf(registerRequest.getCpf());
        usuario.setCep(registerRequest.getCep());
        usuario.setEndereco(registerRequest.getEndereco());
        usuario.setSenha(passwordEncoder.encode(registerRequest.getSenha()));
        usuario.setEmail(registerRequest.getEmail());
        usuario.setPerfil(Perfil.USER);

        usuarioRepository.save(usuario);
    }
} 