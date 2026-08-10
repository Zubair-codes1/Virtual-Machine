	CALL :main
	HALT
:add
	STORE_LOCAL 1
	STORE_LOCAL 0
	LOAD_LOCAL 0
	LOAD_LOCAL 1
	ADD
	RET
	RET
:checkValue
	STORE_LOCAL 0
	LOAD_LOCAL 0
	PUSH 10
	GT
	JIF :elseLabel_0
	PUSH_STR "Value is greater than 10"
	PRINT_STR
	JUMP :endLabel_1
:elseLabel_0
	PUSH_STR "Value is 10 or less"
	PRINT_STR
:endLabel_1
	RET
:main
	PUSH_STR "=== Starting Z++ Execution ==="
	PRINT_STR
	PUSH 5
	STORE_LOCAL 0
	PUSH 7
	STORE_LOCAL 1
	LOAD_LOCAL 0
	LOAD_LOCAL 1
	CALL :add
	STORE_LOCAL 2
	LOAD_LOCAL 2
	PRINT
	PUSH_STR " "
	STORE_LOCAL 3
	LOAD_LOCAL 3
	PRINT_STR
	LOAD_LOCAL 2
	CALL :checkValue
	PUSH_STR "--- Running While Loop ---"
	PRINT_STR
	PUSH 0
	STORE_LOCAL 4
:startLabel_2
	LOAD_LOCAL 4
	PUSH 3
	LT
	JIF :endLabel_3
	PUSH_STR "Inside while loop iteration"
	PRINT_STR
	LOAD_LOCAL 4
	PUSH 1
	ADD
	STORE_LOCAL 4
	JUMP :startLabel_2
:endLabel_3
	PUSH_STR "--- Running For Loop ---"
	PRINT_STR
	PUSH 0
	STORE_LOCAL 5
:startLabel_4
	LOAD_LOCAL 5
	PUSH 5
	LT
	JIF :endLabel_5
	LOAD_LOCAL 5
	PUSH 2
	EQ
	JIF :endLabel_6
	PUSH_STR "Reached threshold, exiting loop early"
	PRINT_STR
	JUMP :endLabel_5
:endLabel_6
	LOAD_LOCAL 5
	PUSH 1
	ADD
	STORE_LOCAL 5
	JUMP :startLabel_4
:endLabel_5
	PUSH_STR "hello world"
	STORE_LOCAL 6
	LOAD_LOCAL 6
	PRINT_STR
	PUSH_STR "=== Program Finished Successfully ==="
	PRINT_STR
	RET
