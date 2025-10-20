package com.example.betaware.dto;

import com.example.betaware.model.enums.ResultadoAposta;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NovaApostaRequest {
    
    @NotBlank(message = "A categoria é obrigatória")
    private String categoria;
    
    @NotBlank(message = "O jogo é obrigatório")
    private String jogo;
    
    @NotNull(message = "O valor é obrigatório")
    @Positive(message = "O valor deve ser positivo")
    private Double valor;
    
    @NotNull(message = "O resultado é obrigatório")
    private ResultadoAposta resultado;
    
    @NotNull(message = "A data é obrigatória")
    private LocalDateTime data;

    public @NotBlank(message = "A categoria é obrigatória") String getCategoria() {
        return categoria;
    }

    public void setCategoria(@NotBlank(message = "A categoria é obrigatória") String categoria) {
        this.categoria = categoria;
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
        return resultado;
    }

    public void setResultado(@NotNull(message = "O resultado é obrigatório") ResultadoAposta resultado) {
        this.resultado = resultado;
    }

    public @NotNull(message = "A data é obrigatória") LocalDateTime getData() {
        return data;
    }

    public void setData(@NotNull(message = "A data é obrigatória") LocalDateTime data) {
        this.data = data;
    }
}