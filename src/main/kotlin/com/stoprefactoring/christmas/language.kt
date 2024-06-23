package com.stoprefactoring.christmas

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

private const val  BUNDLE = "message.text"

object Language:DynamicBundle(BUNDLE){
    fun text(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = getMessage(key, *params)
}