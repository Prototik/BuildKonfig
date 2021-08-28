package com.codingfeline.buildkonfig.compiler

import com.codingfeline.buildkonfig.compiler.generator.BuildKonfigCompiler
import com.codingfeline.buildkonfig.compiler.generator.FileAppender
import java.io.File

class BuildKonfigEnvironment(
    private val data: BuildKonfigData
) {

    sealed class CompilationStatus {
        object Success : CompilationStatus()
        class Failure(val errors: List<String>) : CompilationStatus()
    }

    fun generateConfigs(logger: Logger): CompilationStatus {
        val errors = ArrayList<String>()

        val writer = writer@{ fileName: String ->
            val file = File(fileName)
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
            }
            return@writer file.writer()
        }

        if (data.hasTargetSpecificConfigs) {
            compileExpectActual(data, writer, logger)
        } else {
            compileCommonObject(data, writer, logger)
        }

        return if (errors.isEmpty()) {
            CompilationStatus.Success
        } else {
            CompilationStatus.Failure(errors)
        }
    }

    private fun compileCommonObject(data: BuildKonfigData, writer: FileAppender, logger: Logger): List<String> {
        val errors = mutableListOf<String>()
        try {
            BuildKonfigCompiler.compileCommonObject(
                data.objectProperties,
                data.commonConfig,
                data.hasJsTarget,
                writer,
                logger
            )
        } catch (e: Throwable) {
            e.message?.let { errors.add(it) }
        }
        return errors
    }

    private fun compileExpectActual(data: BuildKonfigData, writer: FileAppender, logger: Logger): List<String> {
        val errors = mutableListOf<String>()
        try {
            BuildKonfigCompiler.compileCommon(
                data.objectProperties,
                data.commonConfig,
                writer,
                logger
            )
        } catch (e: Throwable) {
            e.message?.let { errors.add(it) }
        }

        data.targetConfigs.filter { it.config != null }
            .forEach { config ->
                try {
                    BuildKonfigCompiler.compileTarget(
                        data.objectProperties,
                        config,
                        writer,
                        logger
                    )
                } catch (e: Throwable) {
                    e.message?.let { errors.add(it) }
                }
            }
        return errors
    }
}
