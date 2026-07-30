# Sistema de Gestão de Jogos - Levantamento de Requisitos

Uma empresa entrou em contato pois necessita de um sistema informatizado para gerenciar os jogos realizados em um Clube. Atualmente, todo o controle é realizado manualmente, o que dificulta a organização das partidas, o controle dos participantes e a administração das atividades.

A iniciativa para a digitalização partiu da necessidade de organizar as partidas de **futebol**, que serviram como modelo base para o mapeamento e levantamento dos requisitos.

O sistema deverá centralizar a gestão dos jogos oferecidos pelo clube, contemplando inicialmente a modalidade de futebol e, posteriormente, baralho, damas e sinuca, permitindo a inclusão de novas modalidades futuramente **sem alterações estruturais significativas**.

## 1. Gestão das partidas de futebol

As partidas de futebol são compostas por duas equipes de 8 (oito) jogadores de linha cada, totalizando 16 (dezesseis) participantes por partida (8 × 8).

Os jogadores são classificados conforme seu nível técnico em três categorias:

- **Série A**: jogadores mais experientes;
- **Série B**: jogadores de nível intermediário;
- **Série C**: jogadores iniciantes ou menos experientes.

As equipes são identificadas pelas cores **Azul** e **Amarelo**. Como os coletes utilizados são dupla face, o sistema deverá informar ao jogador a cor da sua equipe, após a publicação da escalação, para que ele utilize o lado correto do colete ao entrar em campo.

Os goleiros não fazem parte do limite de 16 jogadores. Sua participação ocorre por ordem de chegada ao campo, não sendo necessário agendamento pelo sistema.

Os árbitros são voluntários ou colaboradores fixos do clube e não participam do processo de agendamento.

### 1.1. Balanceamento das equipes

Para garantir partidas equilibradas, o sistema deverá permitir o balanceamento dos times. O balanceamento poderá ser realizado manualmente pelos organizadores, permitindo mover jogadores entre as equipes Azul e Amarela sempre que houver concentração excessiva de pessoas com maior nível técnico em um mesmo time.

### 1.2. Controle disciplinar

O sistema deverá permitir o registro de infrações disciplinares aplicadas aos jogadores. Cada infração possuirá um tipo previamente cadastrado, contendo, entre outras informações, o período de suspensão correspondente.

Exemplos:

- **Infração leve**: advertências previstas no regulamento;
- **Infração grave**: agressão física, podendo gerar suspensão entre 15 e 30 dias.

Enquanto estiver suspenso, o jogador ficará impedido de realizar novas inscrições em partidas. O sistema deverá permitir que os administradores personalizem os tipos de infração e seus respectivos períodos de suspensão.

### 1.3. Gestão do calendário

O sistema deverá permitir que administradores configurem:

- os dias de funcionamento das partidas (por exemplo, segundas, quartas e sextas-feiras);
- os horários disponíveis para cada dia;
- períodos de recesso;
- feriados e datas específicas em que não haverá partidas.

Essas configurações deverão refletir automaticamente no calendário disponibilizado aos jogadores.

## 2. Cadastro e acesso ao sistema

Os próprios associados deverão realizar seu cadastro, informando seus dados pessoais e criando uma conta de acesso. O cadastro passa por **aprovação de um administrador** (UC27), que valida o vínculo associativo antes de liberar as inscrições.

Após realizar o login com cadastro aprovado e situação associativa regular, o jogador poderá visualizar as partidas disponíveis e efetuar sua inscrição. Quando uma partida atingir o limite de 16 jogadores confirmados, novas inscrições passam a compor a lista de espera (RN04/RN12).

### 2.1. Perfis de usuários

- **Jogador (sócio):** realiza seu cadastro, efetua login, consulta partidas disponíveis e inscreve-se nas modalidades permitidas.
- **Organizador:** administração operacional dos jogos (formação de equipes, balanceamento, aplicação de punições e gerenciamento das partidas).
- **Administrador:** administração geral do sistema (usuários, aprovação de jogadores, modalidades, calendário, regras de funcionamento e configurações).

## 3. Regras de negócio

