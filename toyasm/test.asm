start:

	lod si,testString       ; 02 69 48 00       - 4
	call println            ; 0E 08 00          - 3

	lod si,testString2
	call println

	hlt                     ; 10                - 1

println:

	ldstr                   ; 40                - 1
	cmp ra,0                ; 14 18 00          - 3
	je println_done         ; 1F 12 00          - 3

	;cmp ra,0x10
	;je print_return

	call putchar
        	jmp println             ; 0F println        - 3

print_return:

	call put_newline
	jmp println

println_done:

	ret                     ; 45                - 1

putchar:

	lod di,0x1000           ; 02 79 00 10       - 4

	lod rb,[cur_y]          ; 02 2A 47 00       - 4
	lod rx,rb               ; 02 42             - 2
	mul rx,80               ; 05 49 50 00       - 4

	lod rb,[cur_x]          ; 02 2A 46 00       - 4
	add rx,rb               ; 03 42             - 2

	add di,rx               ; 03 74             - 2
	ststr                   ; 41                - 1

	lod rb,[cur_x]          ; 02 2A 46 00       - 4
	inc rb                  ; 31                - 1

	cmp rb,80               ; 14 28 50          - 3
	jl store_cur_x          ; 5F 41 00          - 3

	lod rb,[cur_y]          ; 02 2A 47 00       - 4
	inc rb                  ; 31                - 1
	sto rb,[cur_y]          ; 01 2A 47 00       - 4

	lod rb,0                ; 02 28 00          - 3

store_cur_x:

	sto rb,[cur_x]          ; 01 2A 46 00       - 4
	ret                     ; 45                - 1

put_newline:

	lod rb,[cur_y]
	inc rb

	cmp rb,25
	jl newline_valid

	jmp newline_done

newline_valid:

	sto rb,[cur_y]
	lod rb,0
	sto rb,[cur_x]

newline_done:

	ret

cur_x db 0x00                   ; 00                - 1
cur_y db 0x00                   ; 00                - 1

testString db 'Hello, toyasm world!',0x10,0x0       
testString2 db "This is a second test string...",0x10,0x0
