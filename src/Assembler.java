import java.io.*;
import java.util.ArrayList;

/*
ASM  := [Label :] Inst
LabelDef := <Addr>
KeySw := FFFB
DIP_A := FFFC
DIP_B := FFFD
SEG_AB := FFFE
SEG_CD := FFFF


Inst := setix <Label> |
		ld <Reg> |
		ld <Reg>, <Imm> |
		st <Reg> |
		st <Imm> |
		st <Imm>
		<ALU> <Reg> |
		cmp |
		nop |
		<jp> <Label>
*/

public class Assembler
{
	public enum ASMCode
	{
		setix, ld, st, add, sub, and, or, not, inc, dec, cmp, nop, jp, jpc, jpz
	}

	public enum CPCode
	{
		SETIXH(0xd0), SETIXL(0xd1), LDIA(0xd8), LDIB(0xd9), LDDA(0xe0), LDDB(0xe1), STDA(0xf0), STDB(0xf4), STDI(0xf8), ADDA(0x80), SUBA(0x81), ANDA(0x82), ORA(0x83), NOTA(0x84), INCA(0x85), DECA(0x86), ADDB(0x90), SUBB(0x91), ANDB(0x92), ORB(0x93), NOTB(0x98), INCB(0x99), DECB(0x9a), CMP(0xa1), NOP(0x00), JP(0x60), JPC(0x40), JPZ(0x50);

		private int code;

		CPCode(final int code)
		{
			this.code=code;
		}

		public int getCode()
		{
			return code;
		}
	}

	private class ASMException extends Exception
	{
		public static final int IllegalAddressOperand=0;
		public static final int IllegalLebelName=1;
		public static final int TooFewOperands=2;
		public static final int TooManyOperands=3;
		public static final int LebelHasBeenDefined=4;
		public static final int LebelHasNotBeenDefined=5;
		public static final int IllegalOperands=6;

		private int errorNumber;
		private String msg;

		ASMException(final String msg)
		{
			this.msg=msg;
		}

		public String toString()
		{
			return msg;
		}
	}

	private class Label
	{
		private class LabelRecord
		{
			private String name;
			private int position;

			LabelRecord(final String name, final int position)
			{
				this.name=name;
				this.position=position;
			}

			public String getName()
			{
				return name;
			}

			public int getPosition()
			{
				return position;
			}
		}

		private ArrayList<LabelRecord> records;

		Label()
		{
			records=new ArrayList<>();
			records.add(new LabelRecord("KeySw", 0xFFFB));
			records.add(new LabelRecord("DIP_A", 0xFFFC));
			records.add(new LabelRecord("DIP_B", 0xFFFD));
			records.add(new LabelRecord("SEG_AB", 0xFFFE));
			records.add(new LabelRecord("SEG_CD", 0xFFFF));
		}

		public void add(final String name, final int position) throws ASMException
		{
			for(LabelRecord r:records)
			{
				if(r.getName().equals(name))
				{
					throw new ASMException("Label Has Already Been defined: \""+name+"\"");
				}
			}
			records.add(new LabelRecord(name, position));
		}

		public int getPositionOf(final String name) throws ASMException
		{
			for(LabelRecord r : records)
			{
				if(r.getName().equals(name))
				{
					return r.getPosition();
				}
			}
			throw new ASMException("Label Has Not Been Defined: \""+name+"\"");
		}

		public String getNameAt(final int position)
		{
			for(LabelRecord r : records)
			{
				if(r.getPosition()==position)
				{
					return r.getName()+": ";
				}
			}
			return "";
		}

	}

	private class MachineCode
	{
		private int address;
		private CPCode cpCode;
		private String operand;
		private int lineNumber;

		public MachineCode(final int address, final CPCode cpCode, final String operand, int lineNumber)
		{
			this.address=address;
			this.cpCode=cpCode;
			this.operand=operand;
			this.lineNumber=lineNumber;
		}

		public int getAddress()
		{
			return address;
		}

		public CPCode getCpCode()
		{
			return cpCode;
		}

		public String getOperand()
		{
			return operand;
		}

		public int getLineNumber()
		{
			return lineNumber;
		}

		public void setOperand(String operand)
		{
			this.operand=operand;
		}

