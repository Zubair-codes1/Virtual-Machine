# **ZVM - ZUBAIR VIRTUAL MACHINE**

## **What is a Virtual Machine?**

A virtual machine is a complete software resource that acts as its own physical computer
whilst being seperate from the actual device it is running on. In simpler terms, it is a 
software based computer within a real computer. It has its own virtual CPU, memory and storage.
Virtual Machines provide a great way of mimicking other architectures and running them on your device.
They are also very useful in providing a protective layer over the actual hardware, as any malware or 
cyberattacks are carried out on the virtual machine rather than the actual hardware (although in recent
years there has been an increase in malware designed to bypass them).

## **What is the ZVM?**

The ZVM is a custom-built stack-based virtual machine that is turing complete and comes with a
complete toolchain. This includes a custom ISA (Instruction Set Architecture), a compiler (for my custom Z++ language)
and an assembler (along with a custom bytecode), 
a virtual machine to run the programs, a global heap to store dynamic memory such as strings and the Zebugger (Debugger). 
Programs can either be run through binary or through the .asm files straight away. 
The ZVM has its own Fetch-Decode-Execute (FDE) cycle along with a program stack to manage its memory. 
It also uses a program counter (typical of CPUs) to keep track of lines allowing for both sequential
and non-sequential (functions/recursion/loops) programs to be run.

Programs can be compiled from Z++ (.zpp files) to assembly (.asm files), then to binary (.bin files) and then run on the
virtual machine.

## **Program Structure**

```
VirtualMachine/
├── compiler/
│   ├── src/
│   │   ├── zplusplus/
│   │   │   ├── ast/
│   │   │   │   ├── AssignmentStatement.java
│   │   │   │   ├── ASTNode.java
│   │   │   │   ├── BinaryExpression.java
│   │   │   │   ├── BlockStatement.java
│   │   │   │   ├── BreakStatement.java
│   │   │   │   ├── CallingExpression.java
│   │   │   │   ├── Expressionjava
│   │   │   │   ├── ExpressionStatement.java
│   │   │   │   ├── ForStatement.java
│   │   │   │   ├── FunctionDeclarationStatement.java
│   │   │   │   ├── GroupingExpression.java
│   │   │   │   ├── IfStatement.java
│   │   │   │   ├── LiteralExpression.java
│   │   │   │   ├── Parameter.java
│   │   │   │   ├── PrintStatement.java
│   │   │   │   ├── ReturnStatement.java
│   │   │   │   ├── Statement.java
│   │   │   │   ├── UnaryExpression.java
│   │   │   │   ├── VariableDeclarationStatement.java
│   │   │   │   ├── VariableExpression.java
│   │   │   │   └── WhileStatement.java
│   │   │   ├── code_gen/
│   │   │   │   └── CodeGenerator.java
│   │   │   ├── exceptions/
│   │   │   │   ├── CodeGenException.java
│   │   │   │   ├── CompilerException.java
│   │   │   │   ├── SemanticException.java
│   │   │   │   └── SyntaxException.java
│   │   │   ├── lexer/
│   │   │   │   ├── Lexer.java
│   │   │   │   ├── Token.java
│   │   │   │   └── TokenType.java
│   │   │   ├── parser/
│   │   │   │   └── Parser.java
│   │   │   ├── sem_analysis/
│   │   │   │   ├── symbol/
│   │   │   │   │   ├── FunctionSymbol.java
│   │   │   │   │   ├── Symbol.java
│   │   │   │   │   └── VariableSymbol.java
│   │   │   │   ├── Analyser.java
│   │   │   │   ├── Environment.java
│   │   │   │   └── Type.java
│   │   └── └── Main.java
│   └── tests/
│       ├── AnalyserTest.java
│       ├── CodeGeneratorTest.java
│       ├── LexerTest.java
│       └── ParserTest.java
├── assembler/
│   ├── src/
│   │   ├── Assembler.java
│   │   ├── AssemblerException.java
│   │   ├── BinaryWriter.java
│   │   ├── CodeGenerator.java
│   │   ├── EncodedInstruction.java
│   │   ├── Lexer.java
│   │   ├── Main.java
│   │   ├── ParsedLine.java
│   │   ├── Parser.java
│   │   ├── SymbolTable.java
│   │   ├── Token.java
│   │   └── TokenType.java
│   └── tests/
│       ├── LexerTest.java
│       ├── ParserTest.java
│       ├── SymbolTableTest.java
│       ├── CodeGeneratorTest.java
│       └── FullAssemblerTest.java
├── vm/
│   ├── src/
│   │   ├── BinaryLoader.java
│   │   ├── BranchingHandler.java
│   │   ├── ControlHandler.java
│   │   ├── Frame.java
│   │   ├── Instruction.java
│   │   ├── InstructionHandler.java
│   │   ├── IOHandler.java
│   │   ├── LoadedProgram.java
│   │   ├── LogicHandler.java
│   │   ├── Main.java
│   │   ├── MathHandler.java
│   │   ├── MemoryHandler.java
│   │   ├── OpCode.java
│   │   ├── OpCodeCategory.java
│   │   ├── ScopeCategory.java
│   │   ├── StackHandler.java
│   │   ├── VirtualMachine.java
│   │   └── VirtualMachineException.java
│   └── tests/
│       ├── BinaryLoaderTest.java
│       └── VirtualMachineTest.java
├── programs/
│   ├── hello_world.asm
│   ├── factorial.asm
│   └── fibonacci.asm
├── output/
│   └── .gitkeep
├── docs/
│   ├── ISA.md
│   └── BYTECODE.md
├── .gitignore
└── README.md
```

