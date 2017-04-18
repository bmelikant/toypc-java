/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ToyAsm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.regex.Pattern;

/*
 * Assembler.java: First stage of the assembly process for toy executables
 *
 * @author Ben Melikant
 *
 */

public class Assembler {
    
    // instance variables
    private BufferedReader inp;
    private Formatter listOut;
    
    private final File inFile;
    private final File listFile;
    
    private int lineno;
    private int address;

    // code line state variables
    private InstructionPacket instruction;
    private DirectivePacket directive;
    private String linedata;
    private String token;
    private ArrayList<ExePacket> dataList;
    
    private SymbolTable sym_table;
    private ErrorTable error_table;
    
    // create a new assembler instance
    public Assembler (File fi) {
        
        inFile = fi;
        listFile = null;
        
        // reset internal state
        reset ();
    }
    
    public Assembler (File fi,  File lf) {
        
        inFile = fi;
        listFile = lf;
        
        // reset the internal state
        reset ();
    }
    
    // <editor-fold desc="ACCESSOR FUNCTIONS">
    
    // return error status
    public boolean errors_exist () { return error_table.errors_exist (); }
    public ErrorPacket error_at (int idx) { return error_table.error_get (idx); }
    public int error_count () { return error_table.error_count(); }
    
    public ExePacket packet_at (int idx) {
        
        if (idx < dataList.size())
            return dataList.get (idx);
        
        return null;
    }
    
    public int packet_count () { return dataList.size(); }
    
    public int symbol_count () { return sym_table.symbol_count(); }
    public SymtablePacket symbol_at (int idx) { return sym_table.symbol_get(idx); }
    public int symbol_find (String s) { return sym_table.symbol_lookup(s); }
    
    // </editor-fold>
    
    // assemble the file
    public void assemble () {
        
        // attempt to open the input file and create the output file
        try {
            
            // create the input file object
            inp = new BufferedReader (new InputStreamReader (new FileInputStream (inFile)));

            // try to create the listing file (if not null)
            if (listFile != null) {
            
                String output = "ToyASM v3.0 Listing File\n";
                output += "Source file: " + inFile.getName() + "\n\n";
                output += "LINE:ADDR    BINARY DATA        CODE LINE\n";
                output += "-----------------------------------------\n\n";
                
                listOut = new Formatter (listFile);
                listOut.format (output);
            }
            
            else
                listOut = null;
            
            // now start assembling code lines!
            while ((linedata = inp.readLine ()) != null) {
                
                codeLine ();
                lineno++;
            }
            
            inp.close ();

            if (listOut != null) {
                
                listOut.flush ();
                listOut.close ();
            }
            
            // read from the input file, and process it into an instruction packet
        } catch (IOException e) {
            
            // alert the user about the error
            error_table.error_add(ErrorTable.FILE_IO_ERROR, lineno);
        }
    }
    
    // reset the assembler instance
    private void reset () {
        
        lineno = 1;
        address = 0;
        dataList = new ArrayList<>();
        sym_table = new SymbolTable ();
        error_table = new ErrorTable (); 
        linedata = token = "";
        
        create_special_instructions ();
    }
    
    // fetch the next token from the current line (trims token from line)
    private void nextToken () {
        
        token = "";

        // trim all whitespace from the ends of linedata
        linedata = linedata.trim();
        
        // if the line is blank, send a newline character
        if (linedata.isEmpty())
            token = "\n";
        
        // if this is a comment, we send a newline
        else if (linedata.charAt(0) == ';') {
            
            linedata = "";
            token = "\n";
        }
        
        // string literal? Read until end of line or terminating character
        else if (linedata.charAt(0) == '"' || linedata.charAt(0) == '\'') {
            
            char terminator = linedata.charAt(0);
            token += terminator;
            
            for (int i = 1; i < linedata.length (); i++) {
                
                if (linedata.charAt(i) == terminator) {
                    
                    token += terminator;
                    linedata = linedata.substring(i+1).trim();
                    
                    if (!linedata.isEmpty () && linedata.startsWith (","))
                        linedata = linedata.substring (1, linedata.length()).trim();
                    
                    return;
                }
                
                token += linedata.charAt(i);
            }
            
            // blank out linedata!
            linedata = "";
        }
        
        // any other token: Read until EOL
        else {
        
            for (int i = 0; i < linedata.length (); i++) {
                
                if (linedata.charAt(i) == ',' || Character.isWhitespace(linedata.charAt(i))) {
                    
                    linedata = linedata.substring (i+1);
                    return;
                }
                
                token += linedata.charAt(i);
            }
            
            // line is done, blank out linedata!
            linedata = "";
        }
    }
    
