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


