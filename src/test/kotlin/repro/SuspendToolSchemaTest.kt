package repro

import java.lang.reflect.Method
import kotlin.test.Test
import kotlin.test.assertFalse
import org.springframework.ai.mcp.annotation.method.tool.utils.McpJsonSchemaGenerator
import org.springframework.ai.util.json.schema.JsonSchemaGenerator

/**
 * Asserts the expected behaviour: the input schema of a Kotlin `suspend` tool function
 * should describe only its declared parameters, not the synthetic
 * `kotlin.coroutines.Continuation` parameter the compiler appends.
 *
 * Both tests currently fail on Spring AI 2.0.1-SNAPSHOT and 1.1.x: the schema additionally
 * contains a `$completion` property (named `arg1` when compiled without `-java-parameters`)
 * and lists it as required.
 */
class SuspendToolSchemaTest {

	private fun toolMethod(type: Class<*>): Method = type.declaredMethods.first { it.name == "fetch" }

	private fun assertNoContinuation(schema: String) {
		assertFalse(
			schema.contains("\$completion") || schema.contains("Continuation"),
			"schema leaks the synthetic Kotlin continuation parameter:\n$schema",
		)
	}

	@Test
	fun `@Tool suspend function must not leak the continuation parameter`() {
		assertNoContinuation(JsonSchemaGenerator.generateForMethodInput(toolMethod(StandardTools::class.java)))
	}

	@Test
	fun `@McpTool suspend function must not leak the continuation parameter`() {
		assertNoContinuation(McpJsonSchemaGenerator.generateForMethodInput(toolMethod(McpTools::class.java)))
	}
}
