# Frontend Integration Guide - DailyBack Backend

## 1) Visao geral

Este guia descreve como consumir o backend DailyBack pelo frontend sem precisar ler o codigo interno.

### Objetivo do backend
- Autenticacao de usuarios com JWT
- Gestao de familia, membros e permissoes
- Gestao financeira com categorias, contas base, ocorrencias e dashboard

### Modulos disponiveis
- Auth
- Users
- Families
- Family Members
- Permissions
- Categories
- Accounts
- Occurrences
- Dashboard
- System/Health

### Convencoes da API
- Base path: sem prefixo de versao (ex.: `/auth/login`, `/accounts`)
- JSON request/response
- Datas em ISO (`yyyy-MM-dd` para campos de data, timestamp ISO para `createdAt` etc)
- UUIDs em string

### Formato base de erro
Erros seguem `ApiErrorResponse`:
```json
{
  "timestamp": "2026-05-01T10:00:00Z",
  "path": "/accounts",
  "errorCode": "ACCOUNT_ACCESS_DENIED",
  "message": "Access denied",
  "details": null,
  "traceId": "uuid"
}
```

### Autenticacao
- JWT Bearer no header `Authorization: Bearer <token>`
- JWT necessario em todas as rotas protegidas (todas exceto `/`, `/health`, `/swagger`, `/auth/register`, `/auth/login`)

---

## 2) Ambiente e configuracao

### Variaveis recomendadas no frontend
- `API_BASE_URL` (ex.: `http://localhost:8080`)
- `AUTH_TOKEN` (JWT atual)

### Headers obrigatorios
- Sempre: `Content-Type: application/json` em requests com body
- Em rotas autenticadas: `Authorization: Bearer <token>`

### Ambientes sugeridos
- Dev: `http://localhost:8080`
- HML: URL publica de homologacao
- Prod: URL publica de producao

### Exemplo de client base (conceitual)
- Incluir `baseURL` centralizado
- Incluir interceptor para anexar token
- Tratar `401` (sessao invalida) e `403` (sem permissao)

---

## 3) Autenticacao

### 3.1 Cadastrar usuario
- **Metodo**: `POST`
- **Path**: `/auth/register`
- **Auth**: nao
- **Objetivo**: criar usuario e devolver sessao autenticada

**Request**
```json
{
  "firstName": "Breno",
  "lastName": "Morais",
  "document": "12345678901",
  "birthDate": "1990-01-10",
  "password": "StrongPass#123",
  "email": "breno@example.com",
  "phone": "+5511999999999"
}
```

**Response sucesso** `201`
```json
{
  "accessToken": "jwt",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "user": {
    "id": "uuid",
    "firstName": "Breno",
    "lastName": "Morais",
    "document": "12345678901",
    "birthDate": "1990-01-10",
    "phone": "+5511999999999",
    "email": "breno@example.com",
    "status": "ACTIVE",
    "createdAt": "2026-05-01T10:00:00Z",
    "updatedAt": "2026-05-01T10:00:00Z",
    "lastLoginAt": "2026-05-01T10:00:00Z"
  }
}
```

**Erros comuns**
- `VALIDATION_ERROR`
- `DUPLICATE_USER_DOCUMENT`
- `DUPLICATE_USER_EMAIL`
- `DUPLICATE_USER_PHONE`

**Observacoes UX**
- Validar `birthDate` no frontend (`yyyy-MM-dd`) antes de enviar
- Em sucesso, usuario ja pode ser considerado logado

### 3.2 Login
- **Metodo**: `POST`
- **Path**: `/auth/login`
- **Auth**: nao
- **Objetivo**: autenticar com login + senha

**Request**
```json
{
  "login": "breno@example.com",
  "password": "StrongPass#123"
}
```

`login` aceita documento, email ou celular.

**Response sucesso** `200`: mesmo contrato de token de `/auth/register`.

**Erros comuns**
- `INVALID_CREDENTIALS` (`401`)
- `USER_DISABLED` (`403`)

### 3.3 Usuario autenticado
- **Metodo**: `GET`
- **Path**: `/auth/me`
- **Auth**: sim
- **Objetivo**: obter dados do usuario da sessao

**Response sucesso** `200`: `AuthUserResponse`

### 3.4 Logout / refresh
- **Logout**: nao existe endpoint dedicado
- **Refresh token**: nao existe endpoint dedicado
- **Pratica frontend**: remover token local para logout

