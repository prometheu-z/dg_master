package projeto.bdd2.dgmaster.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GerenteRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 3, max = 150, message = "Nome deve ter entre 3 e 150 caracteres")
        String nome,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Pattern(regexp = ValidacaoSenha.REGEX, message = ValidacaoSenha.MENSAGEM)
        String senha,

        // Chave de administrador exigida para criação de gerentes.
        // Não é validada por Bean Validation: é conferida contra a configuração
        // (dgmaster.admin.chave-cadastro-gerente / DGMASTER_ADMIN_KEY).
        String chaveAdmin
) {
}
