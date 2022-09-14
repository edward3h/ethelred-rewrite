package org.ethelred.rewrite.java.logging.log4j

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class LogToNamedMethodTest : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .logCompilationWarningsAndErrors(true)
        .classpath("log4j")
        .build()

    @Test
    fun logToNamed() = assertChanged(
        recipe = LogToNamedMethod(null),
        before = """
            package test;
            
            import org.apache.logging.log4j.Level;
            import org.apache.logging.log4j.LogManager;
            import org.apache.logging.log4j.Logger;
            
            public class Test {
                private static final Logger LOGGER = LogManager.getLogger();
                
                public void doSomething() {
                    LOGGER.log(Level.DEBUG, "message");
                    
                    try {
                        LOGGER.log(Level.INFO, "something {}", 100);
                    } catch (Exception e) {
                        LOGGER.log(Level.ERROR, "error", e);
                    }
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
                
                public void doSomething() {
                    LOGGER.debug("message");
                    
                    try {
                        LOGGER.info("something {}", 100);
                    } catch (Exception e) {
                        LOGGER.error("error", e);
                    }
                }
            }
        """.trimIndent()
    )
}