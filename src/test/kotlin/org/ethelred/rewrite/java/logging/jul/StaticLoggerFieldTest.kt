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

    @Test
    fun testUseFieldNotVariable() = assertChanged(
        recipe = StaticLoggerField("LOGGER"),
        before = """
            import java.util.logging.Logger;
            import java.util.logging.Level;
            
            public class Main {
                public void run() {
                    try {
                        System.out.println("something");
                    }
                    catch (Exception e)
                    {
                         Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
                    }
                }
            
                public static void main(String[] args) {
                    Logger root = Logger.getLogger("");
                    root.setLevel(Level.ALL);
                    Main app = new Main();
                    app.run();
                }
            }
        """.trimIndent(),
        after = """
            import java.util.logging.Logger;
            import java.util.logging.Level;
            import java.util.logging.LogManager;
            
            public class Main {
                private static final Logger LOGGER = LogManager.getLogger("Main");
            
                public void run() {
                    try {
                        System.out.println("something");
                    }
                    catch (Exception e)
                    {
                         LOGGER.log(Level.SEVERE, null, e);
                    }
                }
            
                public static void main(String[] args) {
                    Logger root = Logger.getLogger("");
                    root.setLevel(Level.ALL);
                    Main app = new Main();
                    app.run();
                }
            }
        """.trimIndent()
    )

}