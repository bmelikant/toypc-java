start:

	ldd #1000h
	lds testLine

prn:

	ldstr
	cmpa #0
	je done

	ststr
	jmp prn

done:

	hlt

testLine db "Hello, world!",#0

	hlt
