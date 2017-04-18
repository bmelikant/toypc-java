org $0

	jmp start

println:

	ldstr		; load the next character from the string at reg s into a
	cmpa #0		; see if it is a terminating character
	je println_done	; if so finish

	cmpa #10h		; now check for an end of line character
	je prnRet		; and print a return character if so

	call computeNextPos	; otherwise compute the next location into d
	ststr		; and store the string character there

	jmp println		; print the next character

prnRet:

	lda curY		; get the current Y coordinate
	ada #1		; increment a to make the new coordinate
	sta curY		; and now store the value

	lda #0		; reset the column
	sta curX		; to 0

	jmp println		; print the next character

println_done:

	ret

computeNextPos:

	psh a		; save the a register

	lda curY		; get the current y value into a
	mla #80		; multiply it by video columns
	ada curX		; and add the x value to it
	ldd #1000h		; now load the destination pointer with start addr of video memory
	add a		; add the value in a onto d

	lda curX		; time to update the x value
	ada #1		; add one to it
	sta curX		; and store it back

	pop a		; restore the a register
	ret

start:

	lds helloLine		; load the string into the source register
	call println		; and print it

	hlt		; stop the machine

helloLine db "Hello, world!",#10h,"I am on the second line!",#0
curX db #0
curY db #0
