import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Vritual Machine class that controls the FDE cylce.
 * Connects the entire virtual machine together and holds
 * all the critical fields such as the program counter, stack, call stack
 * and more.
 *
 * @author Zubair Abdul Matin
 */
public class VirtualMachine {
    private int programCounter;
    private Stack<Integer> programStack;
    private Stack<Frame> callStack;
    private byte[] heap;
    private int heapPointer;
    private ArrayList<String> programStorage;
    private ArrayList<Instruction> executableInstructions;
    private Map<String, Integer> labels;
    private TreeMap<Integer, String> addressToLabel;
    private Map<String, Integer> globalVariables;
    private ArrayList<String> constantPool;
    private final Map<String, OpCode> opCodeMapper;
    private boolean isRunning;
    private boolean isDebugging;

    /**
     * Virtual machine constructor
     */
    public VirtualMachine(boolean isDebugging) {
        this.programCounter = 0;
        this.programStack = new Stack<>();
        this.callStack = new Stack<>();
        this.heap = new byte[5000];
        this.heapPointer = 0;
        this.programStorage = new ArrayList<>();
        this.executableInstructions = new ArrayList<>();
        this.labels = new HashMap<>();
        this.addressToLabel = new TreeMap<>();
        this.globalVariables = new HashMap<>();
        this.constantPool = new ArrayList<>();
        this.opCodeMapper = new HashMap<>();

        setOpCodeMapper();

        this.isRunning = true;
        this.isDebugging = isDebugging;
    }

    /**
     * Maps strings to opcode
     */
    private void setOpCodeMapper() {
        // System and Control
        opCodeMapper.put("HALT", OpCode.HALT);
        opCodeMapper.put("NOOP", OpCode.NOOP);
        opCodeMapper.put("DUMP_STACK", OpCode.DUMP_STACK);

        // stack manipulation
        opCodeMapper.put("PUSH", OpCode.PUSH);
        opCodeMapper.put("POP", OpCode.POP);
        opCodeMapper.put("DUP", OpCode.DUP);
        opCodeMapper.put("SWAP", OpCode.SWAP);
        opCodeMapper.put("OVER", OpCode.OVER);
        opCodeMapper.put("PUSH_STR", OpCode.PUSH_STR);

        // Arithmetic operations
        opCodeMapper.put("ADD", OpCode.ADD);
        opCodeMapper.put("SUB", OpCode.SUB);
        opCodeMapper.put("MULT", OpCode.MULT);
        opCodeMapper.put("DIV", OpCode.DIV);
        opCodeMapper.put("MOD", OpCode.MOD);
        opCodeMapper.put("LSHFT", OpCode.LSHIFT);
        opCodeMapper.put("RSHFT", OpCode.RSHIFT);
        opCodeMapper.put("INC_LOCAL", OpCode.INC_LOCAL);
        opCodeMapper.put("DEC_LOCAL", OpCode.DEC_LOCAL);

        // Logic and comparison
        opCodeMapper.put("EQ", OpCode.EQ);
        opCodeMapper.put("NEQ", OpCode.NEQ);
        opCodeMapper.put("GT", OpCode.GT);
        opCodeMapper.put("LT", OpCode.LT);
        opCodeMapper.put("GTE", OpCode.GTE);
        opCodeMapper.put("LTE", OpCode.LTE);
        opCodeMapper.put("AND", OpCode.AND);
        opCodeMapper.put("OR", OpCode.OR);
        opCodeMapper.put("XOR", OpCode.XOR);
        opCodeMapper.put("NOT", OpCode.NOT);

        // Branching and Subroutines
        opCodeMapper.put("JUMP", OpCode.JUMP);
        opCodeMapper.put("JIT", OpCode.JIT);
        opCodeMapper.put("JIF", OpCode.JIF);
        opCodeMapper.put("CALL", OpCode.CALL);
        opCodeMapper.put("RET", OpCode.RET);

        // Memory
        opCodeMapper.put("LOAD", OpCode.LOAD);
        opCodeMapper.put("STORE", OpCode.STORE);
        opCodeMapper.put("STORE_LOCAL", OpCode.STORE_LOCAL);
        opCodeMapper.put("LOAD_LOCAL", OpCode.LOAD_LOCAL);
        opCodeMapper.put("ALLOC", OpCode.ALLOC);
        opCodeMapper.put("LOAD_HEAP", OpCode.LOAD_HEAP);
        opCodeMapper.put("STORE_HEAP", OpCode.STORE_HEAP);

        // I/O
        opCodeMapper.put("PRINT", OpCode.PRINT);
        opCodeMapper.put("PRINT_CHAR", OpCode.PRINT_CHAR);
        opCodeMapper.put("INPUT", OpCode.INPUT);
        opCodeMapper.put("PRINT_STR", OpCode.PRINT_STR);
    }

