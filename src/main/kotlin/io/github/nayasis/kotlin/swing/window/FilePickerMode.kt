package io.github.nayasis.kotlin.swing.window

/**
 * how a file picker dialog behaves.
 */
enum class FilePickerMode {
    /** pick a single existing file */
    Single,
    /** pick multiple existing files */
    Multi,
    /** pick a destination to save to */
    Save,
}
