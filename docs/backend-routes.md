# DailyBack Backend - Documentacao Operacional Final

Documentacao gerada a partir das rotas implementadas no codigo (`ApplicationModule` + arquivos `*Routes.kt`).

## Convencoes

- Base URL: `{{baseUrl}}` (ex.: `http://localhost:8080`)
- Auth header: `Authorization: Bearer {{token}}`
- Formato de erro padrao:

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

## Auth

### POST `/auth/register`
- **Auth**: nao
- **Descricao**: cadastra usuario e retorna token + dados do usuario.
- **Request**:
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
- **Sucesso** `201`:
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
- **Erros principais**: `DUPLICATE_USER_DOCUMENT`, `DUPLICATE_USER_EMAIL`, `DUPLICATE_USER_PHONE`, `VALIDATION_ERROR`
- **Observacoes**: `birthDate` deve ser ISO `yyyy-MM-dd`.

### POST `/auth/login`
- **Auth**: nao
- **Descricao**: login por documento/email/telefone + senha.
- **Request**:
```json
{
  "login": "breno@example.com",
  "password": "StrongPass#123"
}
```
- **Sucesso** `200`: mesmo formato de token de `/auth/register`.
- **Erros principais**: `INVALID_CREDENTIALS`, `USER_DISABLED`
- **Observacoes**: identificador eh resolvido automaticamente (documento/email/celular).

## Users

### GET `/auth/me`
- **Auth**: sim
- **Descricao**: retorna usuario autenticado.
- **Request payload**: nao
- **Sucesso** `200`: objeto `AuthUserResponse`.
- **Erros principais**: `UNAUTHORIZED`, `USER_NOT_FOUND`

## Families

### POST `/families`
- **Auth**: sim
- **Descricao**: cria familia do usuario autenticado.
- **Request**:
```json
{ "name": "Familia Morais" }
```
- **Sucesso** `201`:
```json
{
  "id": "uuid",
  "name": "Familia Morais",
  "createdByUserId": "uuid",
  "status": "ACTIVE",
  "createdAt": "2026-05-01T10:00:00Z",
  "updatedAt": "2026-05-01T10:00:00Z"
}
```
- **Erros principais**: `INVALID_FAMILY_NAME`, `USER_ALREADY_HAS_FAMILY`

### GET `/families/me`
- **Auth**: sim
- **Descricao**: retorna familia do usuario.
- **Sucesso** `200`: `FamilyResponse`
- **Erros principais**: `NO_FAMILY_FOR_USER`, `FAMILY_NOT_FOUND`

## Family Members

### GET `/families/me/members`
- **Auth**: sim
- **Descricao**: lista membros da familia do usuario.
- **Sucesso** `200`: lista de `FamilyMemberResponse`.
- **Erros principais**: `NO_FAMILY_FOR_USER`

### POST `/families/current/members`
- **Auth**: sim + permissao `CAN_INVITE_MEMBERS`
- **Descricao**: cria convite/membro pendente.
- **Request**:
```json
{
  "displayName": "Joao",
  "document": "98765432100",
  "email": "joao@example.com",
  "phone": "+5511988888888"
}
```
- **Sucesso** `201`: `FamilyMemberResponse`
- **Erros principais**: `FAMILY_PERMISSION_DENIED`, `DUPLICATE_FAMILY_MEMBER_INVITE`, `INVITE_TARGET_IN_OTHER_FAMILY`, `INVALID_FAMILY_MEMBER_DISPLAY_NAME`
- **Observacoes**: convite pode ser vinculado automaticamente ao usuario ao registrar depois (fluxo de dominio implementado).

### PATCH `/families/current/members/{memberId}/role`
- **Auth**: sim + permissao `CAN_MANAGE_MEMBERS`
- **Descricao**: promove/rebaixa role de membro.
- **Request**:
```json
{ "role": "ADMIN" }
```
- **Sucesso** `200`: `FamilyMemberResponse`
- **Erros principais**: `FAMILY_PERMISSION_DENIED`, `FAMILY_MEMBER_NOT_FOUND`, `INVALID_FAMILY_MEMBER_ROLE_VALUE`, `INVALID_FAMILY_MEMBER_ROLE_CHANGE`, `LAST_FAMILY_ADMIN`
- **Observacoes**: ultimo ADMIN eh protegido.

