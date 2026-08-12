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

### Calendário: o fuso é o do clube, não o de quem faz a requisição

O clube funciona em dias e horários fixos (RN06) e fecha em feriados e recessos (RN07). Essas regras são inerentemente locais: "partida na segunda às 19h" significa 19h no horário de Brasília, independentemente de onde o cliente que faz a requisição está.

Por isso o `CalendarioService` converte todo `OffsetDateTime` recebido para `America/Sao_Paulo` antes de comparar com os dias e horários configurados. Sem essa conversão, um cliente enviando a data em UTC faria a validação comparar 22h com 19h e rejeitar um agendamento legítimo — um bug sutil, que só costuma aparecer em produção, quando o fuso do cliente diverge do fuso do servidor.

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