        // convert a string to a number
    private int convertNumber (String num) {
        
        int parse = -1;
        
        try {
                
            if (pattern_match (num, XDIGIT_PATTERN)) {
                    
                if (num.endsWith ("h") || num.endsWith("H"))
                    num = num.substring (0, num.length()-2);
                else if (num.startsWith ("0x"))
                    num = num.substring (2, num.length());
                parse = Integer.parseInt(num, 16);
            }
                
            else if (pattern_match (num, DIGIT_PATTERN))
                parse = Integer.parseInt(num, 10);
            else if (pattern_match (num, BDIGIT_PATTERN))
                parse = Integer.parseInt(num.substring (0, num.length()-2), 2);
                
        } catch (NumberFormatException e) {
                
            System.err.println ("Error converting number: " + e.toString ());
        }
        
        return parse;
    }
    
        // parse and assemble one code line
    private void codeLine () {
        
        instruction = new InstructionPacket ();
        directive = new DirectivePacket ();
        
        nextToken ();

        // if the token is a carriage return character, do nothing; blank line!
        if (token.equals ("\n"))
            ;
        
        // if the token exists as a onebyte instruction, process thru onebyte_instruction
         else if (hashmap_match (token, INSTRUCTION_TABLE))
             instruction ();
        
        // if the token exists as a directive, process a directive
        else if (list_match (token, DIRECTIVES_TABLE))
            directive ();
        
        // if the token doesn't match any of the above, see if it conforms to a label pattern
        else if (label_match (token))
            line_label ();
        
        else    
            error_table.error_add(ErrorTable.INSTRUCTION_EXPECTED, lineno);
    }
    
    // assemble a line label
    private void line_label () {
        
        // cache the symbol (it is being defined)
        if (token.endsWith(":"))
            token = token.substring (0, token.length () - 1);
        
        sym_table.symbol_add (token, address, true);
        
        nextToken ();
        
        // we could have an end of line token
        if (token.equals ("\n"))    
            ;
        
        // the following token must be either a mnemonic or a directive
        else if (hashmap_match (token, INSTRUCTION_TABLE))
            instruction ();
        else if (list_match (token, DIRECTIVES_TABLE))
            directive ();
        
        else
            error_table.error_add (ErrorTable.INSTRUCTION_EXPECTED, lineno);
    }
    
    // <editor-fold desc="INSTRUCTION PROCESSING FUNCTIONS">
    
    // assemble an instruction
    private void instruction () {
        
        instruction.storeOpcode (INSTRUCTION_TABLE.get(token.toLowerCase()).opcode);
        int type = INSTRUCTION_TABLE.get(token.toLowerCase()).category;
 
        String opcode = token.toLowerCase();
        
        nextToken ();
        
        // decide what to do next!
        if (type == InstructionPair.ONEBYTE_OPCODE && token.equals ("\n")) {
            
            // place all the data in the instruction packet
            instruction.storeType(InstructionPacket.INSTRUCTION_FIXED);
            instruction.storeSize(1);
            instruction.makeValid();
        }
        
        else if (type == InstructionPair.MATH_OPCODE && register_match (token))
            math_instruction_argument_one ();
        else if (type == InstructionPair.BRANCH_OPCODE && (label_match (token) || immediate_match (token)))
            branch_instruction_argument ();
        else if (type == InstructionPair.STACK_OPCODE && (label_match (token) || immediate_match (token) || register_match (token) || memref_match (token)))
            stack_instruction_argument ();
        else if (type == InstructionPair.SPECIAL_OPCODE) {
            
            // look up the instruction in the special instructions table
            if (SPECIAL_INSTRUCTIONS.containsKey(opcode))
                SPECIAL_INSTRUCTIONS.get(opcode).assemble();
            
            else {
                
                System.err.println ("Error: no handler found for opcode " + token);
                return;
            }
        }
        
        else {
            
            error_table.error_add (ErrorTable.INVALID_OPCODE_OPERANDS, lineno);
            return;
        }
        
        if (instruction.isValid()) {
            
            address += instruction.getSize();
            dataList.add (instruction);
        }
    }
        
    // process the first argument for the math instruction
    private void math_instruction_argument_one () {
        
        // should be a register; fetch it's modr/m value
        instruction.storeModrm ((short) (RM_VALUES_TABLE.get(token.toLowerCase()).rm_value << 4));
        boolean bits16 = RM_VALUES_TABLE.get(token.toLowerCase()).bits16;
        nextToken ();
        
        // could be any type of argument - register, memory, or immediate
        if (memref_match (token))
            math_argument_two_memref ();
        else if (immediate_match (token) && !(instruction.getInstrData()[0] == INSTRUCTION_TABLE.get("sto").opcode))
            math_argument_two_immediate (bits16);
        else if (register_match (token))
            math_argument_two_register (bits16);
        
        // we can LOAD a label (counts as immediate) into a register
        else if (label_match (token) && (instruction.getInstrData()[0] == INSTRUCTION_TABLE.get("lod").opcode))
            math_argument_two_label ();
        
        else {
            
            error_table.error_add (ErrorTable.INVALID_OPCODE_OPERANDS, lineno);
        }
    }
    
