@file:Suppress("unused")

package io.github.nayasis.kotlin.swing.property

import com.fasterxml.jackson.annotation.JsonIgnore
import io.github.nayasis.kotlin.swing.misc.runLater
import io.github.nayasis.kotlin.swing.window.MaximizedProperty
import io.github.nayasis.kotlin.swing.window.fillsCurrentScreen
import io.github.nayasis.kotlin.swing.window.isMaximized
import io.github.nayasis.kotlin.swing.window.previousBoundary
import io.github.nayasis.kotlin.swing.window.previousZoomInset
import io.github.nayasis.kotlin.swing.window.runWhenShown
import java.awt.Component
import java.awt.Container
import java.awt.Window
import java.io.Serializable
import javax.swing.AbstractButton
import javax.swing.JCheckBoxMenuItem
import javax.swing.JComboBox
import javax.swing.JList
import javax.swing.JMenu
import javax.swing.JPasswordField
import javax.swing.JRadioButtonMenuItem
import javax.swing.JTable
import javax.swing.JToggleButton
import javax.swing.RootPaneContainer
import javax.swing.text.JTextComponent
import kotlin.reflect.KClass

/**
 * persisted state of a window and of the components in it that have a name ([Component.setName]).
 *
 * passwords are never stored: [JPasswordField] is skipped.
 */
