# Sistema de Gestão de Jogos

Sistema web para gestão de partidas esportivas de um clube. Substitui o controle manual das partidas, centralizando o agendamento, as inscrições dos jogadores, a formação de equipes e o controle disciplinar, começando pelo futebol e preparado para receber novas modalidades (baralho, damas, sinuca) sem mudanças estruturais.

> Projeto em desenvolvimento incremental, com releases versionadas por marco. Acompanhe pelas [tags](../../tags) e [milestones](../../milestones).

## O problema

O clube gerencia suas partidas manualmente, o que dificulta a organização dos jogos, o controle dos participantes e a administração das atividades. As partidas de futebol, com duas equipes de 8 jogadores de linha identificadas pelas cores **Azul** e **Amarelo**, serviram como modelo para o levantamento de requisitos.

## Roadmap

| Marco | Versão | Escopo | Status |
|-------|--------|--------|--------|
| 0 — Fundação | `v0.0.1` | Setup do projeto, documentação e CI | 🔨 Em andamento |
| 1 — MVP | `v1.0.0` | Cadastro, login, partidas, calendário e inscrições com limite de vagas | Planejado |
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

## Documentação

- [Levantamento de requisitos](docs/requisitos.md), regras de negócio e casos de uso
- [Design do MVP](docs/design-mvp.md), fluxos de inscrição, modelo de dados e decisões técnicas

## Como rodar

*Instruções disponíveis após o setup inicial do projeto (Marco 0).*

## Licença

Distribuído sob a licença MIT. Veja [LICENSE](LICENSE) para mais informações.
