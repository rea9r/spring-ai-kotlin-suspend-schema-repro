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

## Affected code paths

| Annotation | Generator | Module |
| --- | --- | --- |
| `@Tool` | `org.springframework.ai.util.json.schema.JsonSchemaGenerator#generateForMethodInput` | `spring-ai-model` |
| `@McpTool` | `org.springframework.ai.mcp.annotation.method.tool.utils.McpJsonSchemaGenerator#generateForMethodInput` | `spring-ai-mcp-annotations` |

The Spring AI MCP server exposes `@Tool` `ToolCallback`s over MCP, so the `@Tool` path
applies to Kotlin MCP servers as well, not only to direct chat-model tool calling.

## Verified

| Version | `@Tool` path | `@McpTool` path |
| --- | --- | --- |
| `2.0.1-SNAPSHOT` (main) | reproduced | reproduced |
| `1.1.8` (release) | reproduced | n/a — the `@McpTool` annotation path was introduced in 2.0.x |

## How to run

```bash
./gradlew run     # prints the generated schemas for both paths (main / 2.0.1-SNAPSHOT)
./gradlew test    # asserts the continuation parameter is absent — fails on current versions
```

The version can be overridden within the 2.0.x line, e.g. `./gradlew run -PspringAiVersion=2.0.0`.

## Why it matters

`$completion` is not a real tool parameter — it is an artifact of how Kotlin compiles
`suspend` functions to JVM bytecode. It is added to the schema's `required` array, so the
model is asked to provide an argument that has no meaning to it and is not part of the
tool's input.

Kotlin `suspend` functions are not supported as tools today: there is no coroutine handling
in the tool path, and tool methods are invoked via `Method.invoke` (in `MethodToolCallback` /
`AbstractMcpToolMethodCallback`). This reproduction covers the schema-generation side of that
gap; broader `suspend` support is requested in #3718.

## Root cause

Both generators iterate the method parameters and skip framework-supplied parameter types
(`ToolContext`, and the MCP infrastructure types in the MCP generator), but they do not skip
the trailing synthetic `Continuation` parameter of a `suspend` function. There is no
suspend/coroutine handling anywhere in the tool path.

## A possible fix

Skip the trailing `kotlin.coroutines.Continuation` parameter when
`org.springframework.core.KotlinDetector.isSuspendingFunction(method)` is `true` (guarded by
`KotlinDetector.isKotlinReflectPresent()`), in both `JsonSchemaGenerator` and
`McpJsonSchemaGenerator`. This corrects the generated schema independently of whether tool
invocation gains full coroutine support.
