package projeto.bdd2.dgmaster.cliente.api;

import projeto.bdd2.dgmaster.entity.Cliente;

public record ClienteResponse(String cpf, String nome, String email, boolean statusConta) {

    public static ClienteResponse from(Cliente cliente) {
        return new ClienteResponse(cliente.getCpf(), cliente.getNome(), cliente.getEmail(), cliente.isStatusConta());
    }
}