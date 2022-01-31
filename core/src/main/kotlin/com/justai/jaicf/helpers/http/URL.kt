package com.justai.jaicf.helpers.http

fun String.toUrl() = this.replace("(?<=[^:\\s])(/+/)".toRegex(), "/")

fun String.withTrailingSlash(with: Boolean = true) = removeSuffix("/") + if (with) "/" else ""
