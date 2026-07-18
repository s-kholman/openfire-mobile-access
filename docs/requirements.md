# Product Requirements

## Purpose

Openfire Mobile Access is an administrative plugin for Openfire 5.1.1 that enables local mobile credentials for users whose identity, profile and group membership are provided by Active Directory through LDAP.

## Target authentication model

- Windows domain workstations authenticate to Openfire with Kerberos/GSSAPI.
- LDAP is used only as the directory source for users, groups and vCards.
- LDAP passwords are not accepted by Openfire.
- Mobile XMPP clients authenticate with a separate local Openfire password.
- The local username must exactly match the AD `sAMAccountName`.
- Both Windows and mobile sessions use the same XMPP JID.

## Mandatory functional requirements

1. Search for a user by short username only.
2. Confirm that the matching LDAP entry exists.
3. Confirm exact equality between the requested username and LDAP `sAMAccountName`.
4. Confirm membership in the configured mobile-access AD group.
5. Show whether local mobile credentials currently exist.
6. Create or replace a local mobile password without changing LDAP data.
7. Revoke local mobile credentials without deleting the LDAP user.
8. Record all administrative operations in an audit log.
9. Never write clear-text passwords to logs or persistent storage.
10. Preserve Kerberos authentication after mobile credentials are created or revoked.

## Initial environment

- Openfire: 5.1.1
- Java: 21
- Operating system: Ubuntu 24.04 LTS
- Database: PostgreSQL
- XMPP domain: `openfire.krimm.ru`
- Active Directory domain: `krimm.local`
- LDAP user group: `CN=Openfire-Users,OU=OpenFire,DC=krimm,DC=local`

## First release scope

The first release is administrator-only and includes:

- an Openfire admin-console page;
- LDAP user validation;
- allowed-group validation;
- local credential status;
- password creation/replacement;
- access revocation;
- audit logging;
- CSRF protection;
- installation and rollback instructions.

## Out of scope for the first release

- self-service user portal;
- SMS or Telegram delivery;
- two-factor authentication;
- QR-code client provisioning;
- password expiration workflows;
- Laravel integration;
- device binding.
