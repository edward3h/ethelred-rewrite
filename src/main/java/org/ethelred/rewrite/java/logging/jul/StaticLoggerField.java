package org.ethelred.rewrite.java.logging.jul;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.logging.AddLogger;
import org.openrewrite.java.logging.LoggingFramework;
import org.openrewrite.java.search.FindFieldsOfType;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class StaticLoggerField extends Recipe {
    private static final MethodMatcher getLoggerString = new MethodMatcher("java.util.logging.Logger getLogger(String)");
    private static final Set<MethodMatcher> logMMs =
            Stream.of("log config entering exiting fine finer finest info severe throwing warning".split("\\s+"))
                    .map(methodName -> new MethodMatcher("java.util.logging.Logger " + methodName + "(..)"))
                    .collect(Collectors.toSet());

    @Option(displayName = "Logger name",
            description = "The name of the logger to use when generating a field.",
            required = false)
    @Nullable
    String loggerName;
    LoggingFramework loggingFramework = LoggingFramework.JUL;

    @Override
    public String getDisplayName() {
        return "Static Logger field";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaVisitor<>() {
            @Override
            public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                doAfterVisit(new UsesMethod<>(getLoggerString));
                return cu;
            }
        };
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation m = super.visitMethodInvocation(method, executionContext);

                if (logMMs.stream().anyMatch(mm -> mm.matches(method)) && m.getSelect() != null && getLoggerString.matches(m.getSelect())) {
                    return useLoggerField(m);
                }

                return m;
            }

            private J.MethodInvocation useLoggerField(J.MethodInvocation m) {
                J.ClassDeclaration clazz = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
                Set<J.VariableDeclarations> loggers = FindFieldsOfType.find(clazz, loggingFramework.getLoggerType());
                if (!loggers.isEmpty()) {
                    JavaType.Method methodType = null;
                    String variableName = loggers.iterator().next().getVariables().get(0).getName().toString();
                    JavaType.FullyQualified variableType = JavaType.ShallowClass.build(loggingFramework.getLoggerType());
                    if (m.getMethodType() != null) {
                        maybeRemoveImport(m.getMethodType().getDeclaringType());

                        Set<Flag> flags = new LinkedHashSet<>(m.getMethodType().getFlags());
                        flags.remove(Flag.Static);
                        methodType = m.getMethodType().withDeclaringType(variableType).withFlags(flags);
                    }

                    m = m.withSelect(new J.Identifier(randomId(),
                            m.getSelect() == null ?
                                    Space.EMPTY :
                                    m.getSelect().getPrefix(),
                            Markers.EMPTY,
                            variableName,
                            variableType,
                            null)
                    ).withMethodType(methodType);

                } else {
                    doAfterVisit(AddLogger.addLogger(clazz, loggingFramework, loggerName == null ? "LOGGER" : loggerName));
                    doAfterVisit(this);
                }
                return m;
            }
        };
    }
}
