package com.example.betaware.service;

import com.example.betaware.dto.ApostaDTO;
import com.example.betaware.exception.RecursoNaoEncontradoException;
import com.example.betaware.model.Aposta;
import com.example.betaware.model.Usuario;
import com.example.betaware.model.enums.ResultadoAposta;
import com.example.betaware.repository.ApostaRepository;
import com.example.betaware.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApostaServiceTest {

    @Mock
    private ApostaRepository apostaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ApostaService apostaService;

    private Usuario usuario;

    @BeforeEach
    void setup() {
        usuario = Usuario.builder()
                .id(1L)
                .username("usuario1")
                .nome("Usuário Teste")
                .email("usuario@betaware.com")
                .senha("encoded")
                .build();
    }

    @Test
    void criarAposta_deveSalvarEAtribuirUsuario() {
        ApostaDTO dto = new ApostaDTO();
        dto.setCategoria("Futebol");
        dto.setJogo("Time A x Time B");
        dto.setValor(150.0);
        dto.setResultado(ResultadoAposta.GANHOU);
        dto.setData(LocalDateTime.now());

        when(usuarioRepository.findByUsername("usuario1")).thenReturn(Optional.of(usuario));
        when(apostaRepository.save(any(Aposta.class))).thenAnswer(invocation -> {
            Aposta a = invocation.getArgument(0);
            a.setId(10L);
            return a;
        });

        ApostaDTO result = apostaService.criarAposta(dto, "usuario1");

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getUsername()).isEqualTo("usuario1");
        assertThat(result.getCategoria()).isEqualTo("Futebol");
        assertThat(result.getResultado()).isEqualTo(ResultadoAposta.GANHOU);
    }

    @Test
    void criarAposta_quandoUsuarioNaoExiste_deveLancarExcecao() {
        when(usuarioRepository.findByUsername("inexistente")).thenReturn(Optional.empty());
        ApostaDTO dto = new ApostaDTO();
        dto.setCategoria("Basquete");
        dto.setJogo("Lakers x Bulls");
        dto.setValor(50.0);
        dto.setResultado(ResultadoAposta.PERDEU);
        dto.setData(LocalDateTime.now());

        assertThatThrownBy(() -> apostaService.criarAposta(dto, "inexistente"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Usuário não encontrado");
    }

    @Test
    void listarApostasPorUsuario_deveConverterParaDTO() {
        when(usuarioRepository.findByUsername("usuario1")).thenReturn(Optional.of(usuario));

        Aposta aposta1 = Aposta.builder()
                .id(1L)
                .categoria("Futebol")
                .jogo("Jogo 1")
                .valor(100.0)
                .resultado(ResultadoAposta.GANHOU)
                .data(LocalDateTime.now())
                .usuario(usuario)
                .build();

        Aposta aposta2 = Aposta.builder()
                .id(2L)
                .categoria("Basquete")
                .jogo("Jogo 2")
                .valor(50.0)
                .resultado(ResultadoAposta.PERDEU)
                .data(LocalDateTime.now())
                .usuario(usuario)
                .build();

        when(apostaRepository.findByUsuario(usuario)).thenReturn(Arrays.asList(aposta1, aposta2));

        List<ApostaDTO> list = apostaService.listarApostasPorUsuario("usuario1");
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getUsername()).isEqualTo("usuario1");
        assertThat(list.get(1).getCategoria()).isEqualTo("Basquete");
    }

    @Test
    void listarApostasPorPeriodo_deveFiltrarPorData() {
        Aposta aposta = Aposta.builder()
                .id(3L)
                .categoria("Futebol")
                .jogo("Jogo 3")
                .valor(75.0)
                .resultado(ResultadoAposta.PENDENTE)
                .data(LocalDateTime.now())
                .usuario(usuario)
                .build();

        when(apostaRepository.findByDataBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(aposta));

        List<ApostaDTO> list = apostaService.listarApostasPorPeriodo(LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo(3L);
    }

    @Test
    void listarApostasPorUsuarioEPeriodo_deveFiltrarPorUsuarioEData() {
        when(usuarioRepository.findByUsername("usuario1")).thenReturn(Optional.of(usuario));

        Aposta aposta = Aposta.builder()
                .id(4L)
                .categoria("Futebol")
                .jogo("Jogo 4")
                .valor(120.0)
                .resultado(ResultadoAposta.CANCELADA)
                .data(LocalDateTime.now())
                .usuario(usuario)
                .build();

        when(apostaRepository.findByUsuarioAndDataBetween(any(Usuario.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(aposta));

        List<ApostaDTO> list = apostaService.listarApostasPorUsuarioEPeriodo("usuario1", LocalDateTime.now().minusDays(2), LocalDateTime.now().plusDays(2));
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getResultado()).isEqualTo(ResultadoAposta.CANCELADA);
    }
}