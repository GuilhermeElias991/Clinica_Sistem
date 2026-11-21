package br.univille.odontocare.service;

/** Funções puras de CPF/telefone (mesma regra de src/utils.js do front). */
public final class Documentos {

    private Documentos() {}

    public static String digitos(String v) {
        return v == null ? "" : v.replaceAll("\\D", "");
    }

    public static boolean cpfValido(String cpf) {
        String n = digitos(cpf);
        if (n.length() != 11 || n.chars().distinct().count() == 1) return false;
        return digitoVerificador(n, 9) == n.charAt(9) - '0'
                && digitoVerificador(n, 10) == n.charAt(10) - '0';
    }

    private static int digitoVerificador(String n, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (n.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int r = (soma * 10) % 11;
        return r == 10 ? 0 : r;
    }

    /** "52998224725" ou "529.982.247-25" → "529.982.247-25" (formato guardado no banco). */
    public static String formatarCpf(String cpf) {
        String n = digitos(cpf);
        if (n.length() != 11) return cpf;
        return n.substring(0, 3) + "." + n.substring(3, 6) + "." + n.substring(6, 9) + "-" + n.substring(9);
    }

    /** Formata 10 ou 11 dígitos como (47) 99999-9999 / (47) 3333-4444. */
    public static String formatarTelefone(String tel) {
        String n = digitos(tel);
        if (n.length() == 11) return "(" + n.substring(0, 2) + ") " + n.substring(2, 7) + "-" + n.substring(7);
        if (n.length() == 10) return "(" + n.substring(0, 2) + ") " + n.substring(2, 6) + "-" + n.substring(6);
        return tel == null ? null : tel.trim();
    }

    public static String vazioParaNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
