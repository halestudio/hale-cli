# Migrate from the OrientDB instance store to the hale-core instance store API

Date: 2026-11-24

## Intent

hale-cli no longer compiles against the current hale-core snapshot (`6.6.0-SNAPSHOT`): `:compileGroovy` fails with 9 "unable to resolve class" errors for `eu.esdihumboldt.hale.common.instance.orient.storage.*`. This breaks the Check and Docker jobs on every open PR and the Publish run on master.

Cause: hale-core commit `3c10699a` ("refactor!: remove OrientDB based instance store", ING-4137) removed the module `eu.esdihumboldt.hale.common.instance.orient`. The SQLite based instance store is the only temporary instance store now.

## Goals

- hale-cli compiles and its tests pass against hale-core `6.6.0-SNAPSHOT`.
- `rewrite` and `split` behave as before, using the new temporary instance store.
- No OrientDB leftovers remain in the build configuration.

## Non-goals

- Changing the hale-core version pin.
- Changing command line options or the behaviour of `rewrite` / `split`.
- Touching the `--add-exports` JVM args (unclear whether they only served OrientDB).

## Anti-goals

- Hard-wiring the SQLite implementation, which would bypass the store selection via the system property `hale.instance.store` and the fallback handling in hale-core.

## Approaches

1. **Use the instance store API via `InstanceStoreExtension` (chosen).** Direct counterpart of the removed code, keeps store selection and fallback in hale-core, needs no dependency changes (`hale-app-cli` already brings `common.instance.store` and `common.instance.store.sqlite`).
2. **Pin hale-core back to 6.5.2.** Makes the build green, but only postpones the migration and gives up the snapshot that was chosen deliberately.
3. **Instantiate `SqliteInstanceStoreFactory` directly.** Works without the extension registry, but bypasses store selection and ties hale-cli to one implementation.

## Design

### `InstanceCLI.loadTempDatabase`

- Returns `eu.esdihumboldt.hale.common.instance.store.InstanceStore` instead of `LocalOrientDB`.
- Creates the store with `InstanceStoreExtension.getInstance().createStore(DataSet.SOURCE, <temp dir>, null)`. The store owns the directory and deletes it on `close()`.
- Fills it with `eu.esdihumboldt.hale.common.instance.store.StoreInstancesJob` (`name, store, instances, schema, serviceProvider = null, reports, doProcessing = false`), run with the `ConsoleProgressMonitor` as before.
- If the job does not finish with an OK status, the store is closed and an exception is thrown (my call: the old code ignored the status; failing early is safer than writing incomplete data).
- The same applies if the job reports errors: the new `StoreInstancesJob` skips instances it cannot store and only reports them, while the OrientDB based job aborted. hale-cli checks the published reports for errors to keep failing in that case (added after the final review).

### `RewriteCommand` and `SplitCommand`

- Replace `new BrowseOrientInstanceCollection(db, schema, DataSet.SOURCE)` with `store.getInstances(schema)`.
- Replace `db.delete()` with `store.close()` in the existing `finally` blocks.
- Remove the comments about OrientDB caches.
- `RewriteCommand` keeps skipping the store for pass-through writers.

### Build clean-up

- Remove the four `-Dcache.level*` JVM args from `build.gradle` (they only tuned OrientDB).
- Check the OrientDB related entry in `.trivyignore`; remove it if it no longer matches a dependency.

### Tests

- `RewriteCommandTest` must keep passing; make sure it covers the non-pass-through path that goes through the store, extend it if it does not.
- Add a `SplitCommand` test: split a small GML test data set and check that parts are written and together contain all instances. Split relies on resolving instance references from the store and has no coverage today.

## Left to the builder

- Exact shape of the temp directory handling and of the split test data (reuse existing files in `src/test/resources/testdata` where possible).
- Whether the extension registry is available in unit tests as is; if `createStore` finds no store there, find out why before considering any workaround.

## Verification

- `./gradlew build` passes locally (compile, spotless, tests).
- Manual smoke run of `rewrite` and `split` on a test data set.