## **ARCHITECTURE**

```
[ Your Source Code (Z++ file) ] (.zpp)
         │
         ▼
 ┌───────────────┐
 │   Zompiler    │ ──► Frontend: Lexer, Parser and AST
 │  (Compiler)   │ ──► Backend: Semantic Analyser, Code generator
 └───────────────┘
         │
         ▼
[ Assembly code ] (.asm)
 ┌───────────────┐
 │   Zembler     │ ──► Pass 1: Scan Labels & Build Constant Pool
 │  (Assembler)  │ ──► Pass 2: Map Opcodes & Emit 12-byte Instructions
 └───────────────┘
         │
         ▼
[ Binary Bytecode ] (.bin)
         │
         ▼
 ┌───────────────┐
 │  ZVM Engine   │ ──► Dual Memory: [ Call Stack Frames ] (Local Variables)
 │   (Runtime)   │                  [ Global Byte Heap ] (Dynamic Strings)
 │               │ ──► Debugger (Accessed using --debug command when running vm.jar)  
 └───────────────┘
```

## **ISA**

The instruction set architecture is based on the functionality of a stack based 
virtual machine. Hence, all of its operations only have one operand or none. The ISA
is turing complete with full capability for functions, recursion, loops and branching.
The program also produces a stack trace in case of any invalid operations that are passed
into the program. The ISA also has an accompanying Bytecode format that is used by the assembler
to encode the instructions in binary using hex values. You can find the link to both of these below:

[ISA Documentation](docs/ISA.md)  
[Bytecode Format](docs/BYTECODE.md)

## **Zompiler and Z++**

The Zompiler (compiler) compiles Z++ source code and targets my own custom assembly language. The Z++ language
is inspired by many other existing languages. It is a procedural language so far so it has no OOP. The syntax of it is
similar to a Python and a C hybrid. For example, it requires a main() function for the program to run and it generally
has all the main characteristics of C such as being statically types, specified return types, for loop structure etc.

For function declarations it follows a mix of python and C, with the function declaration starting off with "def", then the
return type of the function and then the function name and so on from there.

Currently, the language only supports three main types, those being strings, ints and booleans but this will be expanded on
in later updates to the compiler.

To read more about the compiler you can read its seperate documentation page:

[Z++ Documentation](docs/Z++.md)

## **Zebugger**

The Zebugger (debugger) is one part of the ZVM's toolchain. Using a simple --debug command
after the command to run the binary program, allows for the debugger to be activated. The debugger
allows for the contents of the stack, global variables and the function call stack to be read at every
instruction. The user has the option to stop the program completely by typing N or n (representing No)
to stop the program completely and stop the debugger. This opportunity is given after every instruction is debugged.
The Zebugger is still in the works with plans to add specific BREAKPOINT opcodes so that users can manually control
which lines of the assembly are actually debugged.

## **Building The Project**

Building the project can be done in two ways, either by using the precompiled executable jars that are posted on the
latest version release on GitHub Versions or by downloading the entire source code and then running the following command:

```
mvn clean package (or just 'mvn package')
mvn test
```

