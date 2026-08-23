-- Contas comuns só podem ficar ATIVAS depois da aprovação, que cria o perfil
-- de jogador e concede o respectivo papel. Corrige estados órfãos produzidos
-- pela alteração direta de status no painel administrativo.
UPDATE usuario u
SET status = 'PENDENTE',
    atualizado_em = CURRENT_TIMESTAMP
WHERE u.status = 'ATIVO'
  AND NOT EXISTS (
      SELECT 1 FROM jogador j WHERE j.usuario_id = u.id
  )
  AND NOT EXISTS (
      SELECT 1 FROM usuario_papel up WHERE up.usuario_id = u.id
  );
