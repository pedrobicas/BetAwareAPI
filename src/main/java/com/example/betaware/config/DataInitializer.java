package com.example.betaware.config;

import com.example.betaware.model.Aposta;
import com.example.betaware.model.Usuario;
import com.example.betaware.model.enums.Perfil;
import com.example.betaware.model.enums.ResultadoAposta;
import com.example.betaware.repository.ApostaRepository;
import com.example.betaware.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ApostaRepository apostaRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Bean
    public CommandLineRunner loadData() {
        return args -> {
            try {
                if (usuarioRepository.count() > 0) {
                    System.out.println("Database already contains data. Skipping initialization.");
                    return;
                }

        Usuario admin = Usuario.builder()
                .username("admin")
                .nome("Administrador")
                .cpf("12345678901")
                .cep("12345678")
                .endereco("Rua Exemplo, 123")
                .senha(passwordEncoder.encode("senha123"))
                .email("admin@betaware.com")
                .perfil(Perfil.ADMIN)
                .build();
        
        Usuario user = Usuario.builder()
                .username("usuario1")
                .nome("Usuário Teste")
                .cpf("98765432109")
                .cep("87654321")
                .endereco("Av Principal, 456")
                .senha(passwordEncoder.encode("senha123"))
                .email("usuario@betaware.com")
                .perfil(Perfil.USER)
                .build();
                
        usuarioRepository.save(admin);
        usuarioRepository.save(user);

        Aposta aposta1 = Aposta.builder()
                .categoria("Futebol")
                .jogo("Flamengo x Vasco")
                .valor(100.0)
                .resultado(ResultadoAposta.GANHOU)
                .data(LocalDateTime.now().minusDays(3))
                .usuario(user)
                .build();
                
        Aposta aposta2 = Aposta.builder()
                .categoria("Basquete")
                .jogo("Lakers x Bulls")
                .valor(50.0)
                .resultado(ResultadoAposta.PERDEU)
                .data(LocalDateTime.now().minusDays(2))
                .usuario(user)
                .build();
                
        Aposta aposta3 = Aposta.builder()
                .categoria("Futebol")
                .jogo("Brasil x Argentina")
                .valor(200.0)
                .resultado(ResultadoAposta.PENDENTE)
                .data(LocalDateTime.now().plusDays(2))
                .usuario(user)
                .build();
                
        apostaRepository.save(aposta1);
        apostaRepository.save(aposta2);
        apostaRepository.save(aposta3);
        
        System.out.println("*** INITIALIZATION COMPLETED SUCCESSFULLY ***");
        System.out.println("Created " + usuarioRepository.count() + " users and " + apostaRepository.count() + " bets.");
            } catch (Exception e) {
                System.err.println("Error during database initialization: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}