To compiler, assembler and run Z++/assembly programs, the following commands are used. Note: If the precompiler jar files
are downloaded there is no need to run compiler/target/compiler.jar or similiar for the assembler.jar and vm.jar as that is not
necessary, however if the project is copied / forked, then this is necessary as Maven naturally places the compiler jar files
in these locations. If the jar files are downloaded it is sufficient to just run "compiler.jar"/"assembler.jar"/"vm.jar"
without specifying their paths. Both jar methods are given below.

## **Compiling Programs**

```asm
java -jar compiler/target/compiler.jar <input.zpp> -o <output.asm>
java -jar compiler.jar <input.zpp> -o <output.asm>
```

## **Assembling Programs**

```asm
java -jar assembler/src/target/assembler.jar <input.asm> -o <output.bin>
java -jar assembler.jar <input.asm> -o <output.bin>
```

## **Running Binary Programs**

```
java -jar vm/target/vm.jar <program.bin>
java -jar vm.jar <program.bin>
```

## **Debugging Binary Programs**

```
java -jar vm.jar <program.bin> --debug
```

## **Example Programs**

## **Z++ Example**

Test Program for Z++.

```zpp
def int add(int a, int b) {
    return a + b;
}

def void checkValue(int val) {
    if (val > 10) {
        print("Value is greater than 10");
    } else {
        print("Value is 10 or less");
    }
}

def int main() {
    print("=== Starting Z++ Execution ===");

    int x = 5;
    int y = 7;
    int result = add(x, y);

    checkValue(result);

    print("--- Running While Loop ---");
    int counter = 0;
    while (counter < 3) {
        print("Inside while loop iteration");
        counter = counter + 1;
    }

    print("--- Running For Loop ---");
    for (int i = 0; i < 5; i = i + 1) {
        if (i == 2) {
            print("Reached threshold, exiting loop early");
            break;
        }
    }

    print("=== Program Finished Successfully ===");
    return 0;
}
```

**Compiler and Run**

```
java -jar compiler.jar compilerTest.zpp -o compilerTest.asm
java -jar assembler.jar compilerTest.asm -o compilerTest.bin
java -jar vm.jar compilerTest.bin
```

**Expected Output**

```
VM OUTPUT: === Starting Z++ Execution ===
VM OUTPUT: Value is greater than 10
VM OUTPUT: --- Running While Loop ---
VM OUTPUT: Inside while loop iteration
VM OUTPUT: Inside while loop iteration
VM OUTPUT: Inside while loop iteration
VM OUTPUT: --- Running For Loop ---
VM OUTPUT: Reached threshold, exiting loop early
VM OUTPUT: === Program Finished Successfully ===
```

## **Assembly Only Examples**

### Factorial

Calculating 5!

```asm
CALL :factorial
HALT

:factorial
PUSH 5
STORE_LOCAL 0
PUSH 1
STORE_LOCAL 1

:loop
LOAD_LOCAL 1
LOAD_LOCAL 0
MULT
STORE_LOCAL 1
DEC_LOCAL 0
LOAD_LOCAL 0
PUSH 0
GT
JIT :loop

LOAD_LOCAL 1
PRINT
RET
```

**Assemble And Run**

```
java -jar assembler/target/assembler.jar programs/factorial.asm -o output/factorial.bin
java -jar vm/target/vm.jar output/factorial.bin
```

**Expected Output**

```
VM OUTPUT: 120
```

---

### **ZVM Engine - Strings**

Prints "ZVM Engine is Online" twenty times.

```asm
; --- Initialize Loop Control Variables ---
PUSH 0
STORE loop_counter

; --- Load String References ---
PUSH_STR "ZVM Engine is Online" ; Allocated inside Constant Pool via Assembler
STORE message_ptr

:print_loop
LOAD loop_counter
PUSH 20
LT                  ; Evaluates: loop_counter < 20
                    ; Pushes strictly 1 if true, 0 if false.

JIF :exit_program   ; If loop condition is strictly 0 (False), break out

LOAD message_ptr    ; Fetch the heap base address pointer
PRINT_STR           ; Custom string printer processes target item

; Increment Loop Pointer
LOAD loop_counter
PUSH 1
ADD
STORE loop_counter
JUMP :print_loop

:exit_program
HALT
```

**Assemble And Run**