    private void math_argument_two_memref () {
        
        // we have to break down the memref and see what arguments we have
        instruction.storeModrm ((short) (instruction.getInstrData()[1] | 0x0a));
        instruction.storeSize (4);
        
        token = token.substring (1, token.length()-1);
     
        // if this was a number, the reference is fixed
        if (pattern_match (token, NUMBER_PATTERN)) {
            
            int memref_value = convertNumber (token);
            
            if (memref_value >= 0) {
                
                instruction.storeImmediateWord (memref_value);
                instruction.storeType(InstructionPacket.INSTRUCTION_FIXED);
                instruction.makeValid();
            }
            
            else                
                error_table.error_add (ErrorTable.NUMBER_FORMAT_ERROR, lineno);
        }
        
        else if (pattern_match (token, LABEL_PATTERN)) {
            
            instruction.storeSymbol (token);
            instruction.storeType (InstructionPacket.INSTRUCTION_RELOCATE);
            instruction.makeValid();
        }
        
        else 
            error_table.error_add (ErrorTable.INVALID_EXPRESSION, lineno);

        nextToken ();
        
        if (!token.equals ("\n")) {
            error_table.error_add(ErrorTable.INVALID_OPCODE_OPERANDS, lineno);
        }
    }
    
    private void math_argument_two_immediate (boolean bits16) {
        
        // parse the integer
        int immediate = convertNumber (token);
        instruction.storeType(InstructionPacket.INSTRUCTION_FIXED);
        
        // if the register is 16 bits, then just assemble as 16 bit regardless
        if (bits16 && immediate < 0x10000) {
            
            instruction.storeModrm ((short) (instruction.getInstrData()[1] | 0x09));
            instruction.storeImmediateWord(immediate);
            instruction.storeSize (4);
            instruction.makeValid();
        }
        
        else if (!bits16 && immediate < 0x100) {
            
            instruction.storeModrm ((short) (instruction.getInstrData()[1] | 0x08));
            instruction.storeImmediateByte((short) immediate);
            instruction.storeSize (3);
            instruction.makeValid();
        }
        
        else   
            error_table.error_add (ErrorTable.OPERAND_SIZE_MISMATCH, lineno);

        nextToken ();
        
        if (!token.equals ("\n"))          
            error_table.error_add(ErrorTable.INVALID_OPCODE_OPERANDS, lineno);
    }
    
    private void math_argument_two_register (boolean bits16) {
        
        if (!bits16 && RM_VALUES_TABLE.get(token).bits16)
            error_table.error_add (ErrorTable.OPERAND_SIZE_MISMATCH, lineno);
        
        else {
            
            instruction.storeModrm ((short) (instruction.getInstrData()[1] | RM_VALUES_TABLE.get (token).rm_value));
            instruction.storeType (InstructionPacket.INSTRUCTION_FIXED);
            
            nextToken ();
        
            // the next token must be end of line or it's an error
            if (!token.equals ("\n"))         
                error_table.error_add (ErrorTable.INVALID_OPCODE_OPERANDS, lineno);
        
            // we can make this a valid packet now
            instruction.storeSize (2);
            instruction.makeValid ();
        }
    }
    
    private void math_argument_two_label () {
        
        if (instruction.getInstrData()[1] == 0x01 || instruction.getInstrData()[1] == 0x02)    
            error_table.error_add(ErrorTable.OPERAND_SIZE_MISMATCH, lineno);
        
        else {
        
            instruction.storeSize (4);
            instruction.storeModrm ((short) (instruction.getInstrData()[1] | 0x09));
            instruction.storeSymbol(token);
            instruction.storeType (InstructionPacket.INSTRUCTION_RELOCATE);
        
            sym_table.symbol_add(token, address, false);
            nextToken ();
        
            if (!token.equals ("\n"))
                error_table.error_add(ErrorTable.INVALID_OPCODE_OPERANDS, lineno);
        
            else
                instruction.makeValid();
        }
    }
    
    // assemble the argument for a branch instruction
    private void branch_instruction_argument () {
        
        if (immediate_match (token)) {
            
            instruction.storeImmediateWord (convertNumber(token));
            instruction.storeType (InstructionPacket.INSTRUCTION_FIXED);
        }
        
        else if (label_match (token)) {
            
            instruction.storeSymbol (token);
            instruction.storeType (InstructionPacket.INSTRUCTION_RELOCATE);
            sym_table.symbol_add (token, 0, false);
        }
        
        nextToken ();
        
        if (!token.equals ("\n")) {
            
            error_table.error_add(ErrorTable.INVALID_OPCODE_OPERANDS, lineno);
            return;
        }

        instruction.storeSize (3);
        instruction.makeValid();
    }
    
