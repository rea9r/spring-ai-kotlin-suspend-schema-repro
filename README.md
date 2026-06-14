# Spring AI — Kotlin `suspend` tool functions leak the continuation parameter into the input schema

A Kotlin `suspend` function annotated as a tool has its compiler-appended
`kotlin.coroutines.Continuation` parameter included in the generated JSON input schema. It
appears as a property named `$completion` (or `arg1` when compiled without
`-java-parameters`) and is listed as `required`.

```kotlin
@Tool(description = "Fetch a URL (Kotlin suspend function)")
suspend fun fetch(@ToolParam(description = "the url to fetch") url: String): String = url
```

compiles to `fetch(String, Continuation)` on the JVM, and the generated schema becomes:

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

- The model is asked to provide a `$completion` argument that has no meaning to it. On the
  `@Tool` path the schema also sets `additionalProperties: false`, so it is internally
  inconsistent (a required property the caller cannot legitimately supply).
- Tool invocation runs through `MethodToolCallback` / `AbstractMcpToolMethodCallback`, which
  call `Method.invoke`. A real `Continuation` is never supplied, so a `suspend` tool cannot
  be invoked successfully either.

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