### Recomendada para frontend (auth state)
- Persistir token com cuidado (preferir storage seguro conforme stack)
- Hidratacao inicial: se token existir, chamar `/auth/me`
- Em `401`, limpar sessao e redirecionar para login

---

## 4) Usuario

Estrutura exposta no frontend (`AuthUserResponse`):
- `id`
- `firstName`
- `lastName`
- `document`
- `birthDate`
- `phone`
- `email`
- `status` (`ACTIVE | DISABLED`)
- `createdAt`
- `updatedAt`
- `lastLoginAt`

### Campos internos vs expostos
- Frontend recebe apenas contrato acima (nao recebe hash de senha, etc)

### Estado global recomendado
- `auth.user`
- `auth.token`
- `auth.isAuthenticated`
- `auth.loading` / `auth.error`

---

## 5) Familia

### 5.1 Criar familia
- `POST /families` (auth)
- Request:
```json
{ "name": "Familia Morais" }
```
- Response `201`: `FamilyResponse`

### 5.2 Buscar familia atual
- `GET /families/me` (auth)
- Response `200`: `FamilyResponse`
- Se usuario nao tiver familia: `NO_FAMILY_FOR_USER` (`404`)

### 5.3 Listar membros da familia atual
- `GET /families/me/members` (auth)
- Response `200`: `FamilyMemberResponse[]`

### Papeis e status
- Role: `ADMIN | MEMBER`
- Membership status: `PENDING_REGISTRATION | ACTIVE | REMOVED`

### Como o frontend deve reagir sem familia
- Tratar `NO_FAMILY_FOR_USER` como estado de onboarding
- Exibir CTA para criar familia

### Contexto pessoal vs familiar
- UI pode mostrar modo/aba de contexto
- Acesso familiar depende de membership e permissoes

---

## 6) Membros da familia

### 6.1 Criar membro pendente (convite)
- **Metodo**: `POST`
- **Path**: `/families/current/members`
- **Auth**: sim + permissao `CAN_INVITE_MEMBERS`
- **Request**:
```json
{
  "displayName": "Joao",
  "document": "98765432100",
  "email": "joao@example.com",
  "phone": "+5511988888888"
}
```
- **Response**: `201`, `FamilyMemberResponse`

### 6.2 Alterar role
- `PATCH /families/current/members/{memberId}/role`
- Auth + `CAN_MANAGE_MEMBERS`
- Request: `{ "role": "ADMIN" }`
- Regra: ultimo ADMIN protegido (`LAST_FAMILY_ADMIN`)

### 6.3 Remover membro
- `DELETE /families/current/members/{memberId}`
- Auth + `CAN_MANAGE_MEMBERS`
- `204` em sucesso

### 6.4 Cenarios de status para UI
- `PENDING_REGISTRATION`: convidado ainda nao vinculado a usuario ativo
- `ACTIVE`: membro ativo
- `REMOVED`: removido (nao manter acesso)

### Vinculo automatico posterior
- Suportado no dominio: convite pendente pode ser vinculado quando pessoa se cadastra depois.

---

## 7) Permissoes

### 7.1 Buscar permissoes de um membro
- `GET /families/current/members/{memberId}/permissions`
- Auth

### 7.2 Atualizar permissoes
- `PUT /families/current/members/{memberId}/permissions`
- Auth
- Payload:
```json
{
  "canViewFamilyAccounts": true,
  "canCreateFamilyAccounts": false,
  "canEditFamilyAccounts": false,
  "canDeleteFamilyAccounts": false,
  "canMarkFamilyAccountsPaid": false,
  "canManageCategories": false,
  "canInviteMembers": false,
  "canManageMembers": false,
  "canViewOtherPersonalAccounts": false,
  "canEditOtherPersonalAccounts": false
}
```

### Lista de permissoes do backend
- `CAN_VIEW_FAMILY_ACCOUNTS`
- `CAN_CREATE_FAMILY_ACCOUNTS`
- `CAN_EDIT_FAMILY_ACCOUNTS`
- `CAN_DELETE_FAMILY_ACCOUNTS`
- `CAN_MARK_FAMILY_ACCOUNTS_PAID`
- `CAN_MANAGE_CATEGORIES`
- `CAN_INVITE_MEMBERS`
- `CAN_MANAGE_MEMBERS`
- `CAN_VIEW_OTHER_PERSONAL_ACCOUNTS`
- `CAN_EDIT_OTHER_PERSONAL_ACCOUNTS`

### Comportamento ADMIN x MEMBER
- ADMIN ativo: permissao total da familia
- MEMBER ativo: depende das flags