### DELETE `/families/current/members/{memberId}`
- **Auth**: sim + permissao `CAN_MANAGE_MEMBERS`
- **Descricao**: remove membro.
- **Sucesso** `204`
- **Erros principais**: `FAMILY_PERMISSION_DENIED`, `FAMILY_MEMBER_NOT_FOUND`, `LAST_FAMILY_ADMIN`

## Permissions

### GET `/families/current/members/{memberId}/permissions`
- **Auth**: sim
- **Descricao**: consulta flags de permissao do membro.
- **Sucesso** `200`:
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
- **Erros principais**: `NOT_FAMILY_ADMIN`, `FAMILY_MEMBER_NOT_FOUND`

### PUT `/families/current/members/{memberId}/permissions`
- **Auth**: sim
- **Descricao**: atualiza flags de permissao.
- **Request/Sucesso**: mesmo formato acima.
- **Erros principais**: `NOT_FAMILY_ADMIN`, `FAMILY_MEMBER_NOT_FOUND`

## Categories

### GET `/categories`
- **Auth**: sim
- **Descricao**: lista categorias visiveis no contexto do usuario.
- **Sucesso** `200`:
```json
[
  {
    "id": "uuid",
    "name": "Alimentacao",
    "color": "#22AA66",
    "scope": "PERSONAL",
    "familyId": null,
    "ownerUserId": "uuid",
    "createdAt": "2026-05-01T10:00:00Z",
    "updatedAt": "2026-05-01T10:00:00Z"
  }
]
```
- **Erros principais**: `FAMILY_PERMISSION_DENIED` (quando membership ativo sem permissao de visualizar contas de familia)

### GET `/categories/{id}`
- **Auth**: sim
- **Descricao**: busca categoria por id respeitando contexto.
- **Sucesso** `200`: `CategoryResponse`
- **Erros principais**: `CATEGORY_NOT_FOUND`, `INVALID_PATH_PARAMETER`

### POST `/categories`
- **Auth**: sim (se membership ativo, exige `CAN_MANAGE_CATEGORIES`)
- **Descricao**: cria categoria no contexto permitido.
- **Request**:
```json
{ "name": "Alimentacao", "color": "#22AA66" }
```
- **Sucesso** `201`: `CategoryResponse`
- **Erros principais**: `FAMILY_PERMISSION_DENIED`, `INVALID_CATEGORY_NAME`, `CATEGORY_NAME_ALREADY_EXISTS`

### PUT `/categories/{id}`
- **Auth**: sim (se membership ativo, exige `CAN_MANAGE_CATEGORIES`)
- **Descricao**: atualiza categoria.
- **Request**:
```json
{ "name": "Alimentacao Casa", "color": "#2288FF" }
```
- **Sucesso** `200`: `CategoryResponse`
- **Erros principais**: `CATEGORY_NOT_FOUND`, `CATEGORY_NAME_ALREADY_EXISTS`, `CATEGORY_NOT_MODIFIABLE`

### DELETE `/categories/{id}`
- **Auth**: sim (se membership ativo, exige `CAN_MANAGE_CATEGORIES`)
- **Descricao**: remove categoria.
- **Sucesso** `204`
- **Erros principais**: `CATEGORY_IN_USE`, `CATEGORY_NOT_FOUND`, `CATEGORY_NOT_MODIFIABLE`

## Accounts

### GET `/accounts`
- **Auth**: sim
- **Descricao**: lista contas por escopo.
- **Query params**:
  - `scope`: `PERSONAL | FAMILY | VISIBLE_TO_ME` (default `VISIBLE_TO_ME`)
- **Sucesso** `200`: lista de `AccountResponse`.
- **Erros principais**: `INVALID_ACCOUNT_REQUEST`, `FAMILY_PERMISSION_DENIED` (quando `scope=FAMILY` sem permissao)
- **Observacoes**: escopo e ownership resolvidos no service/repository.

