package br.com.matheusassuncao.gestaojogos.servico;

import br.com.matheusassuncao.gestaojogos.dominio.Papel;
import br.com.matheusassuncao.gestaojogos.dominio.StatusUsuario;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.dto.AdminCriarUsuarioRequest;
import br.com.matheusassuncao.gestaojogos.excecao.RegraNegocioException;
import br.com.matheusassuncao.gestaojogos.repositorio.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminCriarUsuarioTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PapelRepository papeis = mock(PapelRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AdminUsuarioService service = new AdminUsuarioService(usuarios,
            mock(JogadorRepository.class), mock(InscricaoRepository.class), papeis, encoder);

    @Test
    void criaContaAtivaComSenhaCodificadaEExigeTroca() {
        Papel papel = mock(Papel.class);
        when(papel.getNome()).thenReturn("ORGANIZADOR");
        when(papeis.findByNome("ORGANIZADOR")).thenReturn(Optional.of(papel));
        when(encoder.encode("Senha123!")).thenReturn("hash-seguro");
        when(usuarios.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        Usuario criado = service.criar(new AdminCriarUsuarioRequest(" Maria ", " MARIA@EXEMPLO.COM ",
                "Senha123!", Set.of("ORGANIZADOR")), "admin@exemplo.com");
        assertEquals("Maria", criado.getNome());
        assertEquals("maria@exemplo.com", criado.getEmail());
        assertEquals("hash-seguro", criado.getSenhaHash());
        assertEquals(StatusUsuario.ATIVO, criado.getStatus());
        assertTrue(criado.isSenhaProvisoria());
        assertTrue(criado.possuiPapel("ORGANIZADOR"));
    }

    @Test
    void rejeitaEmailDuplicadoAntesDeSalvar() {
        when(usuarios.existsByEmailIgnoreCase("maria@exemplo.com")).thenReturn(true);
        assertThrows(RegraNegocioException.class, () -> service.criar(new AdminCriarUsuarioRequest(
                "Maria", "MARIA@EXEMPLO.COM", "Senha123!", Set.of("ARBITRO")), "admin@exemplo.com"));
        verify(usuarios, never()).save(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void rejeitaPerfilDeJogadorSemFluxoDeAprovacao() {
        assertThrows(RegraNegocioException.class, () -> service.criar(new AdminCriarUsuarioRequest(
                "Maria", "maria@exemplo.com", "Senha123!", Set.of("JOGADOR")), "admin@exemplo.com"));
        verify(usuarios, never()).save(any());
    }

    @Test
    void exigeAoMenosUmPerfil() {
        assertThrows(RegraNegocioException.class, () -> service.criar(new AdminCriarUsuarioRequest(
                "Maria", "maria@exemplo.com", "Senha123!", Set.of()), "admin@exemplo.com"));
        verify(usuarios, never()).save(any());
    }
}
