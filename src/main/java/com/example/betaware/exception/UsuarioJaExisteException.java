package com.example.betaware.exception;

public class UsuarioJaExisteException extends NegocioException {
    public UsuarioJaExisteException(String mensagem) {
        super(mensagem);
    }
}
