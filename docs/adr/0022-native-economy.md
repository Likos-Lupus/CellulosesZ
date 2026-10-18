# ADR 0022 — Native economy ledger

## Status

Accepted.

## Context

CellulosesZ needs a server-side currency that third parties (quests, shops, kits) can award and
charge against without ever bypassing the rules. Prior experience with provider ecosystems (Vault,
Treasury, EconomyCraft) shows two failure modes: multiple writers racing on a bare number, and
"compatibility" layers that duplicate or silently diverge from the real ledger. We want one owner
and one commit path.

## Decision

CellulosesZ ships its own **native economy** as a first-class feature module (`:modules:economy`).
It is not a provider and integrates with nothing: there is **no Vault, Treasury or EconomyCraft
compatibility**, and CellulosesZ never reads or migrates another mod's balances. Coexistence with
another economy mod is limited to command-name conflict handling (the namespaced
`/cellulosesz:<command>` fallback), never shared state.

### Money

- The domain uses an exact `BigDecimal` with `scale = 2` (`Money`), never `Double`/`Float`.
  `setScale(2, RoundingMode.UNNECESSARY)` rejects more than two fractional digits instead of
  rounding. Command/config input is a quoted decimal string; TOML floats are rejected.
- Storage binds the exact minor unit as `BIGINT` (`balance_minor`, `delta_minor`). SQLite's NUMERIC
  affinity can store approximate REAL values, so an integer minor unit is the only portable choice
  across SQLite/H2/MySQL/MariaDB/PostgreSQL.
- A configurable `maximum-balance` bounds every account well below `Long` overflow.

### Accounts and double entry

- A player account is keyed by UUID (`player:<uuid>`); two system accounts (`system:mint`,
  `system:sink`) are the issue/reclaim counterparts. Only `EconomyAccountId` can name a system
  account, so no request can address one as a payment target.
- Every committed transaction has **balanced signed entries** that sum to zero and a persisted
  header. `cz_economy_transactions`/`cz_economy_entries` are immutable; a player account can never
  go negative.
- Opening balance is applied **once**, as a balanced `OPENING` transaction, guarded by the account
  primary key — never "credit zero on every login".

### Atomicity and idempotency

- All mutations commit in one `database.transaction`: guarded balance updates, the header and the
  entries together, or nothing. Debits are conditional (`WHERE balance_minor >= ?`); credits are
  bounded (`WHERE balance_minor <= max - ?`). A zero-row update is classified by re-reading the
  account (insufficient funds / over limit), never guessed.
- Idempotency is `(namespace, origin_id, request_key)` with a request fingerprint; a replay returns
  the stored receipt, a mismatch is an `IdempotencyConflict`. `SET` uses optimistic concurrency
  (`revision`) and records a durable `SET_NO_CHANGE` decision when the target equals the current
  balance.
- A recognized transient backend failure (deadlock / serialization conflict / lock timeout) retries
  the whole transaction a bounded number of times with a short backoff **off** the JDBC block; each
  attempt re-checks the idempotency header, so a retry can never double-apply, and the request key
  and fingerprint are never regenerated. An unrecognized failure is never retried.
- A corrupt row (negative player balance, unknown kind, unbalanced transaction) raises
  `EconomyDataException`; it is never silently repaired or read as zero.

### Cancellation

- A money-moving API request runs behind a `MoneyCommitFence`. `ApiRequestHandle.cancel()` succeeds
  only while the request is still preparing; once it has claimed the database commit region the
  fence refuses cancellation (a blocking JDBC commit cannot be interrupted). `cancel() == true`
  therefore guarantees no funds were moved, and `cancel() == false` means the caller must recover
  the outcome by its `requestKey` — never by retrying with a new key.

### Integration and API

- Commands and the public API both call the single `EconomyService`; `/eco` never re-implements a
  balance write. Commands run their JDBC on the IO dispatcher and never block the server thread.
- The public `EconomyApi` is a thin adapter over a Minecraft-free `EconomyIntegrationPort`.
  Privileged operations (`economy.grant`/`take`/`set`/`pay.as-player`, `economy.balance.read`/
  `ranking.read`/`history.read`) are **deny-by-default** and authorized server-side against the
  extension hub's registered handle plus the live `[api]` config — a self-implemented `AddonAccess`
  is rejected.
- Only committed facts are observable; a rejected operation produces no event.

### Storage migration

- The three economy tables are grouped: a source that predates the feature (0/3 tables) migrates as
  empty, a current source (3/3) copies fully, and a partially-present group (1–2/3) is treated as a
  broken source and refused rather than silently migrated.

## Consequences

- A reload may toggle `[economy] enabled` but never rescale balances; `currencyId`/scale are not
  configuration.
- `enabled = false` (the default) rejects new transactions and keeps all tables and balances; it
  does not delete data.
- There are no shops, auctions, holds or multi-currency exchange in this version. A paid-kit/shop
  flow needs an external coordinator with a durable hold protocol and a reconciliation state; it is
  deliberately out of scope.

## Verification

- `MoneyTest`, `EconomyLedgerCommitTest` and `EconomyLedgerQueryTest` cover the money type and the
  SQLite ledger contract;
  `EconomyLedgerBackendsTest` repeats the contract on **both SQLite and H2** and adds a concurrency
  case (interleaved transfers neither overdraw nor lose funds). `EconomySchemaGroupMigrationTest`
  covers the 0/3, 3/3 and partial-group migration rules. `MoneyCommitFenceTest` and
  `AddonAuthorizationTest` cover the cancel contract and the deny-by-default addon authorization.
- MySQL/MariaDB/PostgreSQL and the live dual-loader matrix are not run in unit CI (they need
  external servers); they are exercised only when such an environment is available.
- The ledger does not yet retry transient backend failures (deadlock / serialization / lock
  timeout): those surface as typed failures and must be retried by the caller with the **same**
  `requestKey`.