### Orientacao de UI
- Esconder/desabilitar botoes conforme flags
- Sempre tratar `403` do backend como fonte final de verdade

---

## 8) Categorias

### Rotas
- `GET /categories`
- `GET /categories/{id}`
- `POST /categories`
- `PUT /categories/{id}`
- `DELETE /categories/{id}`

### Regras de contexto
- Categorias visiveis podem ser globais, pessoais ou familiares
- Operacoes de escrita em contexto familiar exigem `CAN_MANAGE_CATEGORIES`

### Contrato principal
```json
{
  "id": "uuid",
  "name": "Alimentacao",
  "color": "#22AA66",
  "scope": "GLOBAL|FAMILY|PERSONAL",
  "familyId": "uuid|null",
  "ownerUserId": "uuid|null",
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

### Dica para formularios
- Sempre carregar categorias antes de criar/editar conta
- No seletor, mostrar label por `scope`

---

## 9) Contas base

### Rotas
- `GET /accounts`
- `GET /accounts/{id}`
- `POST /accounts`
- `PUT /accounts/{id}`
- `PATCH /accounts/{id}/activate`
- `PATCH /accounts/{id}/deactivate`
- `DELETE /accounts/{id}`

### Scope em listagem
- Query `scope`:
  - `PERSONAL`
  - `FAMILY`
  - `VISIBLE_TO_ME` (default)

### Ownership
- `PERSONAL`: conta do usuario
- `FAMILY`: conta da familia

### `responsibleMemberId`
- Campo opcional para conta `FAMILY`
- Deve ser membro da mesma familia

### Exemplo create PERSONAL
```json
{
  "ownershipType": "PERSONAL",
  "title": "Internet",
  "baseAmount": "120.50",
  "startDate": "2026-05-01",
  "endDate": null,
  "recurrenceType": "MONTHLY",
  "categoryId": "uuid",
  "notes": "Plano fibra",
  "active": true
}
```

### Exemplo create FAMILY
```json
{
  "ownershipType": "FAMILY",
  "responsibleMemberId": "uuid",
  "title": "Aluguel",
  "baseAmount": "1500.00",
  "startDate": "2026-05-01",
  "endDate": null,
  "recurrenceType": "MONTHLY",
  "categoryId": "uuid",
  "notes": "Casa",
  "active": true
}
```

### Orientacao de formulario frontend
- Se `ownershipType=PERSONAL`: ocultar `responsibleMemberId`
- Se `ownershipType=FAMILY`: habilitar seletor de membro responsavel
- Validar `baseAmount` string decimal e datas antes de enviar

---

## 10) Ocorrencias e recorrencia

### Rotas
- `GET /occurrences`
- `GET /occurrences/{id}`
- `PATCH /occurrences/{id}/mark-paid`
- `PATCH /occurrences/{id}/unmark-paid`
- `PATCH /occurrences/{id}/override-amount`

### Filtros disponiveis
- `scope`: `PERSONAL | FAMILY | VISIBLE_TO_ME`
- `status`: `PENDING | PAID`
- `categoryId`
- `text`
- `startDate`
- `endDate`
- `month` (`yyyy-MM`)

### Relacao conta x ocorrencia
- Conta base define ownership/contexto
- Ocorrencia eh snapshot historico da conta em cada vencimento

### Regras importantes
- Ocorrencias pagas nao devem ser recalculadas automaticamente
- Duplicidade protegida pelo backend no upsert por (`account_id`, `due_date`)
- Janela de geracao futura de recorrencia eh mantida pelo backend

### Pagamento/permissao
- Em conta `FAMILY`, marcar/desmarcar pago exige permissao adequada (`canMarkFamilyAccountsPaid`)
- Sobrescrita de valor exige permissao de edicao da conta no contexto

---

## 11) Filtros e contexto

### Escopo de consulta
- **Minhas contas/ocorrencias pessoais**: `scope=PERSONAL`
- **Contas/ocorrencias da familia**: `scope=FAMILY`
- **Tudo visivel para mim**: `scope=VISIBLE_TO_ME`

### Combinacao de filtros (occurrences)
Exemplo:
`/occurrences?scope=VISIBLE_TO_ME&month=2026-05&status=PENDING&categoryId=<uuid>`

### Recomendacao de UI para tabs
- Tab 1: Pessoal (`PERSONAL`)
- Tab 2: Familia (`FAMILY`)
- Tab 3: Visivel para mim (`VISIBLE_TO_ME`)

### Erros comuns de filtro
- scope invalido:
  - contas: `INVALID_ACCOUNT_REQUEST` (`400`)
  - ocorrencias: `INVALID_OCCURRENCE_REQUEST` (`400`)

---

## 12) Dashboard e consultas agregadas

### Rotas implementadas
- `GET /dashboard/home?month=yyyy-MM`
- `GET /dashboard/day?date=yyyy-MM-dd`
- `GET /dashboard/next-12-months?includeDetails=true|false`
- `GET /dashboard/category-summary?month=yyyy-MM`

### Regras de acesso
- Auth obrigatorio
- Se usuario tem membership ativo, backend exige permissao para visualizar contas de familia (`CAN_VIEW_FAMILY_ACCOUNTS`)

### Uso no frontend
- `home`: cards e listas iniciais
- `day`: detalhe por data
- `next-12-months`: projecao/serie mensal
- `category-summary`: grafico por categoria

### UX
- Loading por widget
- Empty state por bloco (nao tratar dashboard inteiro como erro)
- Refetch apos mutacoes financeiras (ex.: pagar ocorrencia)

---

## 13) Modelos de dados para frontend (TypeScript ilustrativo)

```ts
export interface ErrorResponse {
  timestamp: string;
  path: string;
  errorCode: string;
  message: string | null;
  details: Record<string, string> | null;
  traceId: string | null;
}

