/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ToyAsm;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/*
 * ObjectFileWriter: Write data to a .to object file
 * Writes data in the toypc object file format (.to)
 *
 * @author Ben Melikant
 */
public class ObjectFileWriter {
    
    private File outf;
    private DataOutputStream outp;
    
    public ObjectFileWriter (File out) throws FileNotFoundException {
        
        outf = out;
        outp = new DataOutputStream (new FileOutputStream (outf));
    }
    
    public void writeCode (Assembler asm) {
        
        // write out the instruction table
        for (int i = 0; i < asm.packet_count(); i++) {
            
            ExePacket ep = asm.packet_at(i);
            
            if (ep.getClass() == InstructionPacket.class) {
            
                InstructionPacket p = (InstructionPacket) ep;
                
                try {
            
                    if (p.getInstrType() == InstructionPacket.INSTRUCTION_FIXED)
                        outp.writeByte ('F');
                    else if (p.getInstrType() == InstructionPacket.INSTRUCTION_RELOCATE)
                        outp.writeByte ('R');
            
                    outp.writeByte (p.getSize());
            
                    if (p.getInstrData()[0] != -1)
                        outp.writeByte (p.getInstrData()[0]);
                    if (p.getInstrData()[1] != -1)
                        outp.writeByte (p.getInstrData()[1]);
                    if (p.getInstrData()[2] != -1)
                        outp.writeByte (p.getInstrData()[2]);
                    if (p.getInstrData()[3] != -1)
                        outp.writeByte (p.getInstrData()[3]);
            
                    if (p.getSymbol() != null)
                        outp.writeBytes (p.getSymbol());
            
                    outp.flush ();
            
                } catch (IOException e) {
            
                    System.err.println ("Error writing instruction packet to file: " + e.toString());
                }
            }
            
            else if (ep.getClass() == DirectivePacket.class) {
                
                DirectivePacket p = (DirectivePacket) ep;
                
                try {
                    
                    outp.writeByte ((byte) 'd');
                    outp.writeByte ((byte) p.get_data_unit_sz());
                    outp.writeByte ((byte) p.get_data_length());
                    
                    // write the integer data out to the object file
                    if (p.get_data_unit_sz () == DirectivePacket.DIRECTIVE_SZ_BYTE) {
                        
                        for (int j = 0; j < p.get_data_length (); j++) {
                            
                            if (p.get_data_at(j) > 0xff)    
                                System.err.println ("Warning: data exceeds bounds of byte. Truncated - " + p.get_data_at(j));
                            
                            outp.writeByte ((byte) p.get_data_at (j));
                        }
                    }
                    
                    else if (p.get_data_unit_sz ()== DirectivePacket.DIRECTIVE_SZ_WORD) {
                        
                        for (int j = 0; j < p.get_data_length (); j++) {
                            
                            if (p.get_data_at(j) > 0xffff)    
                                System.err.println ("Warning: data exceeds bounds of byte. Truncated - " + p.get_data_at(j));
                            
                            outp.writeShort ((short) p.get_data_at (j));
                        }
                    }
                    
                    else {
                        
                        for (int j = 0; j < p.get_data_length(); j++)    
                            outp.writeInt (p.get_data_at(j));
                    }
                    
                } catch (IOException e) {
                    
                    System.err.println ("Error writing directive packet to file: " + e.toString ());
                }
            }
        }
        
        // start the symbol table
        
        // now write out the symbol table
        for (int i = 0; i < asm.symbol_count(); i++) {
            
            
        }
    }
    
    public void closeObjectFile () {
        
        try {
            
            outp.flush ();
            outp.close ();
        
        } catch (IOException e) {
        
            System.err.println ("Error when closing object file: " + e.toString ());
        }
    }
}
