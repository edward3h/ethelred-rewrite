package org.ethelred.rewrite.java.logging.jul;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.commons.lang.StringEscapeUtils;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

@Value
@EqualsAndHashCode(callSuper = false)
public class FixNullMessage extends Recipe {
    private static final MethodMatcher logLevelStringThrowable = new MethodMatcher("java.util.logging.Logger log(java.util.logging.Level, String, Throwable)");
    @Option(
            displayName = "replacement message"
    )
    String replacement;

    @Override
    public String getDisplayName() {
        return "Replace a null log message argument";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                var m = super.visitMethodInvocation(method, executionContext);
                if (logLevelStringThrowable.matches(method)) {
                    var arguments = method.getArguments();
                    assert arguments.size() == 3;
                    var messageArgument = arguments.get(1);
                    if (JavaType.Primitive.Null == messageArgument.getType()) {
                        doAfterVisit(new ReplaceValue<>(messageArgument, replacement));
                    }
                }
                return m;
            }
        };
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    static class ReplaceValue<P> extends JavaIsoVisitor<P> {
        Expression scope;
        String value;

        @Override
        public J.Literal visitLiteral(J.Literal literal, P p) {
            if (getCursor().isScopeInPath(scope)) {
                return literal.withValue(value).withValueSource("\"" + StringEscapeUtils.escapeJava(value) + "\"").withType(JavaType.Primitive.String);
            }
            return literal;
        }
    }
}