    // assemble the argument for a stack instruction
    private void stack_instruction_argument () {
        
        String curtoken = token;
        nextToken ();
        
        if (token.equals ("\n")) {
            
            // stack instructions are allowed to have any arguments
            if (immediate_match (curtoken)) {
            
                int tokval = convertNumber (curtoken);
                instruction.storeType (InstructionPacket.INSTRUCTION_FIXED);
            
                if (tokval < 0x100) {
                
                    instruction.storeModrm ((short) 0x08);
                    instruction.storeImmediateByte ((short) tokval);
                    instruction.makeValid ();
                    instruction.storeSize (3);
                }
            
                else if (tokval < 0x10000) {
                
                    instruction.storeModrm ((short) 0x09);
                    instruction.storeImmediateWord (tokval);
                    instruction.storeSize (4);
                    instruction.makeValid();
                }
            
                else
                    error_table.error_add(ErrorTable.OPERAND_SIZE_MISMATCH, lineno);
            }
        
            else if (register_match (curtoken)) {
            
                instruction.storeModrm (RM_VALUES_TABLE.get (curtoken).rm_value);
                instruction.storeSize (2);
                instruction.storeType (InstructionPacket.INSTRUCTION_FIXED);
                instruction.makeValid ();
            }
        
            else if (memref_match (curtoken)) {
            
                instruction.storeModrm ((short) 0x0a);
                token = token.substring (1, curtoken.length () - 2);
            
                // must be a label or immediate
                if (label_match (curtoken)) {
                
                    instruction.storeSymbol (curtoken);
                    instruction.storeSize (4);
                    instruction.storeType (InstructionPacket.INSTRUCTION_FIXED);
                    sym_table.symbol_add(curtoken, address, false);
                }
            
                else if (immediate_match (curtoken)) {
                
                    instruction.storeType (InstructionPacket.INSTRUCTION_FIXED);
                    instruction.storeImmediateWord(convertNumber (curtoken));
                    instruction.storeSize (4);
                }
            
                else
                    error_table.error_add (ErrorTable.INVALID_EXPRESSION, lineno);
            
                instruction.makeValid ();
            }
        
            else if (label_match (curtoken)) {
            
                instruction.storeType(InstructionPacket.INSTRUCTION_RELOCATE);
                instruction.storeSymbol (curtoken);
                instruction.storeSize (4);
                sym_table.symbol_add(curtoken, address, false);
            }
        
            else
                error_table.error_add(ErrorTable.INVALID_OPCODE_OPERANDS, lineno);
        }
        
        else
            error_table.error_add (ErrorTable.INVALID_OPCODE_OPERANDS, lineno);
    }
    
    class IncrementInstruction implements InstructionHandler {

        @Override
        public void assemble () {
            
            // this must be a register argument
            if (register_match (token)) {
                
                // opcode = opcode + (register_value - 1)
                int register = RM_VALUES_TABLE.get (token).rm_value - 1;
                instruction.storeOpcode ((short) (instruction.getInstrData()[0] + register));
                
                // see if there is another token
                nextToken ();
                
                if (!token.equals ("\n"))
                    error_table.error_add (ErrorTable.INVALID_OPCODE_OPERANDS, lineno);
                
                else {
                    
                    instruction.storeSize (1);
                    instruction.storeType (InstructionPacket.INSTRUCTION_FIXED);
                    instruction.makeValid();
                }
            }
            
            else
                error_table.error_add(ErrorTable.INVALID_OPCODE_OPERANDS, lineno);
        }
    }
    
    class InterruptInstruction implements InstructionHandler {

        @Override
        public void assemble() {
            
            // must be two bytes only!
            if (immediate_match (token)) {
                
                short intnum = (short) convertNumber (token);
                
                if (intnum < 255 && intnum > 0) {
                
                    instruction.storeSize (2);
                    instruction.storeImmediateByte(intnum);
                    instruction.storeType (InstructionPacket.INSTRUCTION_FIXED);
                    instruction.makeValid();
                }
                
                else
                    error_table.error_add(ErrorTable.OPERAND_SIZE_MISMATCH, lineno);
            }
            
            else
                error_table.error_add (ErrorTable.INVALID_OPCODE_OPERANDS, lineno);
        }
    }
    
    // </editor-fold>
    
    // <editor-fold desc="DIRECTIVE PROCESSING FUNCTIONS">
    
    private void directive () {
        
        // do we have a times directive? Special case if so...
        if (token.equalsIgnoreCase ("times")) {
            
            // process times directive
            directive_times ();
        }
        
        else {
            
            if (token.equalsIgnoreCase ("db"))
                directive.set_unit_size(DirectivePacket.DIRECTIVE_SZ_BYTE);
            else if (token.equalsIgnoreCase ("dw"))
                directive.set_unit_size(DirectivePacket.DIRECTIVE_SZ_WORD);
            else if (token.equalsIgnoreCase ("dd"))
                directive.set_unit_size(DirectivePacket.DIRECTIVE_SZ_DWORD);
            
            nextToken ();
            
            // if there is nothing else, we have an error!
            if (token.equals ("\n")) {
                
                error_table.error_add (ErrorTable.INVALID_EXPRESSION, lineno);
                return;   
            }
            
            else
                directive_argument ();
            
            // add the packet if it is valid
            this.dataList.add (directive);
            address += directive.get_data_length() * directive.get_data_unit_sz();
        }
    }
    
