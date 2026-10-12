# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

basica-swing is a Kotlin Swing library: application base, dialogs, borderless windows, table binding and window-state persistence. It is a port of `basicafx-kt` (JavaFX) at `C:\project_nayasis\basicafx-kt`, branch `feature/remove-tornadofx`.

**Tech stack**: Kotlin 2.2.20, JDK 25 toolchain, Swing/AWT, kotlinx-coroutines (with `Dispatchers.Swing`), OkHttp, basica-kt.

No JavaFX, no TornadoFX-style DSL. The port deliberately dropped JavaFX-only and node-lookup helpers (find-by-id, children walkers, CSS style maps). Do not reintroduce them; components that need persisting are identified by `Component.name`.

The library is look-and-feel agnostic. FlatLaf (`FlatIntelliJLaf`) is a **test-only** dependency used by tests and demo apps through `common/TestCommons.kt:installTheme()`. Use `UIManager` colors/icons in library code instead of hardcoded looks.

## Build Commands

```bash
./gradlew build
./gradlew test
./gradlew test --tests "io.github.nayasis.kotlin.swing.collection.FilterSortListTest"
./gradlew publishToMavenLocal
```

### JVM target

Kotlin 2.2.20 cannot emit JVM 25 bytecode. The build runs on the JDK 25 toolchain but targets 24 (`jvmTarget = JVM_24`, `options.release = 24`). Raise both together once Kotlin supports 25.

### TLS behind the corporate proxy

Downloading new dependencies can fail with `PKIX path building failed`: the JDK truststore lacks the proxy CA. Trust the Windows store for that run:

```bash
./gradlew build "-Dorg.gradle.jvmargs=-Xmx2g -Djavax.net.ssl.trustStoreType=Windows-ROOT"
```

## Architecture

```
app/        SwingApp (launch, LAF, DI, Environment, logging, primary JFrame), SwingView, di/
misc/       threading (SwingThreads, SwingAsync), Desktop, Images, Transferables, WebBrowser, UserAgents
window/     Dialog, Windows (Window extensions), BoundaryChecker, Borderless, WindowHeader,
            WindowMover, WindowResizer, IconContainer, Localizator, progress/
preloader/  DefaultPreloader, DefaultSplash, Notificator
control/    TextFields (mask), combobox/ItemComboBox, table/ (ListTableModel, TableBinding, Tables)
collection/ FilterSortList
property/   InsetProperty, PositionProperty, SizeProperty, TableProperty, WindowProperty, ...
```

### Threading

- All Swing access happens on the EDT. `runLater` queues, `runUiAndWait` blocks until done (direct call when already on the EDT).
- `runAsync` runs on `Dispatchers.IO` inside `asyncScope` and returns `Deferred`. Failures go to the default uncaught exception handler on the EDT; `await()` still rethrows.
- `runAwait` on the EDT spins a `SecondaryLoop`, so the UI keeps painting while it waits.
- `shutdownAsync()` waits for running tasks then cancels; `SwingApp` calls it on exit.
- Every `Dialog` entry point is callable from any thread.

### Images

`BufferedImage` is the single image type. `String.toBufferedImage()` accepts http(s) URLs, `data:...;base64,` URIs and file paths. Use `java.util.Base64`, not basica-kt's `encodeBase64`/`decodeBase64`: those (de)serialize Java objects, not raw bytes.

### Progress dialogs

`AbstractProgressDialog` creates its window lazily because a `JDialog` owner is fixed at construction. Call `initOwner`/`initModality` before first use; afterwards they throw.

### Tables

`ListTableModel<S>(FilterSortList<S>)` + `JTable.bind(model)`: header click cycles asc / desc / none, ctrl-click adds a secondary key. Sort state is `ListTableModel.sortKeys` (`ColumnSortKey(columnId, ascending)`). Hidden columns are removed from the `TableColumnModel` and kept in a registry so `TableProperty` can restore them.

### DI

`@Inject` classes in the app's package are created by `SimpleDiContainer.scanPackages()` from `SwingApp`. Views are not beans: `SwingApp.find()` creates and caches them.

## Testing

- JUnit 5 + Kotest assertions.
- Tests that need a display call `assumeDisplay()` and skip when headless.
- `*Demo` files (`SwingAppDemo`, `DialogDemo`, `ProgressDialogDemo`, `BorderlessWindowDemo`, `SplashDemo`, `TableDemo`) are manual apps with `main`; they assert nothing and Gradle doesn't run them.
- Kotest infix gotcha: `a > 0 shouldBe true` parses as `a > (0 shouldBe true)`. Parenthesize the comparison.
