package projeto.bdd2.dgmaster.cliente.api;

import projeto.bdd2.dgmaster.entity.Cliente;

public record ClienteResponse(Integer idCliente, String cpf, String nome, String email, String telefone) {

    public static ClienteResponse from(Cliente cliente) {
        return new ClienteResponse(
                cliente.getIdCliente(),
                cliente.getCpf(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.getTelefone());
    }
}
