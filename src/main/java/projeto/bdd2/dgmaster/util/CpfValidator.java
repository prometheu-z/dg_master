package projeto.bdd2.dgmaster.util;

/**
 * Sanitização e validação de CPF (RFC-003, CPF-01).
 * A constraint {@code ck_cliente_cpf} exige exatamente 11 dígitos.
 */
public final class CpfValidator {

    private CpfValidator() {
    }

    public static String sanitizar(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }

    public static boolean valido(String cpf) {
        String digitos = sanitizar(cpf);
        if (digitos == null || digitos.length() != 11 || digitos.chars().distinct().count() == 1) {
            return false;
        }

        int[] numeros = new int[11];
        for (int i = 0; i < 11; i++) {
            numeros[i] = digitos.charAt(i) - '0';
        }

        int soma = 0;
        for (int i = 0; i < 9; i++) {
            soma += numeros[i] * (10 - i);
        }
        int primeiro = 11 - (soma % 11);
        if (primeiro >= 10) {
            primeiro = 0;
        }
        if (numeros[9] != primeiro) {
            return false;
        }

        soma = 0;
        for (int i = 0; i < 10; i++) {
            soma += numeros[i] * (11 - i);
        }
        int segundo = 11 - (soma % 11);
        if (segundo >= 10) {
            segundo = 0;
        }
        return numeros[10] == segundo;
    }

    public static void validar(String cpf) {
        if (!valido(cpf)) {
            throw new IllegalArgumentException("CPF inválido. Informe 11 dígitos com dígitos verificadores corretos.");
        }
    }
}
