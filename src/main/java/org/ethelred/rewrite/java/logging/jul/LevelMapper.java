package org.ethelred.rewrite.java.logging.jul;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.java.ChangeFieldName;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.Map;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
public class LevelMapper extends Recipe {
    private static final String LEVEL_CLASS = "java.util.logging.Level";
    private static final Pattern LEVEL_PATTERN = Pattern.compile(LEVEL_CLASS.replaceAll("\\.", "\\."));

    @Override
    public String getDisplayName() {
        return "Map log levels with different names";
    }

    @Option(displayName = "Mapping of old level name to new level name")
    Map<String, String> levelMap;

    @Override
    public Validated validate() {
        return Validated.test("levelMap", "must be a non-empty Map", levelMap, m -> m != null && !m.isEmpty());
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext executionContext) {
                String fieldAccessSimpleName = fieldAccess.getSimpleName();
                if (levelMap.containsKey(fieldAccessSimpleName) && fieldAccess.getTarget().getType().isAssignableFrom(LEVEL_PATTERN)) {
                    doAfterVisit(new ChangeFieldName<>(LEVEL_CLASS, fieldAccessSimpleName, levelMap.get(fieldAccessSimpleName)));
                }
                return super.visitFieldAccess(fieldAccess, executionContext);
            }
        };
    }
}
