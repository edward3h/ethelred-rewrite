package org.ethelred.rewrite.java.logging.jul

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class JulToLog4j2Test : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .logCompilationWarningsAndErrors(true)
        .classpath("log4j")
        .build()

    override val recipe: Recipe = Environment.builder()
        .scanRuntimeClasspath("org.openrewrite.java.logging")
        .build()
        .activateRecipes("org.ethelred.rewrite.java.logging.jul.JulToLog4j2")

    @Test
    fun myExampleIWantToWork() = assertChanged(
        before = """
            package test;
            import java.util.logging.Level;
            import java.util.logging.Logger;
            
            public class Test {
                public void trySomething() {
                    try {
                        doSomething();
                    }
                    catch(Exception e) {
                        Logger.getLogger(Test.class.getName()).log(Level.SEVERE, "Error", e);
                    }
                }
                
                public void doSomething() {
                    // not really
                }
            }
        """.trimIndent(),
        after = """
            package test;
            import org.apache.logging.log4j.Level;
            import org.apache.logging.log4j.LogManager;
            import org.apache.logging.log4j.Logger;
            
            public class Test {
                private static final Logger LOGGER = LogManager.getLogger();
            
                public void trySomething() {
                    try {
                        doSomething();
                    }
                    catch(Exception e) {
                        LOGGER.error("Error", e);
                    }
                }
                
                public void doSomething() {
                    // not really
                }
            }
            
        """.trimIndent()
    )

    @Test
    fun example2() = assertChanged(
        before = """
            package test;
            import java.util.logging.Level;
            import java.util.logging.Logger;
            
            public class Test {
                public void trySomething() {
                    try {
                        doSomething();
                    }
                    catch(Exception e) {
                        Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, e);
                    }
                }
                
                public void doSomething() {
                    // not really
                }
            }
        """.trimIndent(),
        after = """
            package test;
            import org.apache.logging.log4j.Level;
            import org.apache.logging.log4j.LogManager;
            import org.apache.logging.log4j.Logger;
            
            public class Test {
                private static final Logger LOGGER = LogManager.getLogger();
            
                public void trySomething() {
                    try {
                        doSomething();
                    }
                    catch(Exception e) {
                        LOGGER.error("Unknown", e);
                    }
                }
                
                public void doSomething() {
                    // not really
                }
            }
            
        """.trimIndent()
    )
}