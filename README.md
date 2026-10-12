# basica-swing

Basic Swing library based on Kotlin. A Swing port of [basicafx-kt](https://github.com/nayasis/basicafx-kt) without JavaFX.

## Dependency

```kotlin
dependencies {
    implementation("io.github.nayasis:basica-swing:0.1.0-SNAPSHOT")
    // pick a look and feel; the library does not set one
    implementation("com.formdev:flatlaf:3.7.2")
}
```

## Quick start

```kotlin
class MyApp: SwingApp(Main::class) {
    override fun setupLookAndFeel() { FlatIntelliJLaf.setup() }
}

class Main: SwingView("My App") {
    override val root = JPanel(BorderLayout())
}

fun main(args: Array<String>) = SwingApp.launch(MyApp::class, *args)
```

## Packages

| package | contents |
|---|---|
| `app` | `SwingApp`, `SwingView`, `Environment` (application.yml + `key=value` args), `LoggerConfig`, `SimpleDiContainer` / `@Inject` |
| `misc` | `runLater`, `runAsync`, `runAwait`, `runUiAndWait`, coroutine helpers (`launchAsync`, `launchUi`, `withUi`, `withBackground`), `Desktop`, image helpers on `BufferedImage`, `WebBrowser` (OkHttp) |
| `window` | `Dialog` (alert/confirm/error/prompt/file pickers/progress), `Windows`, borderless window support (`WindowHeader`, `WindowMover`, `WindowResizer`), `BoundaryChecker`, `Localizator` |
| `window.progress` | `ProgressDialog`, `MultiProgressDialog` |
| `preloader` | `DefaultPreloader`, `DefaultSplash` |
| `control` | `ItemComboBox`, text mask, `ListTableModel` + `JTable.bind()` (header sort), `JTable` helpers |
| `collection` | `FilterSortList` |
| `property` | serializable window/table state: `WindowProperty`, `TableProperty`, `InsetProperty`, ... |
