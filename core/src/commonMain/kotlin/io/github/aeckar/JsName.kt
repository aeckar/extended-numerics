package io.github.aeckar

import kotlin.annotation.AnnotationTarget.*

/**
 * Gives a declaration (function, property or class) a specific name in JavaScript.
 *
 * Multiplatform implementation of `kotlin.js.JsName`.
 */
@Target(CLASS, FUNCTION, PROPERTY, CONSTRUCTOR, PROPERTY_GETTER, PROPERTY_SETTER)
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
public expect annotation class JsName(val name: String) // TODO remove if not needed