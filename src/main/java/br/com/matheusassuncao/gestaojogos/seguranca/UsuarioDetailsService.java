package br.com.matheusassuncao.gestaojogos.seguranca;

import br.com.matheusassuncao.gestaojogos.dominio.StatusUsuario;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Ponte entre o modelo de domínio e o Spring Security: carrega o usuário
 * pelo e-mail e traduz seus papéis em authorities.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário não encontrado para o e-mail informado."
                ));

        return User.withUsername(usuario.getEmail())
                .password(usuario.getSenhaHash())
                .authorities(mapearAuthorities(usuario))
                // conta pendente ainda não foi aprovada: autentica, mas sem papéis
                .disabled(usuario.getStatus() == StatusUsuario.INATIVO)
                .accountLocked(usuario.getStatus() == StatusUsuario.BLOQUEADO)
                .build();
    }

    private List<GrantedAuthority> mapearAuthorities(Usuario usuario) {
        return usuario.getPapeis().stream()
                .map(papel -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + papel.getNome()))
                .toList();
    }
}
