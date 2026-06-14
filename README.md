# Spring AI — Kotlin `suspend` tool functions leak the continuation parameter into the input schema

A Kotlin `suspend` function annotated as a tool has its compiler-appended
`kotlin.coroutines.Continuation` parameter included in the generated JSON input schema. It
appears as a property named `$completion` (or `arg1` when compiled without
`-java-parameters`) and is listed as `required`.

```kotlin
@Tool(description = "Fetch a URL (Kotlin suspend function)")
suspend fun fetch(@ToolParam(description = "the url to fetch") url: String): String = url
```

compiles to `fetch(url: String, $completion: Continuation)` on the JVM, and the generated schema becomes:

```json
{
  "type": "object",
  "properties": {
    "url": { "type": "string", "description": "the url to fetch" },
    "$completion": { "type": "object" }
  },
  "required": [ "url", "$completion" ],
  "additionalProperties": false
}
```

This affects both the `@Tool` path (`JsonSchemaGenerator`) and the `@McpTool` path
(`McpJsonSchemaGenerator`). Since the Spring AI MCP server exposes `@Tool` `ToolCallback`s
over MCP, the `@Tool` path applies to Kotlin MCP servers as well, not only to direct
chat-model tool calling.

## Run

```bash
./gradlew run     # prints the generated schemas for both paths
./gradlew test    # asserts the continuation parameter is absent (fails until the fix ships)
```

Defaults to `2.0.1-SNAPSHOT` (main); override within the 2.0.x line with `-PspringAiVersion=2.0.0`.

## Reproduced on

| Version | `@Tool` | `@McpTool` |
| --- | --- | --- |
| `2.0.1-SNAPSHOT` (main) | reproduced | reproduced |
| `1.1.8` | reproduced | n/a — the `@McpTool` annotation path was introduced in 2.0.x |

Full analysis and fix: https://github.com/spring-projects/spring-ai/pull/6418