    // process a directive's arguments
    private void directive_argument () {
  
        // see whether this is a string literal or a number
        if (immediate_match (token)) {
            
            int argument = convertNumber (token);
            directive.store_immediate_data(argument);
        }
        
        else if (pattern_match (token, STRLITERAL_PATTERN)) { 
            
            token = token.substring (1, token.length() - 2);
            directive.store_string_data(token);
        }
        
        else if (pattern_match (token, UNCLOSED_PATTERN)) {
            
            error_table.error_add (ErrorTable.UNCLOSED_LITERAL, lineno);
            directive.make_invalid ();
            
            return;
        }
        
        else {
            
            error_table.error_add (ErrorTable.INVALID_EXPRESSION, lineno);
            directive.make_invalid ();
            
            return;
        }
        
        // get the next token
        nextToken ();
        
        if (!token.equals ("\n"))
            directive_argument ();
    }
    
    // process a times directive
    private void directive_times () {
        
    }
    
    // </editor-fold>
    
    // <editor-fold desc="PATTERN MATCHING FUNCTIONS">
    
    private boolean register_match (String s) { return hashmap_match (s.toLowerCase(), RM_VALUES_TABLE); }
    private boolean immediate_match (String s) { return pattern_match (s.toLowerCase (), NUMBER_PATTERN); }
    private boolean memref_match (String s) { return pattern_match (s, MEMREF_PATTERN); }    
    private boolean label_match (String s) { return (!register_match (s) && !immediate_match(s) && !memref_match(s) && pattern_match (s, LABEL_PATTERN)); }
    
    // match a token by pattern
    private boolean pattern_match (String inp, String pattern) {
        
        return Pattern.matches (pattern, inp);
    }
    
    // match a token in a list of tokens
    private boolean list_match (String inp, ArrayList<String> list) {
        
        return list.contains (inp);
    }
    
    // match a token in a hashmap (check for key portion)
    private boolean hashmap_match (String inp, HashMap<String, ?> map) {
        
        return map.containsKey(inp.toLowerCase());
    }
    
// </editor-fold>

    // <editor-fold desc="LOOKUP TABLES / HASHMAPS / PATTERNS AND INITIALIZATION">
    
    /* LOOKUP TABLES / HASHMAPS / PATTERNS*/
    