		public String toString()
		{
			return (String.format("%04x\t%02x\t", address, cpCode.getCode())+"-- "+label.getNameAt(address)+cpCode.name()+" "+operand).trim()+"\n";
		}
	}

	private Label label=new Label();
	private ArrayList<MachineCode> machineCode=new ArrayList<>();

	private ASMException exception;


	private String simplify(final String s)
	{
		return s.trim().replaceAll("\\s+", " ");
	}

	public boolean read(final String input)
	{
		try(BufferedReader br=new BufferedReader(new FileReader(new File(input))))
		{
			int address=0;
			int lineNumber=1;
			int ssindex;

			String raw;
			while(null!=(raw=br.readLine()))
			{
				if((ssindex=raw.indexOf("//"))>=0)
				{
					raw=raw.substring(0, ssindex);
				}
				raw=raw.trim().replaceAll("\\s+", " ");
				if(raw.length()==0)
				{
					++lineNumber;
					continue;
				}

				if((ssindex=raw.trim().indexOf(':'))>0)
				{
					if(!raw.substring(0, ssindex).trim().matches("[0-9a-zA-Z]+"))
					{
						throw new ASMException("Illegal Label Name At "+lineNumber+": Input \""+raw.trim()+"\"");
					}
					label.add(raw.substring(0, ssindex).trim(), address);
					raw=raw.substring(ssindex+1, raw.length());
				}

				String[] list=raw.trim().split(" ", 2);

				if(list[0].matches("[0-9a-f]{4}") && list.length==2)
				{
					label.add(list[0], Integer.parseInt(list[1], 16));
					++address;
					continue;
				}

				ASMCode asm=ASMCode.valueOf(list[0]);
				switch(asm)
				{
					case setix:
					{
						if(list.length!=2)
						{
							throw new ASMException("Too Few Operand At "+lineNumber+": Expects \"setix <Address>\", Input \""+raw.trim()+"\"");
						}
						if(!list[1].matches("[0-9a-f]{4}"))
						{
							throw new ASMException("Illegal Address Operand At "+lineNumber+": Input \""+raw.trim()+"\"");
						}
						machineCode.add(new MachineCode(address, CPCode.SETIXH, list[1].substring(0, 2), lineNumber));
						++address;
						++address;
						machineCode.add(new MachineCode(address, CPCode.SETIXL, list[1].substring(2, 4), lineNumber));
						++address;
						break;
					}

					case ld:
					{
						if(list.length<2)
						{
							throw new ASMException("Too Few Operands At "+lineNumber+": Expects \"ld <Register> | ld <Register>, <Immediate>\", Input \""+raw.trim()+"\"");
						}
						String[] opList=list[1].split(",");
						opList[0]=opList[0].trim();
						if(opList[0].equals("a") || opList[0].equals("b"))
						{
							if(opList.length==2)
							{
								if(opList[1].trim().matches("[0-9a-f]{2}"))
								{
									machineCode.add(new MachineCode(address, CPCode.valueOf("LDI"+opList[0].toUpperCase()), opList[1].trim(), lineNumber));
									++address;
								}
								else
								{
									throw new ASMException("Illegal Immediate Operand At "+lineNumber+": Input \""+raw.trim()+"\"");
								}
							}
							else if(opList.length==1)
							{
								machineCode.add(new MachineCode(address, CPCode.valueOf("LDD"+opList[0].toUpperCase()), "", lineNumber));
							}
							else
							{
								throw new ASMException("Too Many Operands At "+lineNumber+": Expects \"ld <Register> | ld <Register>, <Immediate>\", Input \""+raw.trim()+"\"");
							}
						}
						else
						{
							throw new ASMException("Illegal Operands At "+lineNumber+": Expects \"ld <Register> | ld <Register>, <Immediate>\", Input \""+raw.trim()+"\"");
						}
						break;
					}

					case st:
					{
						if(list.length!=2)
						{
							throw new ASMException("Too Few Operands At "+lineNumber+": Expects \"st <Register | st <Register>, <Immediate>\", Input \""+raw.trim()+"\"");
						}
						if(list[1].equals("a") || list[1].equals("b"))
						{
							machineCode.add(new MachineCode(address, CPCode.valueOf("STD"+list[1].toUpperCase()), "", lineNumber));
						}
						else if(list[1].trim().matches("[0-9a-f]{2}"))
						{
							machineCode.add(new MachineCode(address, CPCode.STDI, list[1], lineNumber));
							++address;
						}
						else
						{
							throw new ASMException("Illegal Operands At "+lineNumber+": Expects \"st <Register> | st <Immediate>\", Input \""+raw.trim()+"\"");
						}
						break;
					}

					case add:
					case sub:
					case and:
					case or:
					case not:
					case inc:
					case dec:
					{
						if(list.length!=2)
						{
							throw new ASMException("Too Few Operands At "+lineNumber+": Expects \""+asm.name()+" <Register>\", Input \""+raw.trim()+"\"");
						}
						if(list[1].equals("a") || list[1].equals("b"))
						{
							machineCode.add(new MachineCode(address, CPCode.valueOf(asm.name().toUpperCase()+list[1].toUpperCase()), "", lineNumber));
						}
						else
						{
							throw new ASMException("Illegal Operand At "+lineNumber+": Expects \"" +asm.name()+" <Register>\", Input \""+raw.trim()+"\"");
						}
						break;
					}

					case cmp:
					case nop:
					{
						if(list.length!=1)
						{
							throw new ASMException("Too Many Operands At "+lineNumber+": Expects \""+asm.name()+"\", Input \""+raw.trim()+"\"");
						}
						machineCode.add(new MachineCode(address, CPCode.valueOf(asm.name().toUpperCase()), "", lineNumber));
						break;
					}

					case jp:
					case jpc:
					case jpz:
					{
						if(list.length!=2)
						{
							throw new ASMException("Too Few Operands At "+lineNumber+": Expects \""+asm.name()+" <Label>\", Input \""+raw.trim()+"\"");
						}
						if(!list[1].trim().matches("[0-9a-zA-Z]+"))
						{
							throw new ASMException("Illegal Label Name At "+lineNumber+": Input \""+raw.trim()+"\"");
						}
						machineCode.add(new MachineCode(address, CPCode.valueOf(asm.name().toUpperCase()), list[1], lineNumber));
						++address;
						++address;
						break;
					}
				}

				++address;
				++lineNumber;
			}
			return true;
		}
		catch(FileNotFoundException e)
		{
			System.out.println(e);
			return false;
		}
		catch(IOException e)
		{
			System.out.println(e);
			return false;
		}
		catch(IllegalArgumentException e)
		{
			System.out.println(e);
			return false;
		}
		catch(ASMException e)
		{
			System.out.println(e);
			return false;
		}
	}

