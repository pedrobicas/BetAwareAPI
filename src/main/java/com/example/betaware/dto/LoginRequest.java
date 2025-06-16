package com.example.betaware.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {
    @NotBlank(message = "O username é obrigatório")
    private String username;
    
    @NotBlank(message = "A senha é obrigatória")
    private String senha;

    public @NotBlank(message = "O username é obrigatório") String getUsername() {
        return username;
    }

    public void setUsername(@NotBlank(message = "O username é obrigatório") String username) {
        this.username = username;
    }

    public @NotBlank(message = "A senha é obrigatória") String getSenha() {
        return senha;
    }

    public void setSenha(@NotBlank(message = "A senha é obrigatória") String senha) {
        this.senha = senha;
    }
}