import java.nio.charset.StandardCharsets;
import java.util.Stack;

/**
 * Class to handle stack operations
 *
 * @author Zubair Abdul Matin
 */
public class StackHandler implements InstructionHandler {

    /**
     * Executes stack operations
     * @param instruction instruction
     * @param opcode opcode
     * @param virtualMachine virtual machine
     * @throws VirtualMachineException exception
     */
    @Override
    public void execute(Instruction instruction, OpCode opcode, VirtualMachine virtualMachine) throws VirtualMachineException {
        Stack<Integer> programStack = virtualMachine.getStack();
        switch (opcode) {
            case PUSH -> programStack.push(instruction.operand());
            case POP -> handlePop(programStack);
            case DUP -> handleDup(programStack);
            case SWAP -> handleSwap(programStack);
            case OVER -> programStack.push(programStack.get(programStack.size() - 2));
            case PUSH_STR -> handlePushStr(instruction, virtualMachine);
        }
    }

    /**
     * Handles Pop
     * @param programStack program stack
     * @throws VirtualMachineException exception
     */
    private void handlePop(Stack<Integer> programStack) throws VirtualMachineException {
        if (programStack.isEmpty()) { throw new VirtualMachineException("Error: No data to pop!"); }
        programStack.pop();
    }

    /**
     * Handles DUP
     * @param programStack program stack
     * @throws VirtualMachineException exception
     */
    private void handleDup(Stack<Integer> programStack) throws VirtualMachineException {
        if (programStack.isEmpty()) { throw new VirtualMachineException("Error: No data to duplicate!"); }
        programStack.push(programStack.peek());
    }

    /**
     * Handles SWAP
     * @param programStack program stack
     * @throws VirtualMachineException exception
     */
    private void handleSwap(Stack<Integer> programStack) throws VirtualMachineException {
        if (programStack.size() < 2) { throw new VirtualMachineException("Error: Nothing to swap!"); }
        int first = programStack.pop();
        int second = programStack.pop();
        programStack.push(first);
        programStack.push(second);
    }

    /**
     * Handles OVER
     * @param programStack prgram stack
     * @throws VirtualMachineException exception
     */
    private void handleOver(Stack<Integer> programStack) throws VirtualMachineException {
        if (programStack.size() < 2) { throw new VirtualMachineException("Error: Nothing to over!"); }
        programStack.push(programStack.get(programStack.size() - 2));
    }

    /**
     * Handles pushing strings
     * @param instruction instruction
     * @param virtualMachine virtual machine
     * @throws VirtualMachineException exception
     */
    private void handlePushStr(Instruction instruction, VirtualMachine virtualMachine) throws VirtualMachineException {
        if (instruction.operand() == null) {
            throw new VirtualMachineException("Error: PUSH_STR Requires a constant pool index!");
        }

        String stringValue = virtualMachine.getConstantPool().get(instruction.operand());

        int length = 0;
        byte[] stringBytes = null;
        int startingAddress = virtualMachine.getHeapPointer();
        byte[] heap = virtualMachine.getHeap();
        int totalBytes = 0;

        final int LENGTH_PREFIX = 4;

        if (!stringValue.isEmpty()) {
            stringBytes = stringValue.getBytes(StandardCharsets.US_ASCII);
            length = stringBytes.length;
            totalBytes = LENGTH_PREFIX + length;

            // check if heap is big enough
            if (startingAddress + totalBytes > heap.length) {
                throw new VirtualMachineException("Error: Heap overflow during PUSH_STR!");
            }

            // push the length values as 4 bytes
            heap[startingAddress]     = (byte) ((length >> 24) & 0xFF);
            heap[startingAddress + 1] = (byte) ((length >> 16) & 0xFF);
            heap[startingAddress + 2] = (byte) ((length >> 8)  & 0xFF);
            heap[startingAddress + 3] = (byte) (length         & 0xFF);

            // adding the rest of the characters
            for (int i = 0; i < length; i++) {
                heap[startingAddress + LENGTH_PREFIX + i] = stringBytes[i];
            }
        }

        virtualMachine.setHeapPointer(startingAddress + totalBytes - 1);
        virtualMachine.getStack().push(startingAddress);

    }
}
