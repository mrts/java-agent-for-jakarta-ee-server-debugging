package org.test;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class DebugLoggingAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        new AgentBuilder.Default()
                .type(named("com.yourtargetpackage.TargetClass")) // <- Replace this with the class you want to observe
                .transform(((builder, typeDescription, classLoader, javaModule, protectionDomain) -> builder
                        .method(named("methodName")) // <- Replace this with the method you want to observe
                        .intercept(Advice.to(LoggingMethodInterceptor.class))
                ))
                .installOn(inst);
    }

    public static class LoggingMethodInterceptor {

        /**
         * Log the argument and call stack trace to stderr.
         *
         * There is no easy way to use a logger in an agent as the WildFly
         * logging system is uninitialized during agent loading.
         *
         * @param arg method argument
         */
        @Advice.OnMethodEnter
        public static void enter(@Advice.Argument(0) Object arg) {
            // Use stderr as there is no easy way to use a logger in an agent, you will get
            // "WFLYLOG0078: The logging subsystem requires the log manager to be org.jboss.logmanager.LogManager. The subsystem has not be initialized and cannot be used."
            // See https://stackoverflow.com/a/22168660
            System.err.println("[DebugLoggingAgent] Method called with argument: " + arg);

            // Use an exception to capture the call stack.
            final Exception e = new Exception("[DebugLoggingAgent] Current call stack:");
            e.printStackTrace(System.err);
        }
    }

}

