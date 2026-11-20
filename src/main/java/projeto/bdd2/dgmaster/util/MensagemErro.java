package projeto.bdd2.dgmaster.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extrai a mensagem de negócio de exceções JDBC/JPA disparadas por
 * {@code SIGNAL SQLSTATE '45000'} nas triggers/procedures (RFC-003, ERR-01).
 */
public final class MensagemErro {

    private static final Logger log = LoggerFactory.getLogger(MensagemErro.class);

    private MensagemErro() {
    }

    public static String extrair(Throwable throwable) {
        Throwable atual = throwable;
        String mensagem = null;
        while (atual != null) {
            if (atual.getMessage() != null && !atual.getMessage().isBlank()) {
                mensagem = atual.getMessage();
            }
            atual = atual.getCause();
        }
        if (mensagem == null) {
            log.warn("Falha sem mensagem", throwable);
            return "Não foi possível concluir a operação.";
        }
        return mensagem.trim();
    }
}
