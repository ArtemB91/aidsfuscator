# Java 25 and Spring fork

This branch preserves four changes tested against a Java 25 Spring Boot application.
The upstream base is [v2.10.0](https://github.com/LvStrnggg/aidsfuscator/tree/v2.10.0),
commit `7bea4cc05b98b492c55986070428e1a6bdf1f750`.
New upstream commits are not included automatically.

## Changes

1. The CLI does not call `UpdateChecker` at startup. The build tool can run without
   an outbound update request.
2. `OptimizationProcessor` skips `IntegerReuseOptimizationPass` and
   `UnusedLocalVariableCleanTransformer`. These passes caused class export failures
   on the tested Java 25 application. This branch disables the passes rather than
   restoring the affected input classes unchanged.
3. For ordinary classes, `ClassRenameTransformer` adds the configured prefix within
   the original package. Use a prefix without `.` or `/`, such as `a`, to preserve
   package access. Mixin naming keeps its upstream behavior.
4. `LocalVariableNameTransformer` clears local variable tables but retains
   `MethodParameters`. Spring can still read parameter names through reflection.

These changes do not provide Spring keep rules. Application configurations must
preserve the names and metadata used by reflection and external contracts.

## Build

Use JDK 25 and Maven 3.9.11:

```sh
mvn --batch-mode -DskipTests package
```

The CLI jar is
`aidsfuscator-main/target/aidsfuscator-obfuscator-1.0-SNAPSHOT.jar`.
The build compiles the upstream test fixtures but does not run behavioral tests.
For a build with a prepared Maven cache, add `--offline`.

See the [upstream usage instructions](README.md#how-to-use) for the CLI layout.

## Verification limits

The class exporter can log a class-write failure and continue. A zero exit code
does not prove that every output class is valid. Check the log and verify the
output with the JVM. Test the same application class files that are packaged for
delivery.

Random transformations can produce different output on each run. Keep the tool
commit, configuration, private mappings, and output hashes for each application
build. This fork does not certify arbitrary inputs or configurations.

The pinned upstream tree has no license file. This fork does not add a license
for upstream code.
