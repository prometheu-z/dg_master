package projeto.bdd2.dgmaster.auth;

import java.util.regex.Pattern;

/**
 * Regra de força de senha definida na HU01/CA01:
 * mínimo de 8 caracteres, contendo letras, números e ao menos um caractere especial.
 *
 * Centralizada aqui para ser reaproveitada tanto pelas anotações de Bean Validation
 * quanto pelo fluxo interno (formulários Thymeleaf), que chamam o controller
 * diretamente e, portanto, não disparam a validação automática.
 */
public final class ValidacaoSenha {

    public static final String REGEX = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$";

    public static final String MENSAGEM =
            "Senha deve ter ao menos 8 caracteres, incluindo letras, números e um caractere especial";

    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private ValidacaoSenha() {
    }

    public static boolean valida(String senha) {
        return senha != null && PATTERN.matcher(senha).matches();
    }

    public static void validar(String senha) {
        if (!valida(senha)) {
            throw new IllegalArgumentException(MENSAGEM);
        }
    }
}
