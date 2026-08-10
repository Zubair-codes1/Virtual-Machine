import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Class to test the full virtual machine workflow, including
 * binary files that are assembled to only, and those that go through
 * the full compiler -> asssembler -> binary execution flow.
 *
 * @author Zubair Abdul Matin
 */
public class VirtualMachineTest {

    @Test
    @DisplayName("Testing compiler program run on virutal machine")
    void testCompilerExecutionFull() {
        VirtualMachine vm = new VirtualMachine(false);
        vm.loadFromBinary("../output/compilerTest.bin");
        assertDoesNotThrow(vm::executeProgram);
    }

    @Test
    @DisplayName("Testing factorial for errors")
    void testFactorialExecutesWithoutError() {
        VirtualMachine vm = new VirtualMachine(false);
        vm.loadFromBinary("../output/factorial.bin");
        assertDoesNotThrow(vm::executeProgram);
    }

    @Test
    @DisplayName("Testing fibonacci for errors")
    void testFibonacciExecutesWithoutError() {
        VirtualMachine vm = new VirtualMachine(false);
        vm.loadFromBinary("../output/fibonacci.bin");
        assertDoesNotThrow(vm::executeProgram);
    }

    @Test
    @DisplayName("Testing hello world for errors")
    void testHelloWorldExecutesWithoutError() {
        VirtualMachine vm = new VirtualMachine(false);
        vm.loadFromBinary("../output/hello_world.bin");
        assertDoesNotThrow(vm::executeProgram);
    }

    @Test
    @DisplayName("Testing storage of global variables")
    void testGlobalVariableStoredCorrectly() {
        VirtualMachine vm = new VirtualMachine(false);
        vm.loadFromBinary("../output/factorial.bin");
        vm.executeProgram();
        assertNotNull(vm.getGlobalVariables());
    }

    @Test
    @DisplayName("Testing invalid binary files")
    void testInvalidBinaryThrows() {
        VirtualMachine vm = new VirtualMachine(true);
        assertThrows(VirtualMachineException.class, () -> {
            vm.loadFromBinary("../output/nonexistent.bin");
        });
    }

    @Test
    @DisplayName("Testing for stack emptiness after halt has run")
    void testStackIsEmptyAfterHalt() {
        VirtualMachine vm = new VirtualMachine(false);
        vm.loadFromBinary("../output/factorial.bin");
        vm.executeProgram();
        assertTrue(vm.getStack().isEmpty());
    }
}
