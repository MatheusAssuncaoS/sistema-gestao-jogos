package br.com.matheusassuncao.gestaojogos.dto;

import br.com.matheusassuncao.gestaojogos.dominio.Categoria;

public record CategoriaResponse(
        Long id,
        String nome,
        Integer peso
) {

    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.getPeso());
    }
}
