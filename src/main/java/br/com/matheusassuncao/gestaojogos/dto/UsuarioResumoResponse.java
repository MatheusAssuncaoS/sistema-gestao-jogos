package br.com.matheusassuncao.gestaojogos.dto;

import br.com.matheusassuncao.gestaojogos.dominio.Papel;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Visão administrativa de um usuário e seus papéis, para o administrador
 * decidir a quem conceder o papel de organizador (UC19).
 */
public record UsuarioResumoResponse(
        UUID id,
        String nome,
        String email,
        String status,
        Set<String> papeis
) {

    public static UsuarioResumoResponse de(Usuario usuario) {
        return new UsuarioResumoResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getStatus().name(),
                usuario.getPapeis().stream()
                        .map(Papel::getNome)
                        .collect(Collectors.toSet())
        );
    }
}
