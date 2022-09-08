package org.ethelred.rewrite.java.logging.jul

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class StaticLoggerFieldTest : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .logCompilationWarningsAndErrors(true)
        .build()

    @Test
    fun testFieldExistsAndIsUsed() = assertUnchanged(
        recipe = StaticLoggerField("LOGGER"),
        before = """
            import java.util.logging.LogManager;
            import java.util.logging.Logger;
            
            public class Test {
                private static final Logger LOGGER = LogManager.getLogger("Test");
                
                void test() {
                    LOGGER.info("Just testing");
                }
            }
        """
    )

    @Test
    fun testFieldExistsAndIsNotUsed() = assertChanged(
        recipe = StaticLoggerField("LOGGER"),
        before = """
            import java.util.logging.LogManager;
            import java.util.logging.Logger;
            
            public class Test {
                private static final Logger LOGGER = LogManager.getLogger("Test");
                
                void test() {
                    Logger.getLogger(Test.class.getName()).info("Just testing");
                }
            }
        """,
        after =  """
            import java.util.logging.LogManager;
            import java.util.logging.Logger;
            
            public class Test {
                private static final Logger LOGGER = LogManager.getLogger("Test");
                
                void test() {
                    LOGGER.info("Just testing");
                }
            }
        """
    )

    @Test
    fun testFieldDoesNotExist() = assertChanged(
        recipe = StaticLoggerField("LOGGER"),
        before = """
            import java.util.logging.Logger;
            
            public class Test {
                void test() {
                    Logger.getLogger(Test.class.getName()).info("Just testing");
                }
            }
        """,
        after =  """
            import java.util.logging.LogManager;
            import java.util.logging.Logger;
            
            public class Test {
                private static final Logger LOGGER = LogManager.getLogger("Test");
            
                void test() {
                    LOGGER.info("Just testing");
                }
            }
        """
    )

}