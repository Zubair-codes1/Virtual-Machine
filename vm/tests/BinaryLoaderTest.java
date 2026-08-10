import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Class to test if the binary loader works correctly,
 * tests fully assembled programs and also a program that is
 * compiled first then assembled to check full compiler + assembler
 * pipeline.
 *
 * @author Zubair Abdul Matin
 */
public class BinaryLoaderTest {

    @Test
    @DisplayName("Load Factorial correctly")
    void testLoadsFactorialCorrectly() {
        BinaryLoader loader = new BinaryLoader();
        LoadedProgram program = loader.loadProgram("../output/factorial.bin");
        assertFalse(program.instructions().isEmpty());
    }

    @Test
    @DisplayName("Lod constant pool correctly from factorial program")
    void testConstantPoolLoadsCorrectly() {
        BinaryLoader loader = new BinaryLoader();
        LoadedProgram program = loader.loadProgram("../output/factorial.bin");
        assertNotNull(program.constantPool());
    }

    @Test
    @DisplayName("Test for non-existent file")
    void testNonExistentFileThrows() {
        BinaryLoader loader = new BinaryLoader();
        assertThrows(VirtualMachineException.class, () -> {
            loader.loadProgram("../output/nonexistent.bin");
        });
    }

    @Test
    @DisplayName("Test invalid magic numbers")
    void testInvalidMagicNumberThrows() throws IOException {
        File fakeFile = new File("../output/fake.bin");
        try{
            try (DataOutputStream dos = new DataOutputStream(
                    new FileOutputStream(fakeFile))) {
                dos.writeShort(0x1234); // wrong magic number
                dos.writeByte(1);
                dos.writeInt(0);
                dos.writeInt(0);
                dos.writeInt(0);
            }

            BinaryLoader loader = new BinaryLoader();
            assertThrows(VirtualMachineException.class, () -> {
                loader.loadProgram("../output/fake.bin");
            });
        }finally {
            fakeFile.delete();
        }
    }

    @Test
    @DisplayName("Test fibonacci program")
    void testFibonacciLoadsCorrectly() {
        BinaryLoader loader = new BinaryLoader();
        LoadedProgram program = loader.loadProgram("../output/fibonacci.bin");
        assertFalse(program.instructions().isEmpty());
    }

    @Test
    @DisplayName("Test hello word program")
    void testHelloWorldLoadsCorrectly() {
        BinaryLoader loader = new BinaryLoader();
        LoadedProgram program = loader.loadProgram("../output/hello_world.bin");
        assertFalse(program.instructions().isEmpty());
    }
}