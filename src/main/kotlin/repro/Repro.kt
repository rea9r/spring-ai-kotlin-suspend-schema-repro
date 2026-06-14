package repro

import java.lang.reflect.Method
import org.springframework.ai.mcp.annotation.method.tool.utils.McpJsonSchemaGenerator
import org.springframework.ai.util.json.schema.JsonSchemaGenerator

private fun toolMethod(type: Class<*>): Method = type.declaredMethods.first { it.name == "fetch" }

private fun jvmParams(method: Method): String =
	method.parameters.joinToString(", ") { "${it.name}: ${it.type.simpleName}" }

fun main() {
	val standard = toolMethod(StandardTools::class.java)
	val mcp = toolMethod(McpTools::class.java)

	println("Compiled JVM signature of `suspend fun fetch(url: String)`:")
	println("    fetch(${jvmParams(standard)})")
	println()

	println("=== @Tool      JsonSchemaGenerator.generateForMethodInput ===")
	println(JsonSchemaGenerator.generateForMethodInput(standard))
	println()

	println("=== @McpTool   McpJsonSchemaGenerator.generateForMethodInput ===")
	println(McpJsonSchemaGenerator.generateForMethodInput(mcp))
}
