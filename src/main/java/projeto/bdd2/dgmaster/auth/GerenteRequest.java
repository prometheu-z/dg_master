package projeto.bdd2.dgmaster.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GerenteRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
        String nome,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Pattern(regexp = ValidacaoSenha.REGEX, message = ValidacaoSenha.MENSAGEM)
        String senha,

        @NotBlank(message = "Matrícula é obrigatória")
        @Size(max = 20, message = "Matrícula deve ter no máximo 20 caracteres")
        String matricula,

        // Chave de administrador exigida para criação de gerentes.
        String chaveAdmin) {
}
