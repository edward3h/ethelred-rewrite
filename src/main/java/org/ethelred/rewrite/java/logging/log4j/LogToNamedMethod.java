package org.ethelred.rewrite.java.logging.log4j;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = false)
public class LogToNamedMethod extends Recipe {
    private static final MethodMatcher LOG_METHOD = new MethodMatcher("org.apache.logging.log4j.Logger log(org.apache.logging.log4j.Level, ..");

    @Override
    public @NotNull String getDisplayName() {
        return "Convert a .log(Level, ...) method to the equivalent level named method";
    }

    @Option(displayName = "Mapping of level name to method name", required = false)
    Map<String, String> levelMap;

    public LogToNamedMethod() {
        this.levelMap = new HashMap<>();
    }

    public LogToNamedMethod(@Nullable Map<String, String> levelMap) {
        if (levelMap == null) {
            this.levelMap = new HashMap<>();
        }
        else
        {
            this.levelMap = new HashMap<>(levelMap);
        }
    }

    public void setLevelMap(@Nullable Map<String, String> levelMap) {
        if (levelMap != null) {
            this.levelMap.putAll(levelMap);
        }
    }

    @Override
    protected @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                var m = super.visitMethodInvocation(method, executionContext);
                if (LOG_METHOD.matches(m)) {
                    var levelArg = m.getArguments().get(0);
                    if (levelArg instanceof J.FieldAccess) {
                        var levelName = ((J.FieldAccess) levelArg).getName().getSimpleName();
                        var methodName = _getMethodName(levelName);
                        var methodType = m.getMethodType();
                        if (methodType != null) {
                            methodType = methodType.withName(methodName);
                        }
                        var args = new ArrayList<>(m.getArguments());
                        args.remove(0);
                        var first = args.remove(0);
                        args.add(0, first.withPrefix(Space.EMPTY));
                        m = m.withName(m.getName().withSimpleName(methodName))
                                .withMethodType(methodType)
                                .withArguments(args);
                    }
                }
                return m;
            }
        };
    }

    @NonNull
    private String _getMethodName(@NonNull String levelName) {
        return levelMap.computeIfAbsent(levelName, String::toLowerCase);
    }
}
