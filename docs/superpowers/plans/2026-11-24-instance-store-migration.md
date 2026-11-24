# Instance Store Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make hale-cli compile and pass its tests against hale-core `6.6.0-SNAPSHOT` by replacing the removed OrientDB store with the hale-core instance store API.

**Architecture:** `InstanceCLI.loadTempDatabase` is the only place that creates and fills the temporary store; it switches to `InstanceStoreExtension` + the new `StoreInstancesJob` and returns an `InstanceStore`. `RewriteCommand` and `SplitCommand` read from `store.getInstances(schema)` and close the store when done.

**Tech Stack:** Groovy 3 (partly `@CompileStatic`), Gradle, JUnit 4, hale-core `6.6.0-SNAPSHOT` (`eu.esdihumboldt.hale.common.instance.store`).

**Spec:** `docs/superpowers/specs/2026-11-24-instance-store-migration-design.md`

## Global Constraints

- Do not change the hale-core version pin (`hale-core = "6.6.0-SNAPSHOT"` in `gradle/libs.versions.toml`) and do not add dependencies; `hale-app-cli` already brings `common.instance.store` and `common.instance.store.sqlite`.
- Create the store only via `InstanceStoreExtension.getInstance().createStore(...)`; never reference `SqliteInstanceStoreFactory` or other classes of `...instance.store.sqlite`.
- No changes to command line options or to the behaviour of `rewrite` / `split`.
- Do not touch the `--add-exports` JVM args in `build.gradle`.
- If `createStore` finds no store in tests ("No instance store implementation available"), find out why (extension registry / `Init.init()`) before considering any workaround, and report it.
- Commits follow Conventional Commits, no `Co-authored-by` footer. The pre-commit hook runs `./gradlew spotlessCheck`; run `./gradlew spotlessApply` before committing.

## Review Focus

1. Storing the source data fails (e.g. the source iterator throws): `loadTempDatabase` must throw and close the store instead of returning a partly filled one. → test in Task 1.
2. `split` on a source with no instances (everything filtered out): exit code 0 and no part files, no exception. → test in Task 2.
3. `rewrite` with a pass-through writer creates no store and must not fail in the `finally` block (`store` is `null`). → covered by the existing `testSimpleRewrite` / `testRewriteFilter`.
4. Temporary store directory is gone after `rewrite` / `split` finished, also on failure (`store.close()` in `finally`). → checked in the Task 1 test via the failure case; for the success case by review.
5. Instances the store cannot serialize are skipped by `StoreInstancesJob` and only reported as errors in the report, the job still ends OK. No test (needs a non-serializable instance); reviewer should confirm the report reaches the `ReportHandler` passed in by both commands.

---

### Task 1: Switch the temporary store to the instance store API

**Files:**
- Modify: `src/main/groovy/to/wetransform/halecli/util/InstanceCLI.groovy` (imports, `loadTempDatabase` at the end of the class)
- Modify: `src/main/groovy/to/wetransform/halecli/data/RewriteCommand.groovy:25-26,92-118`
- Modify: `src/main/groovy/to/wetransform/halecli/data/SplitCommand.groovy:32-33,86-93,139-141`
- Test: `src/test/groovy/to/wetransform/halecli/util/InstanceCLITest.groovy` (create)

**Interfaces:**
- Consumes (hale-core, package `eu.esdihumboldt.hale.common.instance.store`):
  - `InstanceStoreExtension.getInstance().createStore(DataSet dataSet, java.nio.file.Path directory, ServiceProvider services) throws IOException` — the store owns `directory` and deletes it on `close()`.
  - `InstanceStore`: `InstanceCollection getInstances(TypeIndex types)`, `void close() throws IOException`.
  - `new StoreInstancesJob(String name, InstanceStore store, InstanceCollection instances, TypeIndex types, ServiceProvider serviceProvider, ReportHandler reportHandler, boolean doProcessing)`; `IStatus run(IProgressMonitor)`.
- Produces: `static InstanceStore InstanceCLI.loadTempDatabase(InstanceCollection instances, TypeIndex schema, ReportHandler reports = null)` — throws `IllegalStateException` and closes the store if the job status is not OK.

- [ ] **Step 1: Confirm the starting point**

Run: `./gradlew compileGroovy`
Expected: FAIL with `unable to resolve class eu.esdihumboldt.hale.common.instance.orient.storage.LocalOrientDB` (9 errors).

- [ ] **Step 2: Write the failing tests** in `InstanceCLITest.groovy` (same license header and `@BeforeClass Init.init()` as `RewriteCommandTest`)

```groovy
@Test
void testLoadTempDatabase() {
  Schema schema = /* load via SchemaCLI.loadSchema(<schemaLocation of testdata/inspire2.gml>, ...) as RewriteCommand.guessSchema does */
  InstanceCollection source = /* load testdata/inspire2.gml with that schema */
  InstanceStore store = InstanceCLI.loadTempDatabase(source, schema)
  try {
    assertEquals(4, store.getInstances(schema).size())
  } finally {
    store.close()
  }
}

@Test(expected = IllegalStateException)
void testLoadTempDatabaseFailure() {
  // DefaultInstanceCollection subclass whose iterator() throws RuntimeException
  InstanceCLI.loadTempDatabase(failingCollection, schema)
}
```

If `size()` returns `InstanceCollection.UNKNOWN_SIZE` for the store collection, count by iterating (close the `ResourceIterator`).

- [ ] **Step 3: Change `InstanceCLI.loadTempDatabase`** to the signature in *Produces*

