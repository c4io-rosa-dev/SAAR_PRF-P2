package br.edu.fatec.saarprf.service;

public class LinhaInvalidaException extends RuntimeException {

    public LinhaInvalidaException(String motivo) {
        super(motivo);
    }
}
