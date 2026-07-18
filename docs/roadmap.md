# Roadmap

## Phase 0 — Validation

- Confirm Openfire 5.1.1 plugin build requirements and Java 21 compatibility.
- Confirm the exact `DefaultAuthProvider` write path.
- Confirm the `ofUser` schema used by the installed PostgreSQL database.
- Confirm how LDAP and local user providers interact for an LDAP identity with a local credential row.
- Document rollback procedures.

Exit criterion: a reviewed technical decision describing the supported credential-write approach.

## Phase 1 — Plugin skeleton

- Maven build.
- Openfire plugin descriptor.
- Plugin lifecycle class.
- Admin-console menu entry.
- Read-only status page.
- Basic unit-test setup.

Exit criterion: plugin installs and uninstalls on Openfire 5.1.1 without changing data.

## Phase 2 — LDAP identity validation

- Search by short username.
- Exact `sAMAccountName` verification.
- Allowed-group verification.
- Read-only display of identity and current mobile-access status.

Exit criterion: `s_kholman` can be found and validated without database writes.

## Phase 3 — Credential management

- Create the minimal local credential container when absent.
- Set or replace a password through the approved Openfire API.
- Revoke only local credential material.
- Add transactions and idempotency.

Exit criterion: Android login works with a local password while Windows Spark continues to use Kerberos.

## Phase 4 — Audit and hardening

- Persistent audit table.
- CSRF validation.
- Input and output hardening.
- Provider compatibility checks.
- Clear failure messages without sensitive details.
- Integration tests and test checklist.

Exit criterion: first controlled test release.

## Phase 5 — Operational release

- Installation guide.
- Upgrade and rollback guide.
- Administrator guide.
- Versioned release artifact.
- GitHub Actions build.

Exit criterion: version `0.1.0` suitable for the test server.

## Later candidates

- self-service password changes;
- enrollment tokens and QR codes;
- time-limited access;
- second factor;
- Laravel integration;
- SMS or Telegram delivery;
- device registration and access policy.
