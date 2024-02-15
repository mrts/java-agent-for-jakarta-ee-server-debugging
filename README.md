# Java Agent for debugging applications on Jakarta EE or Java EE servers with ByteBuddy

## Introduction

This project provides an example Java Agent that intercepts specified method
calls within applications and logs them for debugging purposes. It is designed
for running on a Jakarta EE or Java EE application server like WildFly, JBoss,
OpenLiberty, Payara or GlassFish, utilizes ByteBuddy for bytecode manipulation
and outputs debug information to `stderr` (which is usually output into
application server logs on `ERROR` level).

It is particularly useful for debugging live servers with minimal overhead as
you can intercept only the single particular method that you need to debug.

## Requirements

- Java JDK 8,
- Maven,
- ability to modify the Jakarta EE application server startup script or to pass JVM arguments to the server.

## Customizing the agent to your specific needs

To customize the Java Agent to your specific debugging needs, you must modify
the agent's method and class matching criteria by adjusting the `type` and
`method` matcher `named` arguments in the agent setup code in
`DebugLoggingAgent.java`.

### Identifying target classes and methods

1. Replace `"com.yourtargetpackage.TargetClass"` with the fully qualified name
   of the class you wish to instrument. This tells ByteBuddy which class's
methods should be intercepted.

2. Replace `"methodName"` with the name of the method you want to instrument.
   This configures ByteBuddy to only intercept calls to this specific method.

### Examples

To capture all methods of a class, use the `ElementMatchers.any()` matcher for methods:

```java
new AgentBuilder.Default()
    .type(named("com.yourtargetpackage.TargetClass"))
    .transform((builder, typeDescription, classLoader, module) -> builder
        .method(any()) // This will match all methods
        .intercept(Advice.to(YourMethodInterceptor.class)))
    .installOn(inst);
```

To capture methods that have specific parameters, use the `takesArguments` matcher:

```java
new AgentBuilder.Default()
    .type(named("com.yourtargetpackage.TargetClass"))
    .transform((builder, typeDescription, classLoader, module) -> builder
        .method(named("methodName").and(takesArguments(int.class, String.class))) // Adjust the argument types as needed
        .intercept(Advice.to(YourMethodInterceptor.class)))
    .installOn(inst);
```

ByteBuddy's `ElementMatchers` provide a powerful API for defining complex
matching criteria, including custom matchers, logical operations (and, or), and
more. For advanced scenarios, you might use matchers like `isAnnotatedWith` to
target methods annotated with a specific annotation or `isPublic` to only
intercept public methods etc.

### Applying changes

After modifying the agent setup code to capture the required methods, rebuild
the agent JAR and deploy it as described in the following sections.

## Building the agent

Build the agent with:

```sh
mvn package
```

This will generate `server-debugging-agent-1.0.0.jar` and
`server-debugging-agent-1.0.0-jar-with-dependencies.jar` JAR files in the
`target/` directory.

### JAR with dependencies

For the Java agent to function correctly, ByteBuddy must be included within the
agent's JAR file. This ensures that all required classes are available to the
JVM at runtime, preventing `ClassNotFoundException` or `NoClassDefFoundError`.

The project uses Maven's _maven-assembly-plugin_ to bundle the agent and
ByteBuddy into a single JAR, commonly referred to as a "fat JAR" or "uber JAR".
The suffix `-with-dependencies` distinguishes this JAR from the one that only
contains the compiled agent code without the dependencies.

## Deploying the agent

To deploy the agent, modify the application server startup script of or set the
JVM argument directly to include the `-javaagent` option that points to the JAR
with dependencies:

```sh
-javaagent:/path/to/server-debugging-agent-1.0.0-jar-with-dependencies.jar
```

For example, in WildFly or JBoss `standalone.conf`, add:

```sh
JAVA_OPTS="$JAVA_OPTS -javaagent:/path/to/server-debugging-agent-1.0.0-jar-with-dependencies.jar"
```

## Integration with application server logging system

There is no easy way to use a logger in an agent as the application server
logging system is uninitialized during agent loading. Therefore the agent
outputs debug information directly to `stderr`. It is common for application
server logging system to intercept `stderr` messages and log them on `ERROR`
level to the server logs by default.
