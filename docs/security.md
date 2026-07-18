# Security Model

## Security objectives

- Compromise of a mobile device must not disclose the user's Active Directory password.
- Openfire must not accept LDAP passwords after migration to the final provider configuration.
- Mobile access can be granted and revoked independently of the AD password.
- A local mobile credential cannot be created for an identity that does not exist in LDAP.
- Administrative actions are attributable and auditable.

## Trust boundaries

1. Active Directory and LDAPS are authoritative for employee identity and group membership.
2. Kerberos is authoritative for Windows domain SSO.
3. Openfire PostgreSQL stores local mobile credential verification material.
4. The plugin admin page is restricted to authenticated Openfire administrators.
5. Mobile clients are untrusted endpoints.

## Mandatory controls

### LDAP validation

Before any credential write, the plugin must:

- perform a fresh LDAP lookup;
- require exactly one matching user;
- compare `sAMAccountName` with the requested username;
- verify membership in the configured allowed group;
- fail closed on LDAP errors or ambiguous results.

### Password handling

- use masked password fields;
- require confirmation for manually entered passwords;
- enforce configurable minimum length;
- never log passwords;
- never return password material after the response that generated it;
- clear transient password arrays when practical;
- prefer Openfire's supported credential API over custom hashing.

### Database writes

- use prepared statements;
- write only the required credential columns;
- never copy LDAP profile data into the local row unless technically required;
- use transactions for multi-step operations;
- make repeated grant/revoke operations idempotent.

### Administrative web security

- require Openfire administrator authorization;
- use POST for state changes;
- enforce CSRF protection;
- validate all inputs server-side;
- escape output in JSP pages;
- avoid including sensitive values in URLs.

### Audit

Audit events include:

- timestamp;
- administrator JID or username;
- target username;
- action;
- result;
- remote address;
- non-sensitive failure reason.

Audit events never include passwords, SCRAM keys, salts or complete LDAP bind credentials.

## Revocation semantics

Revocation clears only local mobile credential material. It must not:

- delete the LDAP identity;
- modify AD;
- remove LDAP groups;
- break Kerberos/GSSAPI authentication.

Optionally, active XMPP sessions may be terminated after revocation. Session termination behavior must be explicit and tested.

## Failure policy

The plugin fails closed when:

- LDAP is unavailable;
- the allowed group cannot be verified;
- more than one LDAP identity matches;
- the username contains unsupported domain or JID syntax;
- the database transaction fails;
- the configured Openfire provider model is incompatible.

## Deployment policy

The first deployment must occur on the test Openfire server. Before installation:

- back up `openfire.xml`;
- create a PostgreSQL dump;
- verify a working emergency administrator path;
- document rollback;
- test with one account (`s_kholman`) before wider use.
