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
 * Create a flat binary image from a ToyAsm assembly language document
 *
 * @author Ben Melikant
 */

public class BinaryFileWriter {
    
    private DataOutputStream outf;
    private boolean errors = false;
    
    public BinaryFileWriter (File out) {
        
        try {
            
            outf = new DataOutputStream (new FileOutputStream (out));
        
        } catch (FileNotFoundException e) {
            
            System.err.println ("Error: Could not open file output stream!");
        }
    }
    
    public boolean errors_exist () { return errors; }
    
    // write the data from the given assembler into the flat binary file
    public void write_binary (Assembler asm) {
        
        for (int i = 0; i < asm.packet_count(); i++) {
            
            if (asm.packet_at(i).getClass() == InstructionPacket.class) {
                
                InstructionPacket p = (InstructionPacket) asm.packet_at(i);
                
                // if the packet is flat (no label references), just assemble it
                if (p.getSymbol() == null) {
                
                    for (int j = 0; j < p.getSize(); j++) {
                        
                        if (p.getInstrData()[j] > -1) {
                        
                            try {
                            
                                outf.writeByte (p.getInstrData()[j]);
                        
                            } catch (IOException e) {
                        
                                System.err.println ("Error writing file data: " + e.toString ());
                                errors = true;
                            }
                        }
                    }
                }
                
                // otherwise, look up the symbol in the table and get its address
                else {
                    
                    int symbol_addr = asm.symbol_at(asm.symbol_find(p.getSymbol())).address;
                    
                    if (symbol_addr >= 0) {
                        
                        try {
                            
                            // write the packet in
                            outf.writeByte (p.getInstrData()[0]);
                            
                            if (p.getInstrData()[1] >= 0)
                                outf.writeByte (p.getInstrData()[1]);
                            
                            short lobyte = (short) (symbol_addr &~ 0xff00);
                            short hibyte = (short) (symbol_addr >> 8);
                            
                            outf.writeByte (lobyte);
                            outf.writeByte (hibyte);
                        
                        } catch (IOException e) {
                            
                            System.err.println ("Error writing file data: " + e.toString ());
                            errors = true;
                        }
                    }
                    
                    else {
                        
                        System.err.println ("Error: symbol undefined - " + asm.symbol_at(asm.symbol_find(p.getSymbol())).symbol);
                        System.err.println ("Defined address: " + asm.symbol_at(asm.symbol_find(p.getSymbol())).address);
                        System.err.println ("The type is: " + asm.symbol_at(asm.symbol_find(p.getSymbol())).symtype);
                        
                        errors = true;
                    }
                }
            }
            
            else if (asm.packet_at(i).getClass() == DirectivePacket.class) {
                
                DirectivePacket p = (DirectivePacket) asm.packet_at(i);
                
                try {
                
                    // write the directive packet to the file
                    if (p.get_data_unit_sz() == DirectivePacket.DIRECTIVE_SZ_BYTE) {
                    
                        for (int j = 0; j < p.get_data_length(); j++) {
                        
                            if (p.get_data_at(j) > 0xff)    
                                System.err.println ("Warning: data value exceeds bounds: " + p.get_data_at(j));
                        
                            outf.writeByte (p.get_data_at(j));
                        }
                    }
                
                    else if (p.get_data_unit_sz () == DirectivePacket.DIRECTIVE_SZ_WORD) {
                    
                        for (int j = 0; j < p.get_data_length (); j++) {
                            
                            if (p.get_data_at(j) > 0xffff)    
                                System.err.println ("Warning: data value exceeds bounds: " + p.get_data_at(j));
                            
                            outf.writeShort ((short) p.get_data_at (j));
                        }
                    }
                
                    else if (p.get_data_unit_sz () == DirectivePacket.DIRECTIVE_SZ_DWORD) {
                    
                        for (int j = 0; j < p.get_data_length (); j++)
                            outf.writeInt ((short) p.get_data_at (j));
                    }
                    
                } catch (IOException e) {
                    
                    System.err.println ("Error writing to binary file: " + e.toString());
                    errors = true;
                }
            }
        }
    }
    
    public void close_stream () {
        
        try {
           
            outf.flush ();
            outf.close ();
            
        } catch (IOException e) {
            
            System.err.println ("Error closing file stream: " + e.toString ());
            errors = true;
        }
    }
}
