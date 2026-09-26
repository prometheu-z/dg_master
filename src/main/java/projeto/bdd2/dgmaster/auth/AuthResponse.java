package projeto.bdd2.dgmaster.auth;

public record AuthResponse(String tipo, String cpf, String nome, String email, boolean autenticado) {
}
