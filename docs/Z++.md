# **Z++ Documentation**

I created the Z++ language as a language that would specifically target my own virtual machine and assembly ISA. The
language is influenced by C and parts of it from python. It's also a procedural language and has no object-oriented programming
(OOP) yet but that may change in later updates to the language (the OOP will follow sort of a Java/C++ syntax). The reason
for choosing this style of language is that C is the basis for most modern languages, so its syntax is widely used. Thus, 
I decided to follow this syntax for my language to make it easier for programmers to quickly pick up the syntax. 

As of now the language has all the standard programming techniques such as selection, iteration, functions etc. however,
it is not very feature-rich. I have explained the syntax along with some examples below.

---
## **main()**

Similar to C, the language requires a main function as the starting off point for the execution of code, this is because
the assembly language immediately jumps to the main function after setting up the global variables in the program. The main()
function doesn't necessarily have to be the first function in the source code as the compiler finds all the functions before the
assembly code is written and keeps them stored for the code generation (using a symbol table/environment).

e.g.

```
def int main() {
    return 0;
}
```

As seen from the above text the general format of a function is similar to C with a return type then the name of the function,
and afterward any parameters then the actual block of code for the function. Since the main() function is the starting off
point of the program, it cannot take any parameters, but can use any global variables that are declared.

The only difference from C is the "def" keyword. I decided to include this in my language as I like the idea of having
a specific keyword to indicate the start of a function just like in python with "def" or in Rust with "fn" as it helps
make the program more readable.

---
## **Statements vs Expressions**

Generally within programming languages, there are two main types of nodes within a syntax, these being statements and expressions.
Statements are lines of code / or parts of a line of code that do not produce a value, meaning that from the start of a statements
execution to the end of it, the net value output should be 0. Since my virtual machine is stack-based, a statement should not leave
a value on the stack after it has finished execution. Expressions on the other hand, do return a value or in the case of the VM,
they leave a value on the stack.

Statements can have expressions within them , for example, a declaration statement (to declare a variable) may assign a value,
values are a type of expression.

Statements must also end with a semicolon to indicate the end of the statement, and so that the compiler can know when the next
statement is starting.

The reason form mentioning expressions and statements is that they help a programmer understand how code functions and what are
the expected values that should be received.

Examples of expressions:

```
5           ->      literal expression
5 + 10      ->      binary expression (two operators)
(5 + 10)    ->      grouping expression (with a binary expression within it)
-5          ->      unary expression (one operator)
!true       ->      also unary expression
x           ->      variable expression
func()      ->      calling expression (calls a function)
```

Examples of statements:

```
x = 5;      ->      assignment statement
int x = 5;  ->      variable declaration statement
return x;   ->      return statement
...
```

## **Variables**
Variables are symbols used in a language that identify data. Variable are names given to data, and that name can then be used
throughout the lifetime of the variable (lifetime of variables discussed under the section on scope) as a substitute for the data.
This is similar to how variables are used in traditional maths such as x = 5. The value x can then be used in any other equation
to represent 5. For example, y = x + 5, would result in y = 5 + 5 and thus y = 10.

Example code:

```
def void main() {
    int x = 5;
    int y = 10;
    
    int z = x + y;
}
```

As shown from the above code, the value 5 is stored in the variable x and the value 10 is stored in the variable y, these variables
can then be used instead of their corresponding values in later parts of the program as we see with:
> int z = x + y;

Note: Variables in Z++ are only allowed to have certain characters within them. They generally follow these rules:
1. Must start with an alphabetical character - so no numbers or other symbols starting the variable name
2. Can contain alphabetical, numerical and the underscore (_) character in any of the other positions

Valid/Invalid Variable names examples:
```
x       -> valid
y       -> valid
x3      -> valid
x_y     -> valid

1x      -> invalid
_x      -> invalid
x%      -> invalid
```


---
## **Data Types**