	public void write(final String output)
	{
		try(FileWriter fw=new FileWriter(new File(output)))
		{
			ArrayList<String> buffer=new ArrayList<>();

			for(MachineCode mc:machineCode)
			{
				buffer.add(mc.toString());
				switch(mc.getCpCode())
				{
					case SETIXH:
					case SETIXL:
					case LDIA:
					case LDIB:
					case STDI:
					{
						buffer.add(String.format("%04x\t%s\n", mc.getAddress()+1, mc.getOperand()));
						break;
					}

					case JP:
					case JPC:
					case JPZ:
					{
						mc.setOperand(String.format("%04x", label.getPositionOf(mc.getOperand())));
						buffer.add(String.format("%04x\t%s\n", mc.getAddress()+1, mc.getOperand().substring(0, 2)));
						buffer.add(String.format("%04x\t%s\n", mc.getAddress()+2, mc.getOperand().substring(2, 4)));
					}
				}
			}

			for(int i=0; i<buffer.size(); ++i)
			{
				if(i==buffer.size()-1)
				{
					System.out.print(buffer.get(i).substring(0, buffer.get(i).length()-1));
					fw.write(buffer.get(i).substring(0, buffer.get(i).length()-1));
				}
				else
				{
					System.out.print(buffer.get(i));
					fw.write(buffer.get(i));
				}
			}
		}
		catch(IOException e)
		{
			System.out.println(e);
		}
		catch(ASMException e)
		{
			System.out.println(e);
		}
	}
}