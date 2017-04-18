org $7000

	jmp bios_entry		; jump to the entry point for our BIOS file

; data section. Hold information for screen coordinates and such
curX db 0x0
curY db 0x0

vidCols db 80
vidRows db 25

biosRevision db 'Revision 1.0',0x10,0x0

waitKey:

	psh s
	psh a
	lds continueString
	call PrintString

waitLoop:

	lda 5
	sta $20F0

	lda $20F1
	cmpa #0
	je waitLoop

	cmpa #FFh
	je waitLoop

	pop a
	pop s
	ret

continueString db "Press any key to continue...",#0

PrintString:

	ldstr		; load a byte from the string at S
	cmpa #0h		; see if it is zero
	je PrintStringDone	; finish the function if so

	cmpa #10		; check for a linefeed character
	je PrintStringCR	; print a crlf if so

	ldb curY		; load the current Y value into B
	mlb vidCols		; multiply it by column numbers
	adb curX		; add the current x value to it

	; get the video ptr here
	ldd #1000h		; video memory starts at 1000h
	add b		; add the computed value from b onto d

	ldb curX		; read the current x value
	adb #1		; increment it

	stb curX		; and store it back

	ststr		; store the character at the given position
	jmp PrintString	; jump back to the start of the function

PrintStringCR:

	lda curY		; get the current Y value
	ada #1		; add one to it
	sta curY		; and store it back to it's place

	ldb #0		; reset the xvalue to zero
	stb curX		; by storing it to curX

	jmp PrintString	; now continue to print the string

PrintStringDone:

	ret		; return from this function

PrintInteger:

	cmpb #0		; see if our number is zero
	je AlreadyZero	; and finish if so

	ldd intStr		; otherwise load the address of the conversion string
	ldx #4		; loop four times

ClearStringLoop:

	lda #0		; we need to clear the string
	ststr		; store zero at intStr

	sbx #1		; subtract one from b
	cmpx #0		; then see if it is zero

	jne ClearStringLoop	; and keep clearing if not

	ldd intStr		; reload the string

ConvertLoop:

	lds convertString	; the string of characters to use during conversion

	ldx b		; load b into x
	mdx #10		; get b % 10 (base is always 10!)
	ads x		; and add that to s to get the index into the conversion string

	ldstr		; load the conversion byte from the conversion string
	ststr		; and store it in the conversion buffer
	call waitKey		; now wait to diagose problems

	dvb #10		; now divide b by ten
	
	cmpb #0
	je EndPrintInteger

	jmp ConvertLoop	; and loop

EndPrintInteger:

	ret		; return to caller

AlreadyZero:

	ldd intStr		; since we already have a zero
	lda #30h		; we just store a zero character
	ststr		; in the int conversion buffer

	ret		; now return
	
LoadTime:

	lda $20FA		; load the hours component
	ada #30h
	sta $10A0		; store it at the given address

	ret

bios_entry:

	lds biosLoadMsg	; load the first line into S
	call PrintString	; and print it

	lds biosRevision	; load the revision string
	call PrintString	; and print it

	lda #31		; character '1'
	sta $21F2		; store it in the low data register

	lda #02		; disk controller store byte
	sta $21F1		; store it in the upper control register

waitForStore:

	lda $21F2		; check for 0x00
	cmpa #0

	je done
	jmp waitForStore

done:

	hlt		; stop the machine

; string data
biosLoadMsg db "Running BIOS Boot Sequence...",#10,#0
testString db "This is a test",#0
intStr db #0,#0,#0,#0,#0
convertString db '0123456789'
testInt db #11

