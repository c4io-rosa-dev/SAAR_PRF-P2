package br.edu.fatec.saarprf.service;

import java.util.Locale;

public final class Normalizacao {

    private Normalizacao() {
    }

    public static String texto(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim().replaceAll("\\s+", " ");
        return limpo.isEmpty() ? null : limpo;
    }

    public static String maiusculo(String valor) {
        String limpo = texto(valor);
        return limpo == null ? null : limpo.toUpperCase(Locale.ROOT);
    }
}
