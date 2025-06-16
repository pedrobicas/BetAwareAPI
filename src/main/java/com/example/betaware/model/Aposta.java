package com.example.betaware.model;

import com.example.betaware.model.enums.ResultadoAposta;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "apostas")
public class Aposta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String categoria;

    @NotBlank(message = "O jogo é obrigatório")
    @Column(nullable = false)
    private String jogo;

    @NotNull(message = "O valor é obrigatório")
    @Positive(message = "O valor deve ser positivo")
    @Column(nullable = false)
    private Double valor;

    @NotNull(message = "O resultado é obrigatório")
    @Column(nullable = false)
    private String resultado;

    @NotNull(message = "A data é obrigatória")
    @Column(nullable = false)
    private LocalDateTime data;

    @NotNull(message = "O usuário é obrigatório")
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotBlank(message = "O jogo é obrigatório") String getJogo() {
        return jogo;
    }

    public void setJogo(@NotBlank(message = "O jogo é obrigatório") String jogo) {
        this.jogo = jogo;
    }

    public @NotNull(message = "O valor é obrigatório") @Positive(message = "O valor deve ser positivo") Double getValor() {
        return valor;
    }

    public void setValor(@NotNull(message = "O valor é obrigatório") @Positive(message = "O valor deve ser positivo") Double valor) {
        this.valor = valor;
    }

    public @NotNull(message = "O resultado é obrigatório") ResultadoAposta getResultado() {
        return ResultadoAposta.valueOf(resultado);
    }

    public void setResultado(@NotNull(message = "O resultado é obrigatório") ResultadoAposta resultado) {
        this.resultado = resultado.name();
    }

    public @NotNull(message = "A data é obrigatória") LocalDateTime getData() {
        return data;
    }

    public void setData(@NotNull(message = "A data é obrigatória") LocalDateTime data) {
        this.data = data;
    }

    public @NotNull(message = "O usuário é obrigatório") Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(@NotNull(message = "O usuário é obrigatório") Usuario usuario) {
        this.usuario = usuario;
    }


}

