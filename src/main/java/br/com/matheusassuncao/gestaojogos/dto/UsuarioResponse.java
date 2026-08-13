package br.com.matheusassuncao.gestaojogos.dto;

import br.com.matheusassuncao.gestaojogos.dominio.Papel;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Nunca expõe o hash da senha: o DTO existe justamente para controlar
 * o que sai da API.
 */
public record UsuarioResponse(
        UUID id,
        String nome,
        String email,
        String status,
        Set<String> papeis,
        boolean senhaProvisoria
) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getStatus().name(),
                usuario.getPapeis().stream()
                        .map(Papel::getNome)
                        .collect(Collectors.toSet()),
                usuario.isSenhaProvisoria()
        );
    }
}
