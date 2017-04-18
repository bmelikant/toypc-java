start:

	lda #31h
	ldd #1000h
	lds testLabel

prn_loop:

	ststr
	ada #01h

	cmpa #42h
	je done

	jmp prn_loop

done:

	hlt

testLabel:

	nop
