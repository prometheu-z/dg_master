package projeto.bdd2.dgmaster.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CadastroRequest(
        @NotBlank(message = "CPF obrigatório")
        @Pattern(regexp = "\\d{11}", message = "CPF deve conter 11 dígitos")
        String cpf,
        @NotBlank(message = "Nome obrigatório")
        String nome,
        @NotBlank(message = "Email obrigatório")
        @Email(message = "Email inválido")
        String email,
        @NotBlank(message = "Senha obrigatória")
        String senha,
        @NotBlank(message = "Data de nascimento obrigatória")
        String dataNascimento
) {
}
