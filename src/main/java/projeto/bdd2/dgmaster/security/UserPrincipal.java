package projeto.bdd2.dgmaster.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {

    private final String email;
    private final String senha;
    private final String nome;
    private final String cpf;
    private final String tipo;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(String email, String senha, String nome, String cpf, String tipo) {
        this.email = email;
        this.senha = senha;
        this.nome = nome;
        this.cpf = cpf;
        this.tipo = tipo;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + tipo));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
