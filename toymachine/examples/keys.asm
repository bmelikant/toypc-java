start:

	ldd #1000h

keyLoop:

	call readKey
	cmpa #10
	je haltMachine

	ststr
	jmp keyLoop

haltMachine:

	hlt

readKey:

	lda #05	; read key function of keyboard controller
	sta $20F0	; store a at the keyboard's data register location

waitLoop:

	lda $20F1		; load the byte from the low data register
	cmpa #FFh		; see if it is no character ready
	je waitLoop		; wait if it is

	ret		; return the key character otherwise
