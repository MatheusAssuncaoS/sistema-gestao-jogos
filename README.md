# Sistema de Gestão de Jogos

![CI](https://github.com/MatheusAssuncaoS/sistema-gestao-jogos/actions/workflows/ci.yml/badge.svg)

Sistema web para gestão de partidas esportivas de um clube. Substitui o controle manual das partidas, centralizando o agendamento, as inscrições dos jogadores, a formação de equipes e o controle disciplinar, começando pelo futebol e preparado para receber novas modalidades (baralho, damas, sinuca) sem mudanças estruturais.

> Projeto em desenvolvimento incremental, com releases versionadas por marco. Acompanhe pelas [tags](../../tags) e [milestones](../../milestones).

## O problema

O clube gerencia suas partidas manualmente, o que dificulta a organização dos jogos, o controle dos participantes e a administração das atividades. As partidas de futebol, com duas equipes de 8 jogadores de linha identificadas pelas cores **Azul** e **Amarelo**, serviram como modelo para o levantamento de requisitos.

## Roadmap

| Marco | Versão | Escopo | Status |
|-------|--------|--------|--------|
| 0 — Fundação | `v0.0.1` | Setup do projeto, documentação e CI | ✅ Concluído |
| 1 — MVP | `v1.0.0` | Cadastro, login, partidas, calendário e inscrições com limite de vagas | ✅ Concluído |
| 2 — Regras ricas | `v2.0.0` | Lista de espera, séries A/B/C, escalação e controle disciplinar | Planejado |
| 3 — Operação | `v3.0.0` | Presença, resultados, relatórios e novas modalidades | Planejado |

## Stack

- **Java 25** + **Spring Boot 4**
- **PostgreSQL** + **Flyway** (migrations versionadas)
- **Docker Compose** para o ambiente local
- **GitHub Actions** para integração contínua

## Decisões técnicas

### Concorrência: dois locks, dois problemas diferentes

O ponto mais sensível do sistema é a disputa pela última vaga de uma partida: dois jogadores clicando em "inscrever-se" no mesmo instante não podem resultar em 17 confirmados. A contagem de vagas e a criação da inscrição precisam ser atômicas, e isso é garantido por um lock pessimista (`SELECT ... FOR UPDATE` na linha da partida), que serializa as inscrições concorrentes: quem chega segundo espera alguns milissegundos e já enxerga a contagem atualizada.

Já a edição de uma partida pelo organizador tem o perfil oposto: a operação dura minutos (um humano com o formulário aberto) e conflitos são raros. Segurar um lock de banco nesse cenário travaria as inscrições da partida inteira. Por isso a edição usa lock otimista (campo `versao`, via `@Version` do JPA): ninguém trava nada, e se duas edições colidirem, a segunda recebe um aviso para recarregar e tentar de novo.

| | Inscrição e cancelamento | Edição pelo organizador |
|---|---|---|
| Estratégia | Lock pessimista (`FOR UPDATE`) | Lock otimista (`versao` / `@Version`) |
| Duração da operação | Milissegundos, automática | Minutos, com formulário aberto |
| Contenção esperada | Alta em picos de inscrição | Rara |
| Custo de um conflito | 17º confirmado (inconsistência de dados) | Retrabalho de digitação |

A regra geral: pessimista para operações curtas e disputadas, onde errar corrompe dados; otimista para operações longas e raras, onde errar custa apenas um retry. Os fluxos completos estão em [docs/design-mvp.md](docs/design-mvp.md).

### Autenticação: sessão agora, JWT depois

O MVP autentica por sessão com cookie, o padrão do Spring Security. A escolha foi deliberada e tem prazo de validade: sessão exige menos código, entrega o fluxo completo mais rápido e mantém o logout realmente invalidando o acesso no servidor, o que é suficiente para o volume de um clube.

O custo aparece em dois pontos. Primeiro, o estado da sessão vive na memória da aplicação, o que complica rodar várias instâncias sem um armazenamento compartilhado. Segundo, como o navegador envia o cookie automaticamente, a proteção contra CSRF passa a ser necessária, e ela está desabilitada aqui para simplificar o consumo da API por clientes REST. Essa é uma dívida técnica consciente, registrada em comentário no `SecurityConfig`.

A migração planejada para JWT stateless resolve os dois pontos de uma vez: nada de estado no servidor e nenhum header enviado automaticamente pelo navegador, o que torna o CSRF inaplicável. O trade-off que ela traz é o logout, já que um token continua válido até expirar a menos que se mantenha uma lista de revogação.

### Calendário geral do clube

O período, os dias da semana e o horário são definidos diretamente em Nova Partida (RN06). Configurações contém somente exceções; não é necessário cadastrar funcionamento ou agendas por modalidade. As agendas antigas permanecem armazenadas para histórico, mas não determinam a disponibilidade e não aceitam novos cadastros ou edições.

As exceções (RN07) bloqueiam dias inteiros, incluindo as datas inicial e final: feriados, recessos/emendas e manutenção ou outros bloqueios. `localId` omitido ou nulo indica o clube inteiro; quando informado, bloqueia somente esse local. Exceções podem se sobrepor, e inativar uma delas não elimina os demais bloqueios. Partidas existentes são preservadas e devem ser revisadas pela equipe quando afetadas.

A consulta `GET /api/calendario/horarios-disponiveis?dias=730&localId=<uuid>` desconta as exceções gerais e as do local informado. Sem `localId`, retorna os horários gerais, descontando somente os bloqueios do clube inteiro. Esse endpoint permanece como sugestão legada; não limita a criação. A tela Nova Partida consulta `GET /api/calendario/excecoes` e gera as datas escolhidas, descontando bloqueios e horários ocupados. A criação e a edição validam as exceções do local escolhido no servidor.

A migration V16 adiciona o local opcional às exceções existentes, que continuam globais. Os antigos registros de funcionamento são preservados, mas deixam de restringir a criação e edição de partidas.

O `CalendarioService` converte as datas recebidas para `America/Sao_Paulo`: uma partida na segunda às 19h significa 19h no horário do clube, independentemente do fuso do cliente.

## Documentação

- [Levantamento de requisitos](docs/requisitos.md), regras de negócio e casos de uso
- [Design do MVP](docs/design-mvp.md), fluxos de inscrição, modelo de dados e decisões técnicas

## Como rodar

Pré-requisitos: JDK 25, Docker e Docker Compose.

```bash
# sobe o PostgreSQL local
docker compose up -d

# inicia a aplicação
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. Para rodar os testes: `./mvnw verify`.

## Licença

Distribuído sob a licença MIT. Veja [LICENSE](LICENSE) para mais informações.

### Abertura automática de inscrições

Ao criar ou editar uma partida, informar o início das inscrições ativa a abertura automática: se o prazo já começou e ainda está vigente, a partida sai de rascunho para aberta imediatamente. Para datas futuras, o backend verifica as aberturas a cada 15 segundos, inclusive após reinicializações. A rotina não abre partidas canceladas, iniciadas ou com inscrições vencidas, e atualiza a versão para respeitar edições concorrentes. Sem início das inscrições, a abertura permanece manual. A arbitragem não é iniciada por essa rotina.

### Duração prevista

O cadastro individual e em lote permite definir `duracaoMinutos` (1 a 1440 minutos). A edição pode atualizar essa duração. Partidas existentes recebem 60 minutos na migration V17 e podem ser ajustadas. A disponibilidade considera o intervalo inteiro no local: o fim de uma partida pode coincidir com o início da próxima, sem sobreposição. Essa duração representa o tempo previsto de ocupação, sem alterar o cronômetro da arbitragem.

### Cancelamento e exclusão lógica de partidas

Partidas em rascunho podem ser marcadas como `EXCLUIDA` quando o cadastro foi feito por engano. O registro permanece no histórico e libera o horário do local. O cancelamento usa `CANCELADA`, pode ser aplicado a rascunhos, partidas abertas ou lotadas e cancela suas inscrições ativas. Partidas excluídas não aparecem na agenda da arbitragem nem nos indicadores operacionais.