| Código | Regra de negócio |
|--------|------------------|
| RN01 | Qualquer pessoa pode criar conta; a inscrição em partidas exige cadastro aprovado e situação associativa regular. |
| RN02 | Cada partida de futebol possui no máximo 16 jogadores de linha confirmados. |
| RN03 | Os goleiros não contam para o limite de participantes. |
| RN04 | Ao atingir 16 confirmados, a partida deixa de aceitar inscrições diretas; novos interessados são direcionados à lista de espera (ver RN12). |
| RN05 | O sistema deve informar a cor do colete (Azul ou Amarelo) de cada jogador **após a publicação da escalação** pelo organizador. |
| RN06 | As partidas ocorrem apenas em dias configurados pelos administradores. |
| RN07 | Feriados e recessos impedem a criação de partidas. |
| RN08 | Jogadores suspensos não podem realizar inscrições. |
| RN09 | Os períodos de suspensão são parametrizáveis. |
| RN10 | Os organizadores podem reorganizar manualmente os times. |
| RN11 | O sistema deve permitir cadastrar novas modalidades esportivas sem alterações estruturais. |
| RN12 | Lista de espera: ao atingir o limite, o 17º jogador entra na fila, ordenada pela data da solicitação. Se alguém cancelar, o sistema promove o primeiro da fila automaticamente. |
| RN13 | Na promoção da lista de espera, o jogador é revalidado (suspensão vigente e situação associativa); se estiver impedido, o sistema passa ao próximo da fila. |
| RN14 | Um jogador não pode possuir mais de uma inscrição ativa na mesma partida; após cancelar, pode se inscrever novamente. |
| RN15 | Se um cancelamento liberar vaga e não houver fila, a partida lotada volta a aceitar inscrições diretas. |

## 4. Modelos de casos de uso

### 4.1. Jogador

| Código | Caso de uso | Descrição |
|--------|-------------|-----------|
| UC01 | Cadastrar-se | Criar uma conta no sistema. |
| UC02 | Realizar login | Acessar o sistema. |
| UC03 | Recuperar senha | Solicitar redefinição da senha. |
| UC04 | Atualizar dados pessoais | Alterar informações do cadastro. |
| UC05 | Consultar partidas | Visualizar partidas disponíveis. |
| UC06 | Inscrever-se em partida | Reservar uma vaga (ou entrar na lista de espera). |
| UC07 | Cancelar inscrição | Cancelar participação antes da partida. |
| UC08 | Consultar escalação | Verificar equipe (Azul/Amarelo) e informações da partida. |
| UC09 | Consultar penalidades | Visualizar suspensões e histórico disciplinar. |
| UC10 | Consultar histórico | Visualizar partidas disputadas. |

### 4.2. Organizador

| Código | Caso de uso | Descrição |
|--------|-------------|-----------|
| UC11 | Gerenciar partidas | Criar, editar ou cancelar partidas. |
| UC12 | Balancear equipes | Distribuir jogadores entre Azul e Amarelo. |
| UC13 | Alterar escalação | Mover jogadores entre equipes. |
| UC14 | Registrar presença | Confirmar jogadores presentes. |
| UC15 | Registrar resultado | Informar placar da partida (opcional). |
| UC16 | Aplicar penalidade | Registrar infrações aos jogadores. |
| UC17 | Consultar inscrições | Visualizar lista de inscritos e fila de espera. |

### 4.3. Administrador

| Código | Caso de uso | Descrição |
|--------|-------------|-----------|
| UC18 | Gerenciar usuários | Criar, editar, bloquear e excluir usuários. |
| UC19 | Gerenciar organizadores | Definir usuários como organizadores. |
| UC20 | Gerenciar modalidades | Cadastrar novos jogos (futebol, damas, sinuca etc.). |
| UC21 | Gerenciar calendário | Configurar dias de funcionamento, horários, feriados e recessos. |
| UC22 | Gerenciar locais | Cadastrar e editar locais das partidas. |
| UC23 | Gerenciar categorias | Manter Séries A, B e C. |
| UC24 | Gerenciar tipos de penalidade | Definir infrações e dias de suspensão. |
| UC25 | Gerar relatórios | Emitir relatórios de partidas, jogadores e penalidades. |
| UC26 | Sincronizar situação financeira | **Escopo a definir** com o clube — integração para atualizar a situação associativa automaticamente. Fora do MVP; no MVP a situação é mantida manualmente (ver UC27). |
| UC27 | Aprovar cadastro de jogador | Validar o vínculo associativo, aprovar o cadastro e manter a situação associativa (regular/irregular). |
