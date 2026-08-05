# **Z++ Documentation**

I created the Z++ language as a language that would specifically target my own virtual machine and assembly ISA. The
language is influenced by C and parts of it from python. It's also a procedural language and has no object-oriented programming
(OOP) yet but that may change in later updates to the language (the OOP will follow sort of a Java/C++ syntax). The reason
for choosing this style of language is that C is the basis for most modern languages, so its syntax is widely used. Thus, 
I decided to follow this syntax for my language to make it easier for programmers to quickly pick up the syntax. 

As of now the language has all the standard programming techniques such as selection, iteration, functions etc. however,
it is not very feature-rich. I have explained the syntax along with some examples below.

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