    /**
     * Loading directly from an asm file.
     * This is legacy code that I left here for future compatibility purposes
     * @param filePath .asm file path
     */
    public void loadFromFile(String filePath) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            // Pass the RAW lines, including blanks and comments
            loadProgramIntoStorage(new ArrayList<>(lines));
            System.out.println("Program loaded successfully from: " + filePath);
        } catch (IOException e) {
            System.err.println("Error: Could not read file at " + filePath);
        }
    }

    /**
     * Loads directly from the binary file produces by the assembler
     * @param filePath file path of binary file
     */
    public void loadFromBinary(String filePath) {
        BinaryLoader loader = new BinaryLoader();
        LoadedProgram program = loader.loadProgram(filePath);
        this.executableInstructions = program.instructions();
        this.constantPool = new ArrayList<>(program.constantPool());

    }

    /**
     * Loads program into storage.
     * This is also legacy code designed for the loadFromFile function.
     * @param lines lines of .asm code
     */
    public void loadProgramIntoStorage(ArrayList<String> lines) {
        programStorage.clear();
        programStack.clear();
        labels.clear();
        globalVariables.clear();
        constantPool.clear();
        programCounter = 0;
        isRunning = true;

        programStorage.addAll(lines);

        int instructionCounter = 0;

        // Pass 1: Map Labels
        for (int i = 0; i < programStorage.size(); i++) {
            String line = programStorage.get(i).trim();

            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                continue;
            }

            if (line.startsWith(":")) {
                labels.put(line.substring(1).toUpperCase(), instructionCounter);
                addressToLabel.put(instructionCounter, line.substring(1).toUpperCase());
            } else {
                instructionCounter++;
            }
        }

        // Pass 2: Create Executable Instructions
        for (int i = 0; i < programStorage.size(); i++) {
            String line = programStorage.get(i).trim();

            // Skip labels, blanks, and comments
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";") || line.startsWith(":")) {
                continue;
            }

            Instruction instruction = decode(line, i + 1);
            executableInstructions.add(instruction);
        }

    }

    /**
     * Executes the entire program
     */
    public void executeProgram() {

        try {
            while (programCounter < executableInstructions.size() && isRunning) {
                Instruction instruction = fetchInstruction();
                // The Null Safety Check
                if (instruction == null) {
                    throw new VirtualMachineException("Null Instruction at address " + programCounter + ".");
                }

                if (instruction.opcode().getScope().equals(ScopeCategory.LOCAL) && callStack.isEmpty()) {
                    throw new VirtualMachineException("No Frame to be accessed!");
                }

                executeOneStep(instruction);
                if (isDebugging) {
                    debug(instruction);
                }

                programCounter++;
            }
        } catch (VirtualMachineException e) {
            System.err.println("\n[VM EXECUTION ERROR]: " + e.getMessage());
            printFunctionStackTrace();
        }catch (Exception e) {
            System.err.println("[INTERNAL EXECUTION ERROR]: " + e.getMessage() + " at line number " + programCounter + ".");
            printFunctionStackTrace();
        } finally {
            isRunning = false;
        }

    }

    /**
     * Fetches one instruction from the executable instructions
     * Used as a helper method.
     * @return Instruction
     */
    private Instruction fetchInstruction() {
        if (!executableInstructions.isEmpty() && programCounter < executableInstructions.size()) {
            return executableInstructions.get(programCounter);
        }
        return null;
    }

    /**
     * Decodes an instruction string into an Instruction
     * @param instruction instruction string
     * @param lineNumber line number of instruction
     * @return Instruction record
     */
    private Instruction decode(String instruction, Integer lineNumber) {
        instruction = instruction.trim();
        // Using \\s+ handles multiple spaces between words in a text file
        String[] instructionParts = instruction.split("\\s+");
        String command = instructionParts[0].toUpperCase().trim();

        OpCode opcode = opCodeMapper.get(command);
        if (opcode == null) {
            throw new VirtualMachineException("Unknown command: " + command);
        }

        // SAFETY CHECK: Only try to get operand if the array has more than 1 part
        String operandStr = (instructionParts.length > 1) ? instructionParts[1].trim() : null;

        // Handle Label prefixing
        if (operandStr != null && operandStr.startsWith(":")) {
            operandStr = operandStr.substring(1);
        }

        // PUSH Logic
        if (opcode.equals(OpCode.PUSH)) {
            if (operandStr == null) throw new VirtualMachineException("PUSH requires a number");
            return new Instruction(opcode, Integer.parseInt(operandStr), lineNumber);
        }

        // LOAD/STORE Logic
        if (
                opcode.equals(OpCode.LOAD) || opcode.equals(OpCode.STORE)
        ) {
            if (operandStr == null) throw new VirtualMachineException(command + " requires a variable name");
            if (!constantPool.contains(operandStr)) {
                constantPool.add(operandStr);
            }
            return new Instruction(opcode, constantPool.indexOf(operandStr), lineNumber);
        }

        // LOAD/STORE LOCAL logic
        if (opcode.equals(OpCode.LOAD_LOCAL) || opcode.equals(OpCode.STORE_LOCAL)) {
            if  (operandStr == null) throw new VirtualMachineException(command + " requires a variable name");
            return new Instruction(opcode, Integer.parseInt(operandStr), lineNumber);
        }

        // INC/DEC LOCAL logic
        if (opcode.equals(OpCode.INC_LOCAL) || opcode.equals(OpCode.DEC_LOCAL)) {
            if (operandStr == null) throw new VirtualMachineException(command + " requires a slot index");
            return new Instruction(opcode, Integer.parseInt(operandStr), lineNumber);
        }

        // JUMP Logic
        if (
                opcode.equals(OpCode.JUMP) || opcode.equals(OpCode.JIT) ||
                opcode.equals(OpCode.JIF) || opcode.equals(OpCode.CALL)
        ) {
            if (operandStr == null) throw new VirtualMachineException(command + " requires a label");

            // Use toUpperCase() to match how Pass 1 stores them
            String searchLabel = operandStr.toUpperCase();
            if (labels.containsKey(searchLabel)) {
                return new Instruction(opcode, labels.get(searchLabel), lineNumber);
            }
            throw new VirtualMachineException("Error: label '" + searchLabel + "' does not exist");
        }

        // Default for HALT, ADD, etc. (No operand needed)
        return new Instruction(opcode, 0, lineNumber);
    }

    /**
     * Executes one step based on type of instruction.
     * Calls each specific handler based on the type passed in.
     * @param instruction instruction
     */
    private void executeOneStep(Instruction instruction) {
        OpCode opCode = instruction.opcode();
        switch (opCode.getCategory()) {
            case SYSTEM -> new ControlHandler().execute(instruction, opCode, this);
            case STACK ->  new StackHandler().execute(instruction, opCode, this);
            case MATH  -> new MathHandler().execute(instruction, opCode, this);
            case LOGIC ->  new LogicHandler().execute(instruction, opCode, this);
            case BRANCH -> new BranchingHandler().execute(instruction, opCode, this);
            case MEMORY ->  new MemoryHandler().execute(instruction, opCode, this);
            case IO ->  new IOHandler().execute(instruction, opCode, this);
            default -> throw new VirtualMachineException("Error: Unknown OpCode category!");
        }
    }

    /**
     * Returns all data in program stack
     */
    public void returnStackData() {
        while (!programStack.isEmpty()) {
            Integer operand = programStack.pop();
            System.out.println(operand);
        }
    }

    /**
     * Gets functino name from function map
     * @param programCounter program counter
     * @return map entry name
     */
    public String getFunctionName(int programCounter) {
        Map.Entry<Integer, String> entry = addressToLabel.floorEntry(programCounter);
        return (entry != null) ? entry.getValue() : "MAIN";
    }

    /**
     * Prints out function call trace
     */
    public void printFunctionStackTrace() {
        System.err.println("\nStack Trace (Most recent call first):");

        // 1. Get the instruction that actually crashed
        Instruction crashInstr = executableInstructions.get(programCounter);
        System.err.println("  at line " + crashInstr.lineNumber() +
                " (inside :" + getFunctionName(programCounter) + ")");

        // 2. Walk back through the return stack
        functionStackLoop();
    }

    /**
     * Gets values on the function stack and displays them.
     */
    private void functionStackLoop() {
        for (int i = callStack.size() - 1; i >= 0; i--) {
            Frame frame = callStack.get(i);
            int returnAddress = frame.getReturnAddress();
            int callAddr = returnAddress - 1; // The address of the CALL instruction

            Instruction callInstr = executableInstructions.get(callAddr);
            System.err.println("  at line " + callInstr.lineNumber() +
                    " (inside :" + getFunctionName(callAddr) + ")");
        }
    }

    /**
     * Called after every function for debugging
     * @param instruction the current instruction
     */
    private void debug(Instruction instruction) {
        System.out.println("------------DEBUGGER------------");
        System.out.println("Instruction Line Number: " + instruction.lineNumber());
        System.out.print("Program Stack (Top to Bottom): ");
        for  (int i = programStack.size() - 1; i >= 0; i--) {
            System.out.print(programStack.get(i) + " ");
        }

        System.out.println();
        System.out.print("Global Variables: ");
        for (String key : globalVariables.keySet()) {
            System.out.print("(" + key + ", " +  globalVariables.get(key) + ") ");
        }

        System.out.println();
        System.out.println("Call Stack (Top to Bottom): ");
        functionStackLoop();

        System.out.println("-------------DEBUGGER------------");

        System.out.print("Continue to next instruction (Y/N): ");
        Scanner input = new Scanner(System.in);
        String continueTo = input.next().toLowerCase();
        if (continueTo.equalsIgnoreCase("N")) {
            this.isRunning = false;

        }


    }

    /**
     * Gets program stack
     * @return program stack
     */
    public Stack<Integer> getStack() {
        return programStack;
    }

    /**
     * Get program counter value
     * @return program counter
     */
    public int getProgramCounter() {
        return programCounter;
    }

    /**
     * Set program counter
     * @param programCounter new program counter
     */
    public void setProgramCounter(int programCounter) {
        this.programCounter = programCounter;
    }

    /**
     * Set is running
     * @param isRunning program running boolean
     */
    public void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }


    public boolean getIsRunning() {
        return isRunning;
    }

    /**
     * @return program storage
     */
    public ArrayList<String> getProgramStorage() {
        return programStorage;
    }


    /**
     * @return constant pool
     */
    public ArrayList<String> getConstantPool() {
        return constantPool;
    }

    /**
     * @return global variables
     */
    public Map<String, Integer> getGlobalVariables() {
        return globalVariables;
    }

    /**
     * @return current active frame
     */
    public Frame getActiveFrame() {
        if (callStack.isEmpty()) throw new VirtualMachineException("Error: callStack is empty");
        return callStack.peek();
    }

    /**
     * gets function call stack
     * @return function call stack
     */
    public Stack<Frame> getCallStack() {
        return callStack;
    }

    /**
     * Prints the global variables
     */
    public void printGlobalVariables() {
        for (Map.Entry<String, Integer> entry : globalVariables.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    /**
     * Gets executable instructions
     * @return executable instructions
     */
    public List<Instruction> getExecutableInstructions() {
        return executableInstructions;
    }

    /**
     * Get Heap
     * @return heap
     */
    public byte[] getHeap() {
        return heap;
    }

    /**
     * Heap Pointer getter
     * @return heap pointer
     */
    public int getHeapPointer() {
        return heapPointer;
    }

    /**
     * Sets the heapPointer using an offset
     * @param offset offset for heap pointer
     */
    public void setHeapPointer(int offset) {
        this.heapPointer = this.heapPointer + offset;
    }

    public void setHeapPointerAt(int offset) {
        this.heapPointer = offset;
    }
}
