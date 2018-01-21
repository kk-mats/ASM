import java.io.*;
import java.util.AbstractMap;
import java.util.ArrayList;

public class CP5
{
	enum Inst
	{
		NOP((byte)0x0),
		LD((byte)0x10), ST((byte)0x11), LEA((byte)0x12),
		ADD((byte)0x20), SUB((byte)0x21), ADDADR((byte)0x22), SUBADR((byte)0x23),
		AND((byte)0x30), OR((byte)0x31), EOR((byte)0x32),
		CPA((byte)0x40), CPL((byte)0x41),
		JPZ((byte)0x60), JMI((byte)0x61), JNZ((byte)0x62), JZE((byte)0x63), JMP((byte)0x64);
		private byte code;
		Inst(final byte code)
		{
			this.code=code;
		}
		public String toString()
		{
			return String.format("%02x", code);
		}
	}

	public class ASMCode
	{
		String label;
		Inst inst;
		String[] operand;
		public ASMCode(final String label, final Inst inst, String[] operand)
		{
			this.label=label;
			this.inst=inst;
			this.operand=operand;
		}

		public String toString()
		{
			return (label.isEmpty() ? "\t\t" : label.length()>4 ? label+"\t" : label+"\t\t")+inst.name()+String.join(", ", operand);
		}
	}

	public class CP5Code
	{
		Inst inst;
		byte r, xr;
		int adr;

		public CP5Code(Inst inst, byte r, byte xr, int adr)
		{
			this.inst=inst;
			this.r=r;
			this.xr=xr;
			this.adr=adr;
		}
		public String toString()
		{
			return String.format("%02X%1X%1X%04X", inst.code, r, xr, adr);
		}
	}

	private ArrayList<AbstractMap.SimpleEntry<String, Integer>> labels=new ArrayList<>();
	private ArrayList<ASMCode> asm=new ArrayList<>();
	private ArrayList<CP5Code> mc=new ArrayList<>();

	public CP5(String input)
	{
		read(input);
	}

	private boolean checkHazzard(int i)
	{
		return false;
	}

	private void read(final String input)
	{
		try(BufferedReader br=new BufferedReader(new FileReader(new File(input))))
		{
			String line;
			int lineNumber=0;

			while(null!=(line=br.readLine()))
			{
				String[] list=line.trim().split(":");
				String label="";
				if(list.length==2)
				{
					labels.add(new AbstractMap.SimpleEntry<>(list[0], lineNumber));
					label=list[0];
					line=list[1];
				}
				list=line.split("\\s", 2);
				Inst inst=Inst.valueOf(list[0].trim());
				String[] operand=new String[0];
				if(list.length>1)
				{
					operand=list[1].split(", ");
					for(int i=0; i<operand.length; ++i)
					{
						operand[i]=operand[i].indexOf("GR")>=0 ? operand[i].replace("GR", "").trim() : operand[i].trim();
					}
				}
				asm.add(new ASMCode(label, inst, operand));
				++lineNumber;
			}
		}
		catch(IOException e)
		{
			System.out.print(e);
		}
	}

	public void assemble()
	{
		ArrayList<String> buffer=new ArrayList<>();
		for(ASMCode ac:asm)
		{
			switch(ac.inst)
			{
				case NOP:mc.add(new CP5Code(ac.inst, (byte)0, (byte)0, 0));
				case JPZ:
				case JMI:
				case JNZ:
				case JMP:
				{
					byte xr=0;
					if(ac.operand.length>1)
					{
						xr=Byte.parseByte(ac.operand[1], 16);
					}
					int adr=labels.stream().filter(l->l.getKey().equals(ac.operand[0])).findAny().get().getValue();
					mc.add(new CP5Code(ac.inst, (byte)0, xr, adr));
				}
				default:
				{
					byte xr=0;
					if(ac.operand.length>2)
					{
						xr=Byte.parseByte(ac.operand[2], 16);
					}
					mc.add(new CP5Code(ac.inst, Byte.parseByte(ac.operand[0]), xr, Integer.parseInt(ac.operand[1], 16)));
				}
			}
		}
	}

	public void write(String output)
	{
		try(FileWriter fw=new FileWriter(new File(output)))
		{
			for(CP5Code m:mc)
			{
				fw.write(m.toString()+"\n");
			}
		}
		catch(IOException e)
		{
			System.out.print(e);
		}
	}
}