data class WindowProperty(
    var inset: InsetProperty?                  = null,
    var maximized: Boolean                     = false,
    var previousZoomInset: InsetProperty?      = null,
    var previousBoundary: MaximizedProperty?   = null,
    var tables: HashMap<String,TableProperty>? = null,
    var checks: HashMap<String,Boolean>?       = null,
    var values: HashMap<String,String>?        = null,
    var lists: HashMap<String,List<Int>>?      = null,
    var visibles: HashMap<String,Boolean>?     = null,
    var editables: HashMap<String,Boolean>?    = null,
    var disables: HashMap<String,Boolean>?     = null,
    var indices: HashMap<String,Int>?          = null,
): Serializable {

    private data class WindowStateSnapshot(
        val maximized: Boolean,
        val hasNormalBoundary: Boolean,
        val normalBoundary: InsetProperty,
    )

    @JsonIgnore
    var includeKlass = ArrayList<KClass<out Component>>()
    @JsonIgnore
    var excludeKlass = ArrayList<KClass<out Component>>()
    @JsonIgnore
    var includeId = HashSet<String>()
    @JsonIgnore
    var excludeId = HashSet<String>()
    @JsonIgnore
    val includeObject = HashSet<Component>()
    @JsonIgnore
    val excludeObject = HashSet<Component>()

    constructor(window: Window, includeChildren: Boolean = true): this() {
        read(window, includeChildren)
    }

    fun read(window: Window, includeChildren: Boolean = true) {

        tables    = hashMapOf()
        checks    = hashMapOf()
        values    = hashMapOf()
        lists     = hashMapOf()
        visibles  = hashMapOf()
        editables = hashMapOf()
        disables  = hashMapOf()
        indices   = hashMapOf()

        val currentInset = InsetProperty(window)
        val snapshot = readWindowState(window, currentInset)

        inset = currentInset
        maximized = snapshot.maximized
        previousZoomInset = window.previousZoomInset
        previousBoundary = MaximizedProperty().apply {
            maximized = snapshot.maximized && snapshot.hasNormalBoundary
            boundary = snapshot.normalBoundary
        }

        if(!includeChildren) return

        namedComponents(window).forEach {
            if( skippable(it) ) return@forEach
            val id = it.name
            when(it) {
                is JTable                    -> tables?.set(id, TableProperty(it))
                is JCheckBoxMenuItem,
                is JRadioButtonMenuItem,
                is JToggleButton             -> checks?.set(id, (it as AbstractButton).isSelected)
                is JPasswordField            -> {}
                is JTextComponent            -> values?.set(id, it.text ?: "")
                is JComboBox<*>              -> indices?.set(id, it.selectedIndex)
                is JList<*>                  -> lists?.set(id, it.selectedIndices.toList())
            }
            visibles?.set(id, it.isVisible)
            disables?.set(id, !it.isEnabled)
            if (it is JTextComponent && it !is JPasswordField) {
                editables?.set(id, it.isEditable)
            }
        }

    }

    @Suppress("UNCHECKED_CAST")
    fun bind(window: Window?, visibility: Boolean = true, includeChildren: Boolean = true) {

        if( window == null ) return

        bindWindowState(window)
        previousZoomInset?.let { window.previousZoomInset = it }

        if(!includeChildren) return

        namedComponents(window).forEach {
            if( skippable(it) ) return@forEach
            val id = it.name
            when(it) {
                is JTable                    -> tables?.get(id)?.bind(it)
                is JCheckBoxMenuItem,
                is JRadioButtonMenuItem,
                is JToggleButton             -> checks?.get(id)?.let { value -> (it as AbstractButton).isSelected = value }
                is JPasswordField            -> {}
                is JTextComponent            -> values?.get(id)?.let { value -> it.text = value }
                is JComboBox<*>              -> indices?.get(id)?.let { value ->
                    if (it.itemCount > 0) it.selectedIndex = value.coerceIn(0, it.itemCount - 1)
                }
                is JList<*>                  -> lists?.get(id)?.let { value ->
                    it.selectedIndices = value.filter { index -> index in 0 until it.model.size }.toIntArray()
                }
            }
            if( visibility ) {
                visibles?.get(id)?.let { value -> it.isVisible = value }
                disables?.get(id)?.let { value -> it.isEnabled = !value }
                if (it is JTextComponent && it !is JPasswordField) {
                    editables?.get(id)?.let { value -> it.isEditable = value }
                }
            }
        }

    }

    // every component with a name, in tree order. table and combo internals are not entered, and neither
    // are the panes a JRootPane names by itself ("null.contentPane" ...).
    private fun namedComponents(window: Window): List<Component> {
        val root  = (window as? RootPaneContainer)?.rootPane
        val found = ArrayList<Component>()
        fun walk(component: Component) {
            if (!component.name.isNullOrEmpty() && component.name != "${root?.name}.contentPane") found.add(component)
            when (component) {
                is JMenu                   -> component.menuComponents.forEach { walk(it) }
                is JTable, is JComboBox<*> -> {}
                is Container               -> component.components.forEach { walk(it) }
            }
        }
        if (root == null) window.components.forEach { walk(it) }
        else listOfNotNull(root.contentPane, root.jMenuBar).forEach { walk(it) }
        return found
    }

    private fun readWindowState(window: Window, currentInset: InsetProperty): WindowStateSnapshot {
        val previousNormalBoundary = window.previousBoundary.boundary.copy()
        val hasPreviousNormalBoundary = previousNormalBoundary.width > 0 && previousNormalBoundary.height > 0
        val likelyMaximized =
            window.isMaximized ||
            window.previousBoundary.maximized ||
            (hasPreviousNormalBoundary && window.fillsCurrentScreen(currentInset))
        return WindowStateSnapshot(
            maximized = likelyMaximized,
            hasNormalBoundary = hasPreviousNormalBoundary,
            normalBoundary = if(hasPreviousNormalBoundary) previousNormalBoundary else currentInset.copy(),
        )
    }

    private fun bindWindowState(window: Window) {
        if(!maximized) {
            inset?.bind(window)
            window.isMaximized = false
            return
        }

        resolveNormalBoundary()?.let { normalBoundary ->
            window.previousBoundary = normalBoundary.toMaximizedProperty()
            window.runWhenShown {
                restoreMaximized(window, normalBoundary)
            }
        }
    }

    private fun resolveNormalBoundary(): InsetProperty? {
        return previousBoundary?.boundary
            ?.takeIf { it.width > 0 && it.height > 0 }
            ?.copy()
            ?: inset?.copy()
    }

    private fun InsetProperty.toMaximizedProperty(): MaximizedProperty {
        return MaximizedProperty().apply {
            maximized = true
            boundary = copy()
        }
    }

    // maximizing right after show can land on the wrong screen area, so it is verified and retried
    private fun restoreMaximized(window: Window, normalBoundary: InsetProperty, attempts: Int = 5, prime: Boolean = true) {
        if(prime) {
            window.isMaximized = false
            normalBoundary.bind(window)
            runLater {
                restoreMaximized(window, normalBoundary, attempts, false)
            }
            return
        }

        window.previousBoundary = normalBoundary.toMaximizedProperty()
        window.isMaximized = true

        runLater {
            if(window.fillsCurrentScreen()) {
                return@runLater
            }
            if(attempts <= 0) {
                return@runLater
            }

            window.isMaximized = false
            normalBoundary.bind(window)
            runLater {
                restoreMaximized(window, normalBoundary, attempts - 1, false)
            }
        }
    }

    private fun skippable(component: Component): Boolean {
        val id = component.name.orEmpty()
        if( excludeKlass.isNotEmpty() && excludeKlass.any { it.isInstance(component) } ) return true
        if( includeKlass.isNotEmpty() && includeKlass.none { it.isInstance(component) } ) return true
        if( excludeId.isNotEmpty() && id in excludeId) return true
        if( includeId.isNotEmpty() && id !in includeId) return true
        if( excludeObject.isNotEmpty() && component in excludeObject) return true
        if( includeObject.isNotEmpty() && component !in includeObject) return true
        return false
    }

}