As of now the Z++ language only has three main types, those being strings, booleans and integers. Strings are a collection of
characters (another data type that doesn't necessarily exist in my language yet but exists in most modern languages), whilst
booleans are a true or false value (0 or 1 respectively). Integers are regular whole numbers. As of now there is no type to
hold decimal values such as the common "float"/"double" data types that are found in most languages.

Example code:

```
def int main() {
    string x = "hello";
    int y = 5;
    bool z = false;
    bool w = true;
}
```

As seen above variables of type string are declared with the "string" keyword, boolean variables are declared with "bool",
and integer variables are declared with "int".

Common errors that people make include:
1. Not surrounding strings with quotation marks.
2. Surrounding integers with quotation marks -> No operations can be performed on this.
3. Making boolean literals capitalised (e.g. True/False). This might be correct in other languages but in Z++ they are lowercase.

---
## **Operators**

Operators in traditional maths are functions that take in values (or one value for unary operators) and produce another value.
This is also true for programming languages, an operator takes in a value or multiple values and returns a new value.

Math Operators: 
1. assignment (=)
2. plus (+)
3. minus (-)
4. multiply (*)
5. divide (/)
6. modulo (% - remainder of division)

Logical Operators:
1. Logical And (&& - true if both values are true, false otherwise)
2. Logical Or (|| - true if either or both of the two values are true, false if none are true)
3. Logical Not (! - true if false, and false if true) 

Bitwise Operators:
1. Bitwise And (& - 1 if both bits are 1)
2. Bitwise Or (| - 1 if one or both bits are 1)
3. Bitwise Xor (^ - 1 only if one bit is 1)
4. Bitwise Not (~ - 1 if 0, 0 if 1 -> flips the bit around)

Comparison Operators:
1. Equality (==, true if both values are equal)
2. Not Equal (!=, true if both values are not equal)
3. Less than (<, true if left value is less than right value)
4. Greater than (> , true if left value is greater than right value)
5. Less than or equal to (<=, true if left value is less than or equal to right value)
6. Greater than or equal to (>=, true if left value is greater than or equal to right value)

Example code:

```
5 + 5          -> 10
5 - 5          -> 0
5 * 5          -> 30
5 / 5          -> 1
10 % 3         -> 1 (10 / 3 = 3 remainder 1)

true && true   -> true
true || false  -> true
!true          -> false
!false         -> true

1 & 1          -> 1
1 | 0          -> 1
1 ^ 0          -> 1
~1             -> 0

5 == 5         -> true
5 != 5         -> false
5 < 5          -> false
5 > 5          -> false
5 <= 5         -> true
5 >= 5         -> true
```

---
## **Output**

To make the virtual machine output values, we use the print() command. As of now, the print command only
accepts string arguments so integers and booleans cannot be printed. As of now the language has no input
mechanism but that will be added in future updates to the language.

Example code:

```
def void main() {
    print("Hello World!");
}
```

Or if variables are involved:

```
def void main () {
    string x = "Hello World!";
    print(x);
}
```

Both will output:
> Hello World!

---
## **Selection**

Selection is an integral part of programming languages, it allows the program to go down multiple
paths based on a certain condition. In the Z++ language this is limited to only two paths. These two paths
are represented by "if" statements and "else" statements.

Structure of an if-else statement:
```
if (condition) {
...
}else {
    ...
}
```

The condition part is any expression that returns a true or false value, if the condition is true then the program will
execute the instructions/statements within the if block but if it is false then it will execute the instructions in the
else block. However, the else part is not necessary and can be skipped, in which case nothing happens if the condition is false
and the program continues with the next instructions after the if statement is complete.

Example code:

```
def void main() {
    int x = 5;
    if (x > 2) {
        print("x is greater than 2");
    } else {
        print("x is less than 2");
    }
}
```

Output:

> x is greater than 2

---
## **Iteration/Loops**

Iteration, or looping, is one of the other core components of a programming language this is because it allows a section
of code to be repeated multiple times. Most languages have multiple types of loops such as a for loop, while loop, for-each 
loop, do-while loop etc. However, I have limited my language to only the for and while loops as the other types of loops
can be expressed with just these two. Underneath the hood, the while loop and the for loop actually function exactly the same
when it comes to the structure of the assembly they compile into, however they only differ in how a programmer writes them.

### **While loops**

Our first type of loop is a while loop. While loops are called condition-controlled loops as a block of code (group of statements)
will execute as long as a condition is true, similar to an if statement but many iterations.

The basic structure of a while loop is as follows:

```
while (condition) {
    ...
}
```

Example code:

```
def void main() {
    int x = 5;
    while (x > 2) {
        print("Hello World!");
    }
}
```

This code first declares and initialises a variable called x with a value of 5. Then it checks the while loop condiiton, which
returns true as 5 > 2, thus it proceeds into the while loop and executes the statements within it (in this case it just prints
hello world).

Now, whilst this piece of code may be correct syntactically (meaning that the compiler will not notice any syntax errors with it
and hence won't throw an error), there is still a logical error in this program (logical errors refer to the errors made by the
programmer despite the code working correctly in terms of its syntax). Can you spot it? If you said that the loop will loop run
forever, then you are correct!

As seen by the code, every single time the statements within the loop end, it goes back up and checks the condition again,
it keeps on doing this until the condition is false. However, in our scenario the condition is never false since 5 is always
greater than 2. Thus, this code will loop forever. To fix this we need to change the code so that x value will decrease over
a few iterations of the loop and there will eventually come an iteration where x is no longer greater than 2.

Fixed code:

```
def void main() {
    int x = 5;
    while (x > 2) {
        print("Hello World!");
        x = x - 1;
    }
}

```

This will output:
```
Hello World!
Hello World!
Hello World!
```

In this instance, we broke out of the while loop by making the condition false, however we can also break out of the for loop
using a "break" statement. The break statement exits the loop that it is currently within, not any other loops or parent loops
(this will be discussed more in the topic of nesting).

Example:

```
def void main() {
    int x = 6;
    while (x > 2) {
        print("Hello World!");
        
        if (x == 4) {
            break;
        }
        
        x = x - 1;
    }
}
```

This code is similar to the previous code, but instead of the x value running down till 2 then the loop stopping, instead,
it will stop as when it hits x = 4 (after it has printed that is).

As you can also tell from the code above, if statements can be used inside while loops, similarly whiile loops can be used inside
if statements. The reason for this is that '{' and '}' indicate the start of a block of code. A block of code contains a list of
statements. If statements and while statements also come under that so they can be used within each other, and you can also have
multiple while loops inside each other (this is called nesting and will be discussed a bit later).

### **For loops**

For loops are the other type of loop that Z++ allows programmers to use. It is slighly different in that, it does have a condition
but usually this condition is related the numerical values whilst in while loops it can be pretty much anything.

Here is the basic syntax of a for loop:

```
for (intialisation; condition; update) {
    ...
}
```

Notice that a for loop has three main parts within its parentheses. The intitialisation part allows you to intitialise a variable
to a certain value, then the condition part is what is checked for the for loop to run. The expression at the end is run right at the end
of the for loop and typically updates the variable that was initialised.

Example code:

```
def void main() {
    for (int x = 0; x < 5; x = x + 1) {
        print("Hello World!");
    }
}
```

Output:

```
Hello World!
Hello World!
Hello World!
Hello World!
Hello World!
```

For loops and while loops have equivalent expressive power, meaning that a while loop can be rewritten as a for loop and
a for loop can be rewritten as a while loop with just a few adjustments.

While loop version of the previous code:

```
def void main() {
    int x = 0;
    while (x < 5) {
        print("Hello World!");
        x = x + 1;
    }
}
```

Break statements can also be used with for loops.

### **Nesting**

Nesting in reference to loops, refers to the idea of having multiple loops within each other (hence nested). This can be really
useful for performing multiple tasks within a loop, but it should be used carefully as nested loops can drastically increase the 
time complexity of the program. For example for two nested loops the general time complexity is given by O(n^2) meaning that
for n input values, the algorithm will take n^2 time which is frankly unuseable with decently large inputs.

Example code:

```
def void main() {
    for (int x = 0; x < 5; x = x + 1) {
        for (int y = 0; y < 5; y = y + 1) {
            print("Hello World!");
        }
    }
}
```

This code will output "Hello World!" 25 times. Notice how the variable used each time must be different, so it's not allowed to
declare the same variable for two for loops within each other or anywhere in the same scope to be exact (we will see more about
this in the topic of scopes/lifetime of variables).

Nesting allows for multiple loops within each other no matter the type of loop. So you can have 30 while loops nested inside a
for loop and that would be perfectly valid syntactically (although its probably not best speed wise).

Generally, too much nesting should be avoided. A lot of algorithms can be done without nesting loops, and many can be done
with one loop inside another. Generally any more than this is considered as a waste and as a programmer you should consider different
ways of getting the solution without nesting too much.
---

## **Functions**

Functions are an essential part of programming languages, and they are quite similar to the functions in mathematics.
For example, in maths, you could have a function such as f(x) = x + 5. This function takes an input x, and returns that value
plus five. Similarly, in maths we can write g(y) = y + f(6). This is a function that is calls another function,
so function calls act like replacements for the actual statements within thme.

Functions in programming are similar as they CAN take in a value and CAN return a value. Notice that I said "CAN". This is because
a function in programming doesn't necessarily have to take an input and doesn't necessarily have to return a value; The most type of
function is one that is a few statements.

We have actually seen functions for Z++ before, with the main() function.

Example of another function:
```
def void func1() {
    print("Hello World!");
}
```

Each function has a name. This function's name is "func1", the rules for function names are the same as for variables names as both
function and variable names come under the same category of identifiers/symbols.

The "def" keyword indicates the start of a function declaration and then right after it the return type is given. Return types
tell the compiler that this function must return an expression of that particular type. The return types are the same as the
data types: string, int and bool but also include a special return type called "void". This return type tells the compiler that
the function won't actually return anything to the user - since Z++ runs on a VM, this means that when the function is called,
no values will be left on the stack after the function call finishes.

After the return type, then the functions name is given. It must be noted that function names must be unique, and you cannot have multiple
functions with the same name. Other languages allow for something called function overloading and overriding however these functionalities
do not exist within Z++ yet.

As mentioned already, "void" functions don't need to return anything but other functions do, so how is this returning done?
Returning is done through the "return" command. The return command can take a value to return.

Example of return usage;

```
def int add(int x, int y) {
    return x + y;
}
```

As shown here, to return a value, we just write return then follow it up with an expression. It must be noted that once a return
statement is processed, the function is exited and the program returns back to the function that called this function.

In this example, we also see our first use of parameters (or inputs). When this function is called, values must be passed into the function
and then these values are stored in the variables x and y respectively. It's similar to variable initialisation but instead of
"=" being used, the initialisation occurs as the values are passed in.

Example of calling a function:

```
def void main() {
    int total = add(5, 3);
    func1();
}
```

When calling a function, we reference its name and then within parentheses we pass values (arguments) into the parameters of the function.
For functions that do not have any parameters, then nothing is to be passed in as this would be an error. Also, functions that return
values such as the add() function, can be assigned to variables since the add() function will be replaced by its return
expression at runtime. They can also not be assigned to an input but in this scenario the programmer loses out on the return value.
As for "void" functions, then they can't be assigned to variables as they do not return any values however,
they can be used without doing so as shown above.

Functions can also contain previously discussed parts of the language such as loops and selection.



