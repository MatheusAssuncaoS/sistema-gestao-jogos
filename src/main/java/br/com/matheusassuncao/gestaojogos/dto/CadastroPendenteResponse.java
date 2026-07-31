package br.com.matheusassuncao.gestaojogos.dto;

import br.com.matheusassuncao.gestaojogos.dominio.Usuario;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CadastroPendenteResponse(
        UUID usuarioId,
        String nome,
        String email,
        OffsetDateTime cadastradoEm
) {

    public static CadastroPendenteResponse de(Usuario usuario) {
        return new CadastroPendenteResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getCriadoEm()
        );
    }
}