    private static final HashMap<String, InstructionPair> INSTRUCTION_TABLE;
    static {
        INSTRUCTION_TABLE = new HashMap<>();
        
        // one byte instructions
        INSTRUCTION_TABLE.put ("nop", new InstructionPair (InstructionPair.ONEBYTE_OPCODE, 0x60));
        INSTRUCTION_TABLE.put ("hlt", new InstructionPair (InstructionPair.ONEBYTE_OPCODE, 0x10));
        INSTRUCTION_TABLE.put ("cpuid", new InstructionPair (InstructionPair.ONEBYTE_OPCODE, 0x15));
        INSTRUCTION_TABLE.put ("pshf", new InstructionPair (InstructionPair.ONEBYTE_OPCODE, 0x16));
        INSTRUCTION_TABLE.put ("popf", new InstructionPair (InstructionPair.ONEBYTE_OPCODE, 0x17));
        INSTRUCTION_TABLE.put ("pshall", new InstructionPair (InstructionPair.ONEBYTE_OPCODE, 0x50));
        INSTRUCTION_TABLE.put ("popall", new InstructionPair (InstructionPair.ONEBYTE_OPCODE, 0x51));
        INSTRUCTION_TABLE.put ("jmpdi", new InstructionPair (InstructionPair.ONEBYTE_OPCODE, 0x46));
        INSTRUCTION_TABLE.put ("in", new InstructionPair (InstructionPair.ONEBYTE_OPCODE, 0x55));
        INSTRUCTION_TABLE.put ("out", new InstructionPair (InstructionPair.ONEBYTE_OPCODE, 0x56));
        INSTRUCTION_TABLE.put ("ldstr", new InstructionPair (InstructionPair.ONEBYTE_OPCODE, 0x40));
        INSTRUCTION_TABLE.put ("ststr", new InstructionPair (InstructionPair.ONEBYTE_OPCODE, 0x41));
        INSTRUCTION_TABLE.put ("ret", new InstructionPair (InstructionPair.ONEBYTE_OPCODE, 0x45));
        INSTRUCTION_TABLE.put ("iret", new InstructionPair (InstructionPair.ONEBYTE_OPCODE, 0x46));
        
        // math instructions
        INSTRUCTION_TABLE.put ("sto", new InstructionPair (InstructionPair.MATH_OPCODE, 0x01));
        INSTRUCTION_TABLE.put ("lod", new InstructionPair (InstructionPair.MATH_OPCODE, 0x02));
        INSTRUCTION_TABLE.put ("add", new InstructionPair (InstructionPair.MATH_OPCODE, 0x03));
        INSTRUCTION_TABLE.put ("sub", new InstructionPair (InstructionPair.MATH_OPCODE, 0x04));
        INSTRUCTION_TABLE.put ("mul", new InstructionPair (InstructionPair.MATH_OPCODE, 0x05));
        INSTRUCTION_TABLE.put ("div", new InstructionPair (InstructionPair.MATH_OPCODE, 0x06));
        INSTRUCTION_TABLE.put ("mod", new InstructionPair (InstructionPair.MATH_OPCODE, 0x07));
        INSTRUCTION_TABLE.put ("and", new InstructionPair (InstructionPair.MATH_OPCODE, 0x11));
        INSTRUCTION_TABLE.put ("or", new InstructionPair (InstructionPair.MATH_OPCODE, 0x12));
        INSTRUCTION_TABLE.put ("xor", new InstructionPair (InstructionPair.MATH_OPCODE, 0x13));
        INSTRUCTION_TABLE.put ("cmp", new InstructionPair (InstructionPair.MATH_OPCODE, 0x14));
        INSTRUCTION_TABLE.put ("shl", new InstructionPair (InstructionPair.MATH_OPCODE, 0x0a));
        INSTRUCTION_TABLE.put ("shr", new InstructionPair (InstructionPair.MATH_OPCODE, 0x0b));

        // branch instructions
        INSTRUCTION_TABLE.put ("call", new InstructionPair (InstructionPair.BRANCH_OPCODE, 0x0e));
        INSTRUCTION_TABLE.put ("jmp", new InstructionPair (InstructionPair.BRANCH_OPCODE, 0x0f));
        INSTRUCTION_TABLE.put ("je", new InstructionPair (InstructionPair.BRANCH_OPCODE, 0x1f));
        INSTRUCTION_TABLE.put ("jne", new InstructionPair (InstructionPair.BRANCH_OPCODE, 0x2f));
        INSTRUCTION_TABLE.put ("jg", new InstructionPair (InstructionPair.BRANCH_OPCODE, 0x3f));
        INSTRUCTION_TABLE.put ("jng", new InstructionPair (InstructionPair.BRANCH_OPCODE, 0x4f));
        INSTRUCTION_TABLE.put ("jl", new InstructionPair (InstructionPair.BRANCH_OPCODE, 0x5f));
        INSTRUCTION_TABLE.put ("jnl", new InstructionPair (InstructionPair.BRANCH_OPCODE, 0x6f));
        INSTRUCTION_TABLE.put ("jz", new InstructionPair (InstructionPair.BRANCH_OPCODE, 0x7f));
        INSTRUCTION_TABLE.put ("jnz", new InstructionPair (InstructionPair.BRANCH_OPCODE, 0x8f));
        INSTRUCTION_TABLE.put ("jc", new InstructionPair (InstructionPair.BRANCH_OPCODE, 0x9f));
        INSTRUCTION_TABLE.put ("jnc", new InstructionPair (InstructionPair.BRANCH_OPCODE, 0xaf));
        
        // stack instructions
        INSTRUCTION_TABLE.put ("psh", new InstructionPair (InstructionPair.STACK_OPCODE, 0x08));
        INSTRUCTION_TABLE.put ("pop", new InstructionPair (InstructionPair.STACK_OPCODE, 0x09));
        
        // special instructions
        INSTRUCTION_TABLE.put ("int", new InstructionPair (InstructionPair.SPECIAL_OPCODE, 0x16));
        INSTRUCTION_TABLE.put ("inc", new InstructionPair (InstructionPair.SPECIAL_OPCODE, 0x30));
        INSTRUCTION_TABLE.put ("dec", new InstructionPair (InstructionPair.SPECIAL_OPCODE, 0x37));
    }
    
    private HashMap<String, InstructionHandler> SPECIAL_INSTRUCTIONS;
    public void create_special_instructions () {
        SPECIAL_INSTRUCTIONS = new HashMap<>();
        
        SPECIAL_INSTRUCTIONS.put ("inc", new IncrementInstruction ());
        SPECIAL_INSTRUCTIONS.put ("dec", new IncrementInstruction ());
        SPECIAL_INSTRUCTIONS.put ("int", new InterruptInstruction ());
    }
    
    private static final HashMap<String, RegisterPair> RM_VALUES_TABLE;
    static {
        RM_VALUES_TABLE = new HashMap<>();
        RM_VALUES_TABLE.put ("ra", new RegisterPair (0x01, false));
        RM_VALUES_TABLE.put ("rb", new RegisterPair (0x02, false));
        RM_VALUES_TABLE.put ("rc", new RegisterPair (0x03, true));
        RM_VALUES_TABLE.put ("rx", new RegisterPair (0x04, true));
        RM_VALUES_TABLE.put ("ry", new RegisterPair (0x05, true));
        RM_VALUES_TABLE.put ("si", new RegisterPair (0x06, true));
        RM_VALUES_TABLE.put ("di", new RegisterPair (0x07, true));
    }
    
    private static final ArrayList<String> DIRECTIVES_TABLE = new ArrayList<>(
        Arrays.asList (new String [] { "db", "dw", "dd", "times" }));
    
