# Contributing

Development follows a protected-branch workflow:

- `main` contains released and production-ready code.
- `develop` contains integrated development work.
- Features use `feature/<short-name>` branches.
- Fixes use `fix/<short-name>` branches.

Every change should be submitted through a pull request. Keep commits focused, use imperative commit messages, and update English and Russian documentation together when behavior or architecture changes.

## Quality gates

Before merging, the project should pass compilation, unit tests, formatting, static analysis, and documentation checks.

## Security

Do not commit passwords, LDAP bind credentials, Kerberos keytabs, certificates, private keys, production hostnames, internal IP addresses, database dumps, or personal data.

---

# Участие в разработке

Разработка ведётся по схеме защищённых веток:

- `main` содержит выпущенный и готовый к эксплуатации код.
- `develop` содержит объединённые изменения разработки.
- Новые функции разрабатываются в ветках `feature/<краткое-название>`.
- Исправления выполняются в ветках `fix/<краткое-название>`.

Каждое изменение оформляется через Pull Request. Коммиты должны быть небольшими и осмысленными. При изменении поведения или архитектуры английская и русская документация обновляются одновременно.

## Проверки качества

Перед объединением должны успешно выполняться компиляция, модульные тесты, форматирование, статический анализ и проверка документации.

## Безопасность

Запрещено добавлять в репозиторий пароли, LDAP bind-учётные данные, Kerberos keytab, сертификаты, закрытые ключи, реальные внутренние адреса, дампы баз данных и персональные данные.