### GET `/accounts/{id}`
- **Auth**: sim
- **Descricao**: busca conta por id no contexto visivel.
- **Sucesso** `200`: `AccountResponse`
- **Erros principais**: `ACCOUNT_NOT_FOUND`, `INVALID_PATH_PARAMETER`

### POST `/accounts`
- **Auth**: sim
- **Descricao**: cria conta base `PERSONAL` ou `FAMILY`.
- **Request PERSONAL**:
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
- **Request FAMILY**:
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
- **Sucesso** `201`: `AccountResponse`
- **Erros principais**: `INVALID_ACCOUNT_OWNERSHIP`, `CATEGORY_NOT_FOUND`, `INVALID_ACCOUNT_TITLE`, `INVALID_ACCOUNT_AMOUNT`, `INVALID_ACCOUNT_DATE_RANGE`, `FAMILY_PERMISSION_DENIED` (criacao FAMILY sem permissao)
- **Observacoes**: gera ocorrencias futuras conforme janela de recorrencia.

### PUT `/accounts/{id}`
- **Auth**: sim
- **Descricao**: atualiza dados base da conta.
- **Sucesso** `200`: `AccountResponse`
- **Erros principais**: `ACCOUNT_NOT_FOUND`, `ACCOUNT_ACCESS_DENIED`, `CATEGORY_NOT_FOUND`, validacoes de payload.
- **Observacoes**: ocorrencias pagas/historicas sao preservadas; futuras pendentes sao recalculadas.

### PATCH `/accounts/{id}/activate`
- **Auth**: sim
- **Descricao**: ativa conta.
- **Sucesso** `200`: `AccountResponse`
- **Erros principais**: `ACCOUNT_NOT_FOUND`, `ACCOUNT_ACCESS_DENIED`

### PATCH `/accounts/{id}/deactivate`
- **Auth**: sim
- **Descricao**: desativa conta.
- **Sucesso** `200`: `AccountResponse`
- **Erros principais**: `ACCOUNT_NOT_FOUND`, `ACCOUNT_ACCESS_DENIED`

### DELETE `/accounts/{id}`
- **Auth**: sim
- **Descricao**: exclui conta (ou desativa se houver historico relevante).
- **Sucesso** `204`
- **Erros principais**: `ACCOUNT_NOT_FOUND`, `ACCOUNT_ACCESS_DENIED`

## Occurrences

### GET `/occurrences`
- **Auth**: sim
- **Descricao**: lista ocorrencias visiveis por escopo e filtros.
- **Query params**:
  - `scope`: `PERSONAL | FAMILY | VISIBLE_TO_ME`
  - `status`: `PENDING | PAID`
  - `categoryId`: UUID
  - `text`: busca em titulo/notas
  - `startDate`: `yyyy-MM-dd` (**obrigatorio**)
  - `endDate`: `yyyy-MM-dd` (**obrigatorio**)
  - `month`: `yyyy-MM` (opcional, aplicado em conjunto com o intervalo informado)
- **Sucesso** `200`: lista de `OccurrenceResponse`
- **Erros principais**: `INVALID_OCCURRENCE_REQUEST`, `FAMILY_PERMISSION_DENIED` (quando `scope=FAMILY` sem permissao)
- **Validacoes**:
  - retorna `400` quando `startDate`/`endDate` nao sao enviados
  - retorna `400` quando formato de data eh invalido
  - retorna `400` quando `endDate < startDate`

### GET `/occurrences/{id}`
- **Auth**: sim
- **Descricao**: busca ocorrencia por id respeitando acesso da conta pai.
- **Sucesso** `200`: `OccurrenceResponse`
- **Erros principais**: `OCCURRENCE_NOT_FOUND`