    private static final String LABEL_PATTERN = "[a-zA-Z0-9_]*:?";
    private static final String MEMREF_PATTERN = "\\[[a-zA-Z0-9_]*\\]";
    private static final String STRLITERAL_PATTERN = "\".*\"|'.*'";
    private static final String UNCLOSED_PATTERN = "\".*|'.*";
    private static final String XDIGIT_PATTERN = "(0x[a-fA-F0-9]*)|([a-fA-F0-9]*(h|H))";
    private static final String DIGIT_PATTERN = "([0-9]*(d|D)?)";
    private static final String BDIGIT_PATTERN = "([0|1]*(b|B))";
    private static final String NUMBER_PATTERN = XDIGIT_PATTERN + "|" + DIGIT_PATTERN + "|" + BDIGIT_PATTERN;
    
// </editor-fold>
}

// InstructionPair: holds an opcode and default modr/m value for an instruction
// created to be stored in a hashmap
class InstructionPair {
    
    public static final int MATH_OPCODE         = 0x00;
    public static final int BRANCH_OPCODE       = 0x01;
    public static final int ONEBYTE_OPCODE      = 0x02;
    public static final int STACK_OPCODE        = 0x03;
    public static final int SPECIAL_OPCODE      = 0x05;
    
    public int category;
    public short opcode;
    
    public InstructionPair (int category, int opcode) {
    
        this.category = category;
        this.opcode = (short) opcode;
    }
}

class RegisterPair {
    
    public short rm_value;
    public boolean bits16;
    
    public RegisterPair (int rm, boolean bits16) {
        
        this.rm_value = (short) rm;
        this.bits16 = bits16;
    }
}

// class SymbolTable: Holds the list of symbols for this program
class SymbolTable {
    
    private final ArrayList<SymtablePacket> symbols;
    
    public SymbolTable () {
        
        symbols = new ArrayList<>();
    }
    
    // add a new symbol to the table
    // if definition, then this is being added as a defined symbol
    public void symbol_add (String sym, int addr, boolean definition) {
        
        int sym_idx = symbol_lookup (sym);
        System.out.println ("Symbol index (" + sym + "): " + sym_idx);
        
        // if the symbol exists...
        if (sym_idx > 0 && definition) {
            
            if (symbol_get (sym_idx).symtype == SymtablePacket.SYMBOL_UNDEF) {
                
                symbol_get(sym_idx).symtype = SymtablePacket.SYMBOL_DEF;
                symbol_get(sym_idx).address = addr;
            }
            
            else if (symbol_get (sym_idx).symtype == SymtablePacket.SYMBOL_DEF)
                symbol_get(sym_idx).symtype = SymtablePacket.SYMBOL_MULTIDEF;
        }
        
        // otherwise, we can just add it
        else {
            
            if (definition)
                symbols.add (new SymtablePacket (sym, addr, SymtablePacket.SYMBOL_DEF));
        }
    }
    
    // get the symbol at idx from the table
    public SymtablePacket symbol_get (int idx) {
        
        if (symbols.size () > idx)
            return symbols.get (idx); 

        return null;
    }
    
    public int symbol_count () { return symbols.size (); }
    
    // get the index of the symbol with label sym
    public int symbol_lookup (String sym) {
        
        int idx = -1;
        
        for (int i = 0; i < symbols.size (); i++) {
            
            if (symbols.get(i).symbol.equals (sym))
                idx = i;
        }
        
        return idx;
    }
}

// hold the list of errors for this program
class ErrorTable {

    public static final int NUMBER_FORMAT_ERROR     = 0;
    public static final int INVALID_INSTRUCTION     = 1;
    public static final int OPERAND_SIZE_MISMATCH   = 2;
    public static final int INVALID_EXPRESSION      = 3;
    public static final int INVALID_OPCODE_OPERANDS = 4;
    public static final int INSTRUCTION_EXPECTED    = 5;
    public static final int UNCLOSED_LITERAL        = 6;
    public static final int FILE_IO_ERROR           = 7;
    
    private final ArrayList<ErrorPacket> error_list;
    private boolean errors;
    
    public ErrorTable () {
        
        errors = false;
        error_list = new ArrayList<>();
    }
    
    public void error_add (int error, int lineno) {
        
        // add the given error to the list
        error_list.add (new ErrorPacket (ERROR_MAP.get(error), lineno));
        errors = true;
    }
    
    public ErrorPacket error_get (int idx) {
        
        return error_list.get (idx);
    }
    
    public int error_count () { return error_list.size (); }
    
    public boolean errors_exist () { return errors; }
    
