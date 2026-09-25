package eu.purrtech.purrTechCrops;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Residence is compileOnly, so it is missing from the test classpath just like on servers without it.
 */
class HookIsolationTest {

    @Test
    void residenceIsNotOnTheClasspath() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.bekvon.bukkit.residence.containers.Flags"));
    }

    @Test
    void mainClassLoadsWithoutResidence() {
        // Initializing links and verifies the class, which fails if it eagerly needs hook dependencies.
        assertDoesNotThrow(() -> Class.forName(PurrTechCrops.class.getName(), true, getClass().getClassLoader()));
    }
}
