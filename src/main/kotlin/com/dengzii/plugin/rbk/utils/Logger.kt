package com.dengzii.plugin.rbk.utils

import com.intellij.openapi.diagnostic.LogLevel

object Logger {
    private val logger by lazy {
        com.intellij.openapi.diagnostic.Logger.getInstance(Logger::class.java).apply {
            setLevel(LogLevel.DEBUG)
        }
    }

    fun info(msg: String) {
        logger.info(msg)
    }

    fun warn(msg: String) {
        logger.warn(msg)
    }

    fun error(msg: String) {
        logger.error(msg)
    }

    fun error(e: Throwable) {
        logger.error(e)
    }




}