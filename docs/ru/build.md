# Сборка и установка

## Базовая совместимость

Проект наследуется от официального родительского POM для плагинов Openfire версии `5.1.1`.
Целевой байткод — Java 17. Это соответствует настройке компилятора в исходном коде Openfire 5.1.1 и позволяет запускать плагин на Java 21, которая используется на целевом сервере.

## Сборка

```bash
mvn clean verify
```

Openfire-сборка создаётся в файле:

```text
target/mobileaccess-openfire-plugin-assembly.jar
```

Перед установкой файл необходимо переименовать в соответствии с Maven `artifactId`:

```bash
cp target/mobileaccess-openfire-plugin-assembly.jar target/mobileaccess.jar
```

## Установка

Файл `mobileaccess.jar` можно загрузить через административную консоль Openfire или скопировать в каталог `plugins/` установленного Openfire.
Openfire автоматически распакует и загрузит архив плагина.

## Структура исходного кода

```text
plugin.xml
pom.xml
src/main/java/
src/main/resources/
src/main/web/      # будет добавлен при реализации интерфейса администратора
```

Главный класс реализует `org.jivesoftware.openfire.container.Plugin`. Метод `destroyPlugin()` обязан освободить все ресурсы и ссылки, удерживаемые плагином.

## Использованные первичные источники

- исходный код Openfire 5.1.1: `plugins/pom.xml`;
- исходный код Openfire 5.1.1: `Plugin.java`;
- официальное руководство Openfire Plugin Developer Guide.