```
java -jar assembler/target/assembler.jar programs/dynamic_strings.asm -o output/dynamic_strings.bin
java -jar vm/target/vm.jar output/dynamic_strings.bin
```


**Expected Output**

```
VM OUTPUT: ZVM Engine is Online (printed 20 times)
```

---

### **Fibonacci**

Prints the 9th fibonacci number (34)

```asm
CALL :fibonacci
HALT

:fibonacci
PUSH 0
STORE_LOCAL 0
PUSH 1
STORE_LOCAL 1
PUSH 8
STORE_LOCAL 2

:loop
LOAD_LOCAL 0
LOAD_LOCAL 1
ADD
STORE_LOCAL 3

LOAD_LOCAL 1
STORE_LOCAL 0

LOAD_LOCAL 3
STORE_LOCAL 1

DEC_LOCAL 2
LOAD_LOCAL 2
PUSH 0
GT
JIT :loop

LOAD_LOCAL 1
PRINT
RET
```

**Assemble And Run**

```
java -jar assembler/target/assembler.jar programs/fibonacci.asm -o output/fibonacci.bin
java -jar vm/target/vm.jar output/fibonacci.bin
```

**Expected Output**

```
VM OUTPUT: 34
```

---

## **Technical Details**

The compiler is similar to most compilers but its unique in that it doesnt target a standard ISA such x86-64 or ARM64,
rather it specifically targets my own assembly language that I created for the virtual machine. It follows the typical
structure of a compiler with a frontend which is the lexer and parser duo (parser naturally includes the abstract syntrax tree as
part of it) and also a backend which is the semantic analyser and the code generator. Typically there would be more steps such
as the intermediate representation and maybe a linker but since I am targeting my own assembly code, I didn't see the need of
doing these steps. The compiler is completely seperated from the assembler and the vm, so it only produces the assembly and
leaves the assembling and execution to them.

The Assembler runs on a two pass system. It first looks at the entire
.asm file and strips it of comments, labels and empty lines. In the first pass
it also builds a symbol table which includes all the labels and a constant pool.
In the second pass, the assembler generates the actual binary instructions in their 
bytecode format.

The approach to functions in my assembly language is based on frames.
Each function has an associated frame. This frame stores its local variables.
Every time a function is called, a new frame is created and put in the call stack.
The frame at the top of the stack is the current active frame, and it is where all the
LOCAL instructions act upon. Once the RET instruction is executed, the active frame is removed.

Strings can be used using a global heap which is accessed using a heap pointer. Strings are 
put into the heap using a length prefixed format in which the first 4 bytes represent the length of the string.

## **Design Decisions**

Building a VM comes with a lot of decision-making and considerations of tradeoffs. Below I list a few
features that I implemented in my VM and the possible tradeoffs of it compared to other methods.

1. Stack-based vs Register-based: I chose a stack-based architecture for its simpler implementation
and smaller ISA, making it more approachable for anyone writing programs for the ZVM.
The tradeoff is performance: register-based VMs are generally faster and more optimisable,
but that wasn't a priority here.
2. Why a two-pass assembler? A single-pass assembler can't resolve forward references 
— labels defined later in the program than the jump that targets them. 
The first pass builds a symbol table of all labels and constants upfront, 
so the second pass can encode every instruction correctly regardless of label order.
3. Why frames? Each function call gets its own frame on the call stack, 
holding its local variables and return address. This makes recursion work naturally
(each call is fully isolated) and lets me design LOCAL instructions that always target
the active frame without any ambiguity.
4. Why C based syntax? I decided to use syntax that is roughly equivalent to the syntax of C since most modern
languages are based on C so it makes it easier for other programmers to pick up the language. I did however, incorporate
other language techniques such as "def" from python becuase I liked the idea of having a specific keyword to indicate the
start of a function.
5. Why no OOP? For now, I have decided to leave the language as a procedural language as they are generally easier to
build. I do plan to add OOP at some point and follow a C++/Java type syntax for it. It would also require some extra work with
the assembler and the virtual machine so that different object types can be accomodated.
6. Why not a tree-walk interpreter? I decided not to go the route of a tree-walk interpreter as I specifically wanted
to target my own assembly language. 
4. Why JAR files? JAR files makes running programs on the VM much easier without understanding
the actual functionality of the VM and the assembler. This makes the project more accessible. I have used Maven
to make building the system easier and then it can be ran through the JAR files.
