package com.example.betaware.service;

import com.example.betaware.dto.JwtResponse;
import com.example.betaware.dto.LoginRequest;
import com.example.betaware.dto.RegisterRequest;
import com.example.betaware.exception.UsuarioJaExisteException;
import com.example.betaware.model.Usuario;
import com.example.betaware.model.enums.Perfil;
import com.example.betaware.repository.UsuarioRepository;
import com.example.betaware.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_deveRetornarJwtResponse() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .username("usuario1")
                .nome("Usuário Teste")
                .perfil(Perfil.USER)
                .build();

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                usuario,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(eq(authentication))).thenReturn("fakeToken");

        LoginRequest login = new LoginRequest();
        login.setUsername("usuario1");
        login.setSenha("senha123");

        JwtResponse response = authService.login(login);

        assertThat(response.getToken()).isEqualTo("fakeToken");
        assertThat(response.getUsername()).isEqualTo("usuario1");
        assertThat(response.getPerfil()).isEqualTo("USER");
    }

    @Test
    void register_quandoUsernameExiste_deveLancarExcecao() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("usuario1");
        req.setEmail("email@teste.com");
        req.setCpf("12345678900");
        req.setNome("Nome Teste");
        req.setCep("12345678");
        req.setEndereco("Rua X");
        req.setSenha("senha123");

        when(usuarioRepository.existsByUsername("usuario1")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(UsuarioJaExisteException.class)
                .hasMessageContaining("Username já está em uso");
    }

    @Test
    void register_deveSalvarComSenhaCriptografadaEPerfilUser() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("novoUsuario");
        req.setEmail("novo@teste.com");
        req.setCpf("12345678900");
        req.setNome("Nome Teste");
        req.setCep("12345678");
        req.setEndereco("Rua X");
        req.setSenha("senha123");

        when(usuarioRepository.existsByUsername("novoUsuario")).thenReturn(false);
        when(usuarioRepository.existsByEmail("novo@teste.com")).thenReturn(false);
        when(usuarioRepository.existsByCpf("12345678900")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("ENCODED");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(req);

        verify(usuarioRepository).save(captor.capture());

        Usuario saved = captor.getValue();
        assertThat(saved.getSenha()).isEqualTo("ENCODED");
        assertThat(saved.getPerfil()).isEqualTo(Perfil.USER);
        assertThat(saved.getUsername()).isEqualTo("novoUsuario");
    }
}