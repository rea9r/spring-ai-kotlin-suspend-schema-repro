package repro

import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.McpToolParam
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam

/**
 * A tool exposed through the standard `@Tool` path (spring-ai-model). The Spring AI MCP
 * server bridges these `ToolCallback`s to MCP, so this is the path most Kotlin MCP
 * servers actually use.
 */
class StandardTools {

	@Tool(description = "Fetch a URL (Kotlin suspend function)")
	suspend fun fetch(@ToolParam(description = "the url to fetch") url: String): String = url
}

/**
 * The same tool declared through the annotation-driven `@McpTool` path
 * (spring-ai-mcp-annotations).
 */
class McpTools {

	@McpTool(description = "Fetch a URL (Kotlin suspend function)")
	suspend fun fetch(@McpToolParam(description = "the url to fetch") url: String): String = url
}
