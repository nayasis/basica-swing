package io.github.nayasis.kotlin.swing.app.di

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Inject(val name: String = "")

