# Security Policy

## Supported versions

The project is pre-release. Security fixes are applied only to the latest development version until the first stable release.

## Reporting a vulnerability

Do not disclose vulnerabilities in a public issue. Contact the repository owner privately and include:

- affected version or commit;
- reproducible steps;
- expected and actual behavior;
- potential impact;
- suggested mitigation, when known.

## Security principles

- Active Directory passwords are never stored or processed by this plugin.
- Mobile credentials are separate from AD and VPN credentials.
- Passwords must be written through Openfire authentication APIs, never as plaintext SQL.
- Administrative actions must be authorized and audited.
- Logs must not contain passwords, password hashes, bind credentials, keytabs, session tokens, or personal message content.
- LDAP is treated as the authoritative source for identity and group membership.

---

# Политика безопасности

## Поддерживаемые версии

Проект пока находится в предварительной разработке. До первого стабильного выпуска исправления безопасности применяются только к актуальной версии разработки.

## Сообщение об уязвимости

Не публикуйте сведения об уязвимости в открытом Issue. Свяжитесь с владельцем репозитория приватно и укажите:

- затронутую версию или commit;
- последовательность воспроизведения;
- ожидаемое и фактическое поведение;
- возможные последствия;
- предполагаемое исправление, если оно известно.

## Принципы безопасности

- Плагин никогда не хранит и не обрабатывает пароль Active Directory.
- Мобильный пароль отделён от учётных данных AD и VPN.
- Пароли записываются через API аутентификации Openfire, а не открытым SQL.
- Административные операции требуют авторизации и аудита.
- В журналы нельзя писать пароли, хеши, bind-учётные данные, keytab, токены сессий и содержимое сообщений.
- LDAP считается главным источником идентичности пользователя и членства в группах.
