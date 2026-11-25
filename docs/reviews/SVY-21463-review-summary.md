# Review Summary — SVY-21463: Modernize plugin: drop bundled jars, migrate to networknt validator 2.0.1, Tycho/Java 21, add tests

**Risk: LOW.** The review started at MODERATE because the `sj.jsonschemavalidation` 3.0.0 bundle now resolves its Jackson and networknt packages from the host product instead of bundled jars, and `core` and `ui` depend on it non-optionally. A manual check in an installed 2026.12 developer build (`Svy2026_12_28_09_2026`) closed that risk:
- The product ships `sj.jsonschemavalidation_3.0.0`, `com.networknt.json-schema-validator_2.0.1`, `com.ethlo.time.itu_1.14.0`, Jackson 2.22.x (including dataformat-yaml) and slf4j-api 2.0.19, all listed in `bundles.info`.
- The IDE starts normally and its `.log` has no resolution errors.
- Adding `"bogusKey": 1` to the `visible` property in `bootstrapcomponents/button/button.spec` and building produced JSON Problem markers on the correct line (24). Validation works end to end on networknt.

The remaining items below are minor follow-ups.

## Scope reviewed
- JSON-Schema-Validation-Eclipse: `5106d08` (36 files, +2642/-192), origin/master.
- servoy-eclipse: `5383d8d05` (`launch_targets/com.servoy.eclipse.target.target` only, +112/-104), origin/master. The same commit also carries unrelated bumps (Chromium/CEF, BouncyCastle, byte-buddy, antlr4, java-jwt, snakeyaml) and about 20 reordered blocks.

## Release-target check: INDETERMINATE
- servoy-eclipse, Fix Version 2026.12: MATCH. origin/master is 2026.12.0 rc and carries the commit.
- JSON-Schema-Validation-Eclipse (extension package, own version line): no Jira comment names an extension version. The reviewer confirmed in chat that 3.0.0 is the intended version for 2026.12. The 3.0.0 jar is already published to developer.servoy.com/mvn_repository.

## Manual test plan
1. Build the plugin repo (Tycho, Java 21) and run the 12 tests in `JsonValidatorTest` and `SpecFileValidationTest`.
2. Start the product with the 3.0.0 bundle. `ss sj` in the OSGi console should show ACTIVE, with no resolution errors in the Error Log.
3. In a resources project, add a bogus key to a component `.spec`. Expect one marker on the right line, cleared after the fix.
4. Create a sibling `FooSchema.json` and `Foo.json` with a wrong type. Expect a marker at the right line.
5. Compare markers for one broken `.spec` per error class against the `lts_2026` build: missing required, wrong type in a `oneOf` branch, extra property, duplicate key, trailing `}`.
6. Add a sibling schema with an unresolvable `$ref`. The build should not lose validation of the other files.
7. Validate a schema without `$schema` that uses draft-04 idioms (`exclusiveMinimum: true`).
8. Check an empty `.spec` and an empty `.json` file.
9. Use a schema with `{"$ref":"http://localhost:<port>/x"}` and a listener on that port. Check whether the IDE makes the request.
10. Run each exporter launch (war, solution, mobile) once.
11. Smoke test the designer on Windows, macOS and Linux (Chromium bump), and run a DLTK JavaScript parse.
12. Time a clean build of a resources project with many `.spec` files.

Automated: `mvn clean verify` in JSON-Schema-Validation-Eclipse. In servoy-eclipse: `mvn clean verify -Pproduct` and `mvn -Ptarget`.

This plan was also posted to the Jira issue (comment 235146).

## Possible improvements / follow-ups
- Exporter launches: the four dev launches under `com.servoy.eclipse.exporter.solution/launch_files/` (`automaticAdd=false`) select `sj.jsonschemavalidation` but list no networknt, itu or Jackson entries. Use Validate Plug-ins on them, or remove them if unused. The product itself is confirmed fine.
- `JsonValidator.validate` does not guard `getSchema` or `validate`, and `ValidateJson.checkResource` catches only `IOException` and `CoreException`. A bad `$ref` or an invalid `pattern` in a sibling schema can escape the builder. Consider catching `RuntimeException` and reporting it as a marker.
- Marker messages: the old validator combined nested `oneOf`/`anyOf` detail into one message, and the new one emits one flat marker per networknt error. Observed in the manual check: a single bogus key inside a property definition produced three markers on the same line: `must be valid to one and only one schema, but 0 are valid` (the `oneOf` summary), `object found, string expected` (the plain-string branch, noise), and `property 'bogusKey' is not defined ...` (the real error, listed last). Consider dropping the parent `oneOf` error and the branch errors that don't apply. Only `groupingtable.spec` is tested.
- Gotcha for manual testing: the validator is a project builder. With Build Automatically off, saving a `.spec` shows no markers until Project → Build Project.
- Draft default is draft-07 when `$schema` is missing. The code comment says this matches the old validator, which was not verified.
- Unrelated dependency bumps share the commit. `servoy-extensions` pins bcprov 1.85.2 while the IDE target now has 1.86. At `5383d8d05` alone, the target said 3.0.0 while `feature.xml` still said 2.0.1, so cherry-picking that commit alone would break the build.
- Security, unresolved: whether networknt 2.0.1's default loader fetches remote or `file:` `$ref`s from schemas in downloaded packages (test step 9), and ReDoS via a schema `pattern` (the old validator had the same exposure). No CVE check was done on the BouncyCastle or java-jwt bumps.
- The `SchemaRegistry` is rebuilt and the spec schema recompiled on every file validation.
- `lts_2026` still has 2.0.1 in the target, so later lts-to-master merges will conflict in the target file.
