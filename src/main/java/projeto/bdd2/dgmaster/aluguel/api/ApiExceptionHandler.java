package projeto.bdd2.dgmaster.aluguel.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import projeto.bdd2.dgmaster.util.MensagemErro;

import java.sql.SQLException;
import java.util.Map;

/**
 * ERR-01: traduz {@code SIGNAL SQLSTATE '45000'} e violações de constraint
 * em respostas 400 com a mensagem de negócio vinda do MySQL.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("mensagem", exception.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException exception) {
        String mensagem = exception.getMessage() == null ? "Acesso negado" : exception.getMessage();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("mensagem", mensagem));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensagem", exception.getMessage()));
    }

    @ExceptionHandler({DataAccessException.class, SQLException.class})
    public ResponseEntity<Map<String, String>> handleDatabaseError(Exception exception) {
        String mensagem = MensagemErro.extrair(exception);
        log.warn("Erro de regra de negócio no banco: {}", mensagem);
        return ResponseEntity.badRequest().body(Map.of("mensagem", mensagem));
    }
}