- Temp directory: `java.nio.file.Files.createTempDirectory('hale-cli-store')`; drop the Guava `Files` import if it is unused afterwards.
- `createStore(DataSet.SOURCE, dir, null)`, then the job with `types = schema`, `serviceProvider = null`, `doProcessing = false`, run with `new ConsoleProgressMonitor()`. Keep the job name `"Load source instances into temporary database"`; the anonymous subclass with the empty `onComplete()` is no longer needed.
- If the returned `IStatus` is not `isOK()`: close the store, then throw `new IllegalStateException('Loading instances into temporary database failed: ' + status.message, status.exception)`.
- Replace the two `...orient.storage...` imports.

- [ ] **Step 4: Adapt `RewriteCommand` and `SplitCommand`**

- Variable `LocalOrientDB db` → `InstanceStore store`.
- `new BrowseOrientInstanceCollection(db, schema, DataSet.SOURCE)` → `store.getInstances(schema)`.
- `db.delete()` → `store.close()` (keep the `null` check in `RewriteCommand`).
- Remove the two-line comment "It is important that OrientDB caches are disabled …" in both files, and imports that are unused afterwards (`DataSet`, the orient imports).

- [ ] **Step 5: Run the tests**

Run: `./gradlew spotlessApply test --tests '*InstanceCLITest' --tests '*RewriteCommandTest'`
Expected: PASS, including `testRewriteNoPassthrough` (the path through the store). `testSimpleRewrite` needs network access to `wetransform.box.com`; if only that test fails because of the sandbox proxy, say so in the report instead of changing it.

- [ ] **Step 6: Commit**

```bash
git add src/main/groovy/to/wetransform/halecli src/test/groovy/to/wetransform/halecli/util/InstanceCLITest.groovy
git commit -m "fix: use hale-core instance store instead of removed OrientDB store"
```

---

### Task 2: Test coverage for `split`

**Files:**
- Test: `src/test/groovy/to/wetransform/halecli/data/SplitCommandTest.groovy` (create, modelled on `RewriteCommandTest`)
- Modify (only if a test uncovers a defect): `src/main/groovy/to/wetransform/halecli/data/SplitCommand.groovy`

**Interfaces:**
- Consumes: the `split` command as registered in `src/main/resources/plugin.xml` (same parent command as `rewrite`, so `['data', 'split']` — check the `name` attribute there); options `--data`, `--schema`, `--target <folder>`, `--threshold <n>`, `--data-filter`.
- Produces: nothing for later tasks.

- [ ] **Step 1: Write the tests**

`--schema` is required for `split` (no guessing): use the hy-p schema location from the `xsi:schemaLocation` of `testdata/inspire2.gml`, `http://inspire.ec.europa.eu/schemas/hy-p/4.0/HydroPhysicalWaters.xsd`. `inspire2.gml` contains 4 features. Use a JUnit `TemporaryFolder` rule for the target folder.

```groovy
@Test
void testSplit() {
  // args: data split --data testdata/inspire2.gml --schema <xsd> --target <folder> --threshold 2
  assertEquals(0, code)
  def parts = targetFolder.listFiles().findAll { it.name ==~ /part_\d+\.gml/ }
  def counts = parts.collect { new XmlSlurper().parse(it).featureMember.size() }
  assertEquals(4, counts.sum())
  assertTrue(counts.every { it <= 2 })
}

@Test
void testSplitNoInstances() {
  // as above plus: --data-filter "CQL:id = 'doesNotExist'"
  assertEquals(0, code)
  assertEquals(0, targetFolder.listFiles().count { it.name.endsWith('.gml') })
}
```

- [ ] **Step 2: Run them**

Run: `./gradlew spotlessApply test --tests '*SplitCommandTest'`
Expected: PASS. If a test fails, determine whether the cause is the store migration, a long-standing defect in `SplitCommand`, or a wrong assumption in the test (e.g. the element name of the members in the written parts). Fix a migration defect here; for the other two, report before changing behaviour or weakening an assertion.

- [ ] **Step 3: Commit**

```bash
git add src/test/groovy/to/wetransform/halecli/data/SplitCommandTest.groovy
git commit -m "test: cover the split command"
```

---

### Task 3: Remove OrientDB leftovers from the build

**Files:**
- Modify: `build.gradle:13-20` (`defaultJvmArgs`)
- Modify: `.trivyignore`

**Interfaces:** none.

- [ ] **Step 1: Remove the four `-Dcache.level1.*` / `-Dcache.level2.*` entries** from `defaultJvmArgs`; the two `--add-exports` entries stay.

- [ ] **Step 2: Check that OrientDB is gone from the runtime classpath**

Run: `./gradlew dependencies --configuration runtimeClasspath | grep -i orient`
Expected: no output. If something still matches, stop and report it; leave `.trivyignore` unchanged.

- [ ] **Step 3: Remove the OrientDB comment and `CVE-2017-11467`** from `.trivyignore`, keeping the first line with the documentation link.

- [ ] **Step 4: Full verification**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL (compile, spotless, all tests; see the note on network access in Task 1 Step 5).

Run: `git grep -niE "orient|cache\.level" -- . ':!docs'`
Expected: no output.

- [ ] **Step 5: Smoke run of the built application**

Run `./gradlew installDist`, then with `build/install/hale/bin/hale`: `data rewrite` on `src/test/resources/testdata/inspire2.gml` with `--target-writer eu.esdihumboldt.hale.io.wfs.fc.write-2.0`, and `data split` with `--threshold 2`.
Expected: exit code 0 and written target files for both, no warnings about the removed JVM args.

- [ ] **Step 6: Commit**

```bash
git add build.gradle .trivyignore
git commit -m "build: remove OrientDB related JVM args and Trivy exception"
```
