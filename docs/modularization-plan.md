# SafeVault Backend Modularization (Modular Monolith)

## Goal

Refactor backend structure into clear modules without changing external API behavior.

## Logical Modules

- `auth` - registration/login/token/identity verification
- `vault` - encrypted vault and private key lifecycle
- `share` - cloud sharing and notification workflows
- `contact` - friend/contact relationship graph
- `platform` - security/config/rate-limiting/infra concerns

## Current Mapping

- Controllers remain in `controller/`, grouped by module ownership:
  - auth: `AuthController`, `AccountController`, `UserController`, `VerificationWebController`
  - vault: `VaultController`
  - share/contact: `ContactShareController`, `FriendController`
- Services/repositories/entities are preserved to avoid runtime behavior changes.
- Module boundaries are documented via `modules/*/package-info.java`.

## Dependency Direction

`controller -> service -> repository/entity`

Cross-module calls are allowed only via service interfaces/facades.

## Incremental Extraction Path (Future)

1. Introduce module-local facades per domain.
2. Move domain-specific DTOs to module subpackages.
3. Add architecture tests to block forbidden dependencies.
4. Extract first microservice from lowest-coupled domain (`share` or `contact`).