### PATCH `/occurrences/{id}/mark-paid`
- **Auth**: sim
- **Descricao**: marca ocorrencia como paga.
- **Sucesso** `200`: `OccurrenceResponse`
- **Erros principais**: `OCCURRENCE_NOT_FOUND`, `ACCOUNT_ACCESS_DENIED`
- **Observacoes**: em conta FAMILY depende de `canMarkFamilyAccountsPaid`.

### PATCH `/occurrences/{id}/unmark-paid`
- **Auth**: sim
- **Descricao**: desfaz pagamento.
- **Sucesso** `200`: `OccurrenceResponse`
- **Erros principais**: `OCCURRENCE_NOT_FOUND`, `ACCOUNT_ACCESS_DENIED`

### PATCH `/occurrences/{id}/override-amount`
- **Auth**: sim
- **Descricao**: sobrescreve valor da ocorrencia.
- **Request**:
```json
{ "amount": "999.99" }
```
- **Sucesso** `200`: `OccurrenceResponse`
- **Erros principais**: `INVALID_OCCURRENCE_AMOUNT`, `OCCURRENCE_NOT_FOUND`, `ACCOUNT_ACCESS_DENIED`
- **Observacoes**: em conta FAMILY depende de permissao de edicao da conta.

### Regras de recorrencia (atualizacao de conta base)
- Criacao de conta recorrente gera ocorrencias com o mesmo `baseAmount` inicial.
- `override-amount` altera apenas a ocorrencia alvo (nao altera a conta base e nao propaga para outras ocorrencias).
- Ao editar a conta base recorrente:
  - **ocorrencia antiga** = `dueDate < hoje` **ou** `status=PAID`
  - **ocorrencia futura** = `dueDate >= hoje` **e** `status=PENDING`
  - somente ocorrencias futuras pendentes sao atualizadas.

## Dashboard

### GET `/dashboard/home`
- **Auth**: sim (se membership ativo, exige `CAN_VIEW_FAMILY_ACCOUNTS`)
- **Query params**: `month` (`yyyy-MM`, opcional)
- **Sucesso** `200`:
```json
{
  "overdue": [],
  "next7Days": [],
  "totalPendingInMonth": "0.00",
  "totalPaidInMonth": "0.00",
  "upcoming": [],
  "categorySummary": []
}
```
- **Erros principais**: `INVALID_DASHBOARD_QUERY`, `FAMILY_PERMISSION_DENIED`

### GET `/dashboard/day`
- **Auth**: sim (mesma regra de permissao)
- **Query params**: `date` (obrigatorio, `yyyy-MM-dd`)
- **Sucesso** `200`: lista de `DashboardOccurrenceResponse`
- **Erros principais**: `MISSING_QUERY_PARAMETER`, `INVALID_DASHBOARD_QUERY`, `FAMILY_PERMISSION_DENIED`

### GET `/dashboard/next-12-months`
- **Auth**: sim (mesma regra de permissao)
- **Query params**: `includeDetails` (`true|false`)
- **Sucesso** `200`: lista de `DashboardMonthProjectionResponse`
- **Erros principais**: `INVALID_DASHBOARD_QUERY`, `FAMILY_PERMISSION_DENIED`

### GET `/dashboard/category-summary`
- **Auth**: sim (mesma regra de permissao)
- **Query params**: `month` (`yyyy-MM`, opcional)
- **Sucesso** `200`: lista de `DashboardCategorySummaryResponse`
- **Erros principais**: `INVALID_DASHBOARD_QUERY`, `FAMILY_PERMISSION_DENIED`

## System

### GET `/health`
- **Auth**: nao
- **Descricao**: health check da API e banco.
- **Sucesso** `200` ou `503`:
```json
{
  "status": "UP",
  "checks": {
    "database": "UP"
  },
  "timestamp": "2026-05-01T10:00:00Z"
}
```

### GET `/`
- **Auth**: nao
- **Descricao**: endpoint basico de status do servico.
- **Sucesso** `200`:
```json
{
  "service": "daily-back",
  "status": "running"
}
```

### GET `/swagger`
- **Auth**: nao
- **Descricao**: UI do Swagger (arquivo atual de OpenAPI no projeto ainda minimalista).
