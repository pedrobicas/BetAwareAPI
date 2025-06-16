package com.example.betaware.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "O username é obrigatório")
    private String username;
    
    @NotBlank(message = "O nome é obrigatório")
    private String nome;
    
    @NotBlank(message = "O CPF é obrigatório")
    @Pattern(regexp = "^\\d{11}$", message = "CPF deve conter 11 dígitos")
    private String cpf;
    
    @NotBlank(message = "O CEP é obrigatório")
    @Pattern(regexp = "^\\d{8}$", message = "CEP deve conter 8 dígitos")
    private String cep;
    
    private String endereco;
    
    @NotBlank(message = "A senha é obrigatória")
    private String senha;
    
    @NotBlank(message = "O email é obrigatório")
    @Email(message = "Email inválido")
    private String email;


    public @NotBlank(message = "O username é obrigatório") String getUsername() {
        return username;
    }

    public void setUsername(@NotBlank(message = "O username é obrigatório") String username) {
        this.username = username;
    }

    public @NotBlank(message = "O nome é obrigatório") String getNome() {
        return nome;
    }

    public void setNome(@NotBlank(message = "O nome é obrigatório") String nome) {
        this.nome = nome;
    }

    public @NotBlank(message = "O CPF é obrigatório") @Pattern(regexp = "^\\d{11}$", message = "CPF deve conter 11 dígitos") String getCpf() {
        return cpf;
    }

    public void setCpf(@NotBlank(message = "O CPF é obrigatório") @Pattern(regexp = "^\\d{11}$", message = "CPF deve conter 11 dígitos") String cpf) {
        this.cpf = cpf;
    }

    public @NotBlank(message = "O CEP é obrigatório") @Pattern(regexp = "^\\d{8}$", message = "CEP deve conter 8 dígitos") String getCep() {
        return cep;
    }

    public void setCep(@NotBlank(message = "O CEP é obrigatório") @Pattern(regexp = "^\\d{8}$", message = "CEP deve conter 8 dígitos") String cep) {
        this.cep = cep;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public @NotBlank(message = "A senha é obrigatória") String getSenha() {
        return senha;
    }

    public void setSenha(@NotBlank(message = "A senha é obrigatória") String senha) {
        this.senha = senha;
    }

    public @NotBlank(message = "O email é obrigatório") @Email(message = "Email inválido") String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank(message = "O email é obrigatório") @Email(message = "Email inválido") String email) {
        this.email = email;
    }
}