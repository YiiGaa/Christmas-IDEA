package com.stoprefactoring.christmas.base

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

private const val  BUNDLE = "messages.text"

object Language:DynamicBundle(BUNDLE){
    fun text(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = getMessage(key, *params)
}