    // map the errors to their integer values
    private static final HashMap<Integer, String> ERROR_MAP;
    static {
        ERROR_MAP = new HashMap<>();    
    
        ERROR_MAP.put (NUMBER_FORMAT_ERROR, "Invalid number format");
        ERROR_MAP.put (INVALID_INSTRUCTION, "Invalid instruction or directive");
        ERROR_MAP.put (OPERAND_SIZE_MISMATCH, "Operand size mismatch");
        ERROR_MAP.put (INVALID_EXPRESSION, "Invalid expression");
        ERROR_MAP.put (INVALID_OPCODE_OPERANDS, "Invalid combination of opcode and operands");
        ERROR_MAP.put (INSTRUCTION_EXPECTED, "Instruction or directive expected");
        ERROR_MAP.put (UNCLOSED_LITERAL, "Unclosed string literal");
        ERROR_MAP.put (FILE_IO_ERROR, "Error accessing input file");
    }
}

// Packet: All other packets are derived from this class
class ExePacket {
    
    protected boolean valid;
    protected int size;
}

// InstructionPacket: Holds the data for a single instruction
class InstructionPacket extends ExePacket {

    public static final int INSTRUCTION_NONE = -1;
    public static final int INSTRUCTION_FIXED = 0;
    public static final int INSTRUCTION_RELOCATE = 1;
    
    private final short [] instr_data;
    private String symbol;
    private int instrType;
    
    // create a new instruction packet
    public InstructionPacket () {
        
        instr_data = new short []{ -1, -1, -1, -1 };
        symbol = null;
        instrType = INSTRUCTION_NONE;
        size = -1;
        valid = false;
    }
    
    // store the data into the packet
    public void storeOpcode (short opcode) { instr_data[0] = opcode; }
    public void storeModrm  (short modrm) { instr_data[1] = modrm; }
    
    public void storeImmediateByte (short byteval) { 
        
        if (symbol == null)
            instr_data[2] = (short) (byteval &~ 0xff00); 
    }
    
    public void storeImmediateWord (int wordval) {
        
        if (symbol == null) {
            
            instr_data[2] = (short) (wordval &~ 0xff00);
            instr_data[3] = (short) (wordval >> 8);
        }
    }
    
    public void storeSymbol (String symbol) {
        
        if (instr_data[2] == -1 && instr_data[3] == -1)
            this.symbol = symbol;
    }
    
    public void storeType (int type) {
        
        if (type == INSTRUCTION_FIXED || type == INSTRUCTION_RELOCATE)
            instrType = type;
    }
    
    public void storeSize (int size) {
        
        if (size > 0 && size <= 4)
            this.size = size;
    }
    
    public void makeValid () { valid = true; }
    
    public short [] getInstrData () { return instr_data; }
    public String getSymbol () { return symbol; }
    public int getInstrType () { return instrType; }
    public int getSize () { return size; }
    public boolean isValid () { return valid; }
}

// DirectivePacket: Holds the data for a (variable declaring) directive
class DirectivePacket extends ExePacket {
    
    public static final int DIRECTIVE_SZ_BYTE = 1;
    public static final int DIRECTIVE_SZ_WORD = 2;
    public static final int DIRECTIVE_SZ_DWORD = 4;
    
    private final ArrayList<Integer> directive_data;
    private int unit_size;
    private boolean valid;
    
    public DirectivePacket () {
        
        directive_data = new ArrayList<>();
        unit_size = DIRECTIVE_SZ_BYTE;
        valid = true;
    }
    
    // set the number of bytes per data unit
    public void set_unit_size (int sz) {
        
        if (sz == DIRECTIVE_SZ_BYTE || sz == DIRECTIVE_SZ_WORD || 
                sz == DIRECTIVE_SZ_DWORD)
            unit_size = sz;
    }
    
    // add a string to the directive data for this packet
    public void store_string_data (String s) {
        
        for (char c : s.toCharArray())    
            directive_data.add((int)(c));
    }
    
    // add an immediate value to the directive data
    public void store_immediate_data (int imm) { directive_data.add (imm); }
    
    // get the number of data items in this list
    public int get_data_length () { return directive_data.size (); }
    
    // get the data unit at index idx
    public int get_data_at (int idx) {
        
        if (idx < directive_data.size())
            return directive_data.get (idx);
        
        return -1;
    }
    
    // get the data unit size
    public int get_data_unit_sz () { return unit_size; }
    
    // make this packet valid
    public void make_invalid () { valid = true; }
}

// SymtablePacket: Holds the data for a symbol table entry
class SymtablePacket {
    
    public static final int SYMBOL_UNDEF = 0;
    public static final int SYMBOL_DEF = 1;
    public static final int SYMBOL_MULTIDEF = 2;
    
    public String symbol;
    public int address;
    public int symtype;
    
    public SymtablePacket (String sym, int addr, int symtype) {
        
        this.symbol = sym;
        this.address = addr;
        this.symtype = symtype;
    }
}

// ErrorPacket: Holds the data for an error within the code file
class ErrorPacket {
    
    public String errmsg;
    public int lineno;
    
    public ErrorPacket (String msg, int line) {
        
        errmsg = msg;
        lineno = line;
    }
}

// interface InstructionHandler: Define callback function for assembling special instructions
interface InstructionHandler {
    
    public void assemble ();
}