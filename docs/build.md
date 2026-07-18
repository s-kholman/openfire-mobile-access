# Build and deployment

## Compatibility baseline

The project inherits from the official Openfire plugins parent POM version `5.1.1`.
The bytecode target is Java 17, which matches the compiler target configured by Openfire 5.1.1 and remains runnable on the Java 21 runtime used by the target server.

## Build

```bash
mvn clean verify
```

The Openfire assembly is created as:

```text
target/mobileaccess-openfire-plugin-assembly.jar
```

Before deployment, rename it to match the Maven artifact ID:

```bash
cp target/mobileaccess-openfire-plugin-assembly.jar target/mobileaccess.jar
```

## Deployment

Deploy `mobileaccess.jar` through the Openfire Admin Console or copy it to the Openfire `plugins/` directory.
Openfire expands and loads plugin archives automatically.

## Source layout

```text
plugin.xml
pom.xml
src/main/java/
src/main/resources/
src/main/web/      # added when the Admin Console UI is implemented
```

The entry class implements `org.jivesoftware.openfire.container.Plugin` and must release all retained resources in `destroyPlugin()`.

## References

- Openfire 5.1.1 source code: `plugins/pom.xml`
- Openfire 5.1.1 source code: `Plugin.java`
- Official Openfire Plugin Developer Guide
