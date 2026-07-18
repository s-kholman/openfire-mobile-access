# Architecture

## Overview

The plugin adds an administrative control plane for local mobile credentials while Openfire continues to obtain employee identity data from Active Directory.

```text
Windows Spark
    -> SASL GSSAPI / Kerberos
    -> Openfire
    -> user identity from LDAP

Mobile XMPP client
    -> password-based SASL
    -> DefaultAuthProvider
    -> local credential fields in ofUser
    -> user identity from LDAP
```

The same username and JID are used for both paths.

## Provider model

Target Openfire configuration:

```text
User Provider:   LDAP, optionally wrapped by HybridUserProvider for a local emergency admin
Group Provider:  LDAP
VCard Provider:  LDAP
Auth Provider:   DefaultAuthProvider
Kerberos:        SASL GSSAPI, configured independently of AuthProvider
```

`LdapAuthProvider` must not be part of the final authentication chain.

## Core components

### MobileAccessPlugin

Openfire plugin lifecycle entry point. It initializes services, database schema and admin-console integration.

### LdapIdentityService

Validates that:

- the LDAP entry exists;
- `sAMAccountName` exactly equals the normalized requested username;
- the account belongs to the configured mobile-access group;
- the account is suitable for access according to configured policy.

It must not validate an AD password.

### MobileCredentialService

Coordinates credential creation, replacement and revocation. It does not expose password material after the operation completes.

### CredentialRepository

Performs narrowly scoped database operations for the local credential container. It must not overwrite LDAP-owned profile attributes such as display name or email.

### AuditService

Records administrative actions, result, actor, target username and remote address. Passwords and password-derived values must never be logged.

### Admin web layer

Provides Openfire admin-console pages with CSRF protection and administrator authorization.

## Credential storage strategy

Openfire's `DefaultAuthProvider` stores password verification data in `ofUser`. LDAP remains the authoritative source for the user profile.

For an LDAP user, the plugin may create a minimal shadow row keyed by the same username. The row exists only to hold local authentication material.

The implementation must verify the exact Openfire 5.1.1 database contract before enabling writes. Direct generation of password hashes is avoided when a supported Openfire API can be used safely.

## Username rules

Accepted:

```text
s_kholman
```

Rejected:

```text
KRIMM\\s_kholman
s_kholman@krimm.local
s_kholman@openfire.krimm.ru
```

The normalized username must match LDAP `sAMAccountName` exactly under the configured case policy.

## Local emergency administrator

A fully local emergency administrator is a separate concern. If required, it is exposed through `HybridUserProvider` with LDAP as primary and `DefaultUserProvider` as secondary.

The employee mobile credential feature must never create arbitrary local-only identities.

## Future extension points

- self-service password change;
- short-lived enrollment tokens;
- QR-code provisioning;
- second factor;
- Laravel integration;
- mobile access expiration;
- device-level access policy.
