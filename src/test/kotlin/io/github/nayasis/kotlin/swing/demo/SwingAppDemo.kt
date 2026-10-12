package io.github.nayasis.kotlin.swing.demo

import io.github.nayasis.kotlin.swing.app.SwingApp
import io.github.nayasis.kotlin.swing.app.SwingView
import io.github.nayasis.kotlin.swing.common.installTheme
import io.github.nayasis.kotlin.swing.preloader.DefaultPreloader
import io.github.nayasis.kotlin.swing.preloader.DefaultSplash
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

/** manual demo: run `main` by hand. it asserts nothing. */
fun main() {
    DefaultPreloader.set(DemoSplash::class)
    SwingApp.launch(DemoApp::class)
}

class DemoSplash: DefaultSplash(527, 297, "/test.png")

class DemoApp: SwingApp(DemoView::class) {
    override fun setupLookAndFeel() = installTheme()
}

class DemoView: SwingView("test view") {

    override val root = JPanel(BorderLayout()).apply {
        add(JLabel("test label"), BorderLayout.NORTH)
        add(JTextField(20), BorderLayout.CENTER)
    }

}