export interface AuthUserResponse {
  id: string;
  firstName: string;
  lastName: string;
  document: string;
  birthDate: string; // yyyy-MM-dd
  phone: string | null;
  email: string | null;
  status: "ACTIVE" | "DISABLED";
  createdAt: string;
  updatedAt: string;
  lastLoginAt: string | null;
}

export interface AuthTokenResponse {
  accessToken: string;
  tokenType: "Bearer";
  expiresInSeconds: number;
  user: AuthUserResponse;
}

export interface FamilyResponse {
  id: string;
  name: string;
  createdByUserId: string | null;
  status: "ACTIVE" | "ARCHIVED";
  createdAt: string;
  updatedAt: string;
}

export interface FamilyMemberResponse {
  id: string;
  familyId: string;
  userId: string | null;
  displayName: string;
  document: string | null;
  email: string | null;
  phone: string | null;
  role: "ADMIN" | "MEMBER";
  status: "PENDING_REGISTRATION" | "ACTIVE" | "REMOVED";
  invitedByUserId: string | null;
  joinedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface FamilyMemberPermissionsResponse {
  canViewFamilyAccounts: boolean;
  canCreateFamilyAccounts: boolean;
  canEditFamilyAccounts: boolean;
  canDeleteFamilyAccounts: boolean;
  canMarkFamilyAccountsPaid: boolean;
  canManageCategories: boolean;
  canInviteMembers: boolean;
  canManageMembers: boolean;
  canViewOtherPersonalAccounts: boolean;
  canEditOtherPersonalAccounts: boolean;
}

export interface CategoryResponse {
  id: string;
  name: string;
  color: string | null;
  scope: "GLOBAL" | "FAMILY" | "PERSONAL";
  familyId: string | null;
  ownerUserId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AccountResponse {
  id: string;
  title: string;
  baseAmount: string;
  startDate: string;
  endDate: string | null;
  recurrenceType: "UNIQUE" | "DAILY" | "WEEKLY" | "MONTHLY";
  categoryId: string;
  notes: string | null;
  active: boolean;
  ownershipType: "PERSONAL" | "FAMILY";
  ownerUserId: string | null;
  familyId: string | null;
  createdByUserId: string | null;
  responsibleMemberId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface OccurrenceResponse {
  id: string;
  accountId: string;
  titleSnapshot: string;
  amountSnapshot: string;
  dueDate: string;
  status: "PENDING" | "PAID";
  paidAt: string | null;
  notesSnapshot: string | null;
  categoryIdSnapshot: string;
  createdAt: string;
  updatedAt: string;
}

export interface DashboardOccurrenceResponse {
  id: string;
  accountId: string;
  title: string;
  amount: string;
  dueDate: string;
  status: "PENDING" | "PAID";
  categoryId: string;
}
```

---

## 14) Fluxos de integracao recomendados

### Fluxo 1: onboarding inicial
1. `POST /auth/register` (ou `POST /auth/login`)
2. Persistir token
3. `GET /auth/me`
4. `GET /families/me`
   - Se `NO_FAMILY_FOR_USER`: direcionar para criar familia

### Fluxo 2: setup de familia
1. `POST /families`
2. `GET /families/me/members`
3. `POST /families/current/members`
4. refetch de membros

### Fluxo 3: setup financeiro basico
1. `GET /categories`
2. `POST /accounts` (PERSONAL ou FAMILY)
3. `GET /accounts?scope=VISIBLE_TO_ME`
4. `GET /occurrences?scope=VISIBLE_TO_ME&month=yyyy-MM`

### Fluxo 4: governanca de UI por permissao
1. `GET /families/current/members/{memberId}/permissions`
2. montar guards na UI
3. habilitar/ocultar acoes sensiveis

---

## 15) Boas praticas para frontend

- Centralizar HTTP client e auth interceptor
- Centralizar parser de erro (`errorCode`)
- Tratar `401` globalmente (limpar sessao)
- Tratar `403` com feedback de permissao
- Usar tipagem forte (interfaces/DTOs)
- Fazer refetch/invalidacao apos mutacoes
- Nao confiar apenas na UI para seguranca
- Sempre respeitar resposta do backend

---

## 16) Tabela-resumo final

| Modulo | Rota | Auth | Uso principal no frontend |
|---|---|---|---|
| Auth | `POST /auth/register` | Nao | Cadastro inicial |
| Auth | `POST /auth/login` | Nao | Login |
| Users | `GET /auth/me` | Sim | Hidratar sessao |
| Families | `POST /families` | Sim | Criar familia |
| Families | `GET /families/me` | Sim | Ler contexto familiar |
| Family Members | `GET /families/me/members` | Sim | Listar membros |
| Family Members | `POST /families/current/members` | Sim + permissao | Convidar membro |
| Family Members | `PATCH /families/current/members/{memberId}/role` | Sim + permissao | Promover/rebaixar |
| Family Members | `DELETE /families/current/members/{memberId}` | Sim + permissao | Remover membro |
| Permissions | `GET /families/current/members/{memberId}/permissions` | Sim | Ler flags |
| Permissions | `PUT /families/current/members/{memberId}/permissions` | Sim | Atualizar flags |
| Categories | `GET /categories` | Sim | Carregar categorias |
| Categories | `GET /categories/{id}` | Sim | Detalhe categoria |
| Categories | `POST /categories` | Sim | Criar categoria |
| Categories | `PUT /categories/{id}` | Sim | Editar categoria |
| Categories | `DELETE /categories/{id}` | Sim | Excluir categoria |
| Accounts | `GET /accounts` | Sim | Listar contas por escopo |
| Accounts | `GET /accounts/{id}` | Sim | Detalhe conta |
| Accounts | `POST /accounts` | Sim | Criar conta base |
| Accounts | `PUT /accounts/{id}` | Sim | Atualizar conta |
| Accounts | `PATCH /accounts/{id}/activate` | Sim | Ativar conta |
| Accounts | `PATCH /accounts/{id}/deactivate` | Sim | Desativar conta |
| Accounts | `DELETE /accounts/{id}` | Sim | Excluir/desativar por regra |
| Occurrences | `GET /occurrences` | Sim | Listar ocorrencias |
| Occurrences | `GET /occurrences/{id}` | Sim | Detalhe ocorrencia |
| Occurrences | `PATCH /occurrences/{id}/mark-paid` | Sim | Marcar pagamento |
| Occurrences | `PATCH /occurrences/{id}/unmark-paid` | Sim | Desfazer pagamento |
| Occurrences | `PATCH /occurrences/{id}/override-amount` | Sim | Ajustar valor snapshot |
| Dashboard | `GET /dashboard/home` | Sim | Resumo home |
| Dashboard | `GET /dashboard/day` | Sim | Detalhe diario |
| Dashboard | `GET /dashboard/next-12-months` | Sim | Projecao |
| Dashboard | `GET /dashboard/category-summary` | Sim | Resumo por categoria |
| System | `GET /health` | Nao | Healthcheck |
| System | `GET /` | Nao | Ping base da API |
| System | `GET /swagger` | Nao | UI swagger |

---

## 17) Qualidade e aderencia

- Conteudo alinhado com rotas reais em:
  - `ApplicationModule`
  - `AuthRoutes`, `FamilyRoutes`, `CategoryRoutes`, `AccountRoutes`, `OccurrenceRoutes`, `DashboardRoutes`, `HealthRoutes`
- Campos e enums usados conforme DTOs/enums reais (`*Dtos.kt`, `FamilyPermissionKey`, `RecurrenceType`, `AccountOwnershipType`, etc)
- Sem endpoints inventados.
