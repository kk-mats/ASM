import java.util.ArrayList;
import java.io.*;

public class SimpleAssembler
{
	public enum Code
	{
		SETIXH(0xd0, 1), SETIXL(0xd1, 1),
		LDIA(0xd8, 1), LDIB(0xd9, 1), LDDA(0xe0, 0), LDDB(0xe1, 0),
		STDA(0xf0, 0), STDB(0xf4, 0), STDI(0xf8, 1),
		ADDA(0x80, 0), SUBA(0x81, 0), ANDA(0x82, 0), ORA(0x83, 0), NOTA(0x84, 0), INCA(0x85, 0), DECA(0x86, 0),
		ADDB(0x90, 0), SUBB(0x91, 0), ANDB(0x92, 0), ORB(0x93, 0), NOTB(0x98, 0), INCB(0x99, 0), DECB(0x9a, 0),
		CMP(0xa1, 0), NOP(0x00, 0),
		JP(0x60, 2), JPC(0x40, 2), JPZ(0x50, 2);

		private int code;
		private int nOperand;

		Code(final int code, final int operand)
		{
			this.code=code;
			this.nOperand=operand;
		}

		public int getCode()
		{
			return code;
		}

		public int getnOperand()
		{
			return nOperand;
		}
	}

	public class Label
	{
		private String name;
		private int index;
		Label(final String name, final int index)
		{
			this.name=name;
			this.index=index;
		}

		public String getName()
		{
			return name;
		}

		public int getIndex()
		{
			return index;
		}
	}

	public class MachineCode
	{
		private int address;
		private Code inst;
		private String operand;
		private String rawCode;
		MachineCode(final int address, final Code inst, final String operand, final String rawCode)
		{
			this.address=address;
			this.inst=inst;
			this.operand=operand;
			this.rawCode=rawCode;
		}

		public int getAddress()
		{
			return address;
		}

		public Code getInst()
		{
			return inst;
		}

		public String getOperand()
		{
			return operand;
		}

		public String getRawCode()
		{
			return rawCode;
		}
	}

	private ArrayList<Label> label;
	private ArrayList<MachineCode> machineCode;

	SimpleAssembler()
	{
		label=new ArrayList<>();
		machineCode=new ArrayList<>();
	}

	public void translate(final String input, final String output)
	{
		read(input);
		write(output);
	}

	private void read(final String input)
	{
		try(BufferedReader br=new BufferedReader(new FileReader(new File(input))))
		{
			ArrayList<String> rawCode=new ArrayList<>();
			int address=0;

			String str;
			while(null!=(str=br.readLine()))
			{
				rawCode.add(str);
			}

			for(String raw:rawCode)
			{
				int labelIndex;
				String c=raw;

				//raw code := [<label> :] <inst> [<nOperand>]
				//remove label
				if((labelIndex=c.indexOf(':'))!=-1)
				{
					label.add(new Label(c.substring(0, labelIndex).trim(), machineCode.size()));
					c=c.substring(labelIndex+1, c.length());
				}

				String[] list=c.split(" ");
				Code inst=Code.valueOf(list[0]);
				if(inst.getnOperand()>0)
				{
					machineCode.add(new MachineCode(address, inst, list[1], raw));
					++address;

					for(int i=0; i<inst.getnOperand(); ++i)
					{
						machineCode.add(new MachineCode(address, null, null, null));
						++address;
					}
				}
				else
				{
					machineCode.add(new MachineCode(address, inst, null, raw));
					++address;
				}
			}
		}
		catch(FileNotFoundException e)
		{
			System.out.println(e);
		}
		catch(IOException e)
		{
			System.out.println(e);
		}
	}

	public void write(final String output)
	{
		try(FileWriter fw=new FileWriter(new File(output)))
		{
			ArrayList<String> buffer=new ArrayList<>();

			for(int i=0; i<machineCode.size(); ++i)
			{
				buffer.add(String.format("%04x\t%x\t--%s\n", machineCode.get(i).address, machineCode.get(i).getInst().getCode(), machineCode.get(i).getRawCode()));

				if(machineCode.get(i).getInst().getnOperand()==2)
				{
					int index=0;
					for(Label l : label)
					{
						if(l.getName().equals(machineCode.get(i).getOperand()))
						{
							index=l.getIndex();
							break;
						}
					}
					String ix=String.format("%04x", machineCode.get(index).getAddress());
					++i;
					buffer.add(String.format("%04x\t%s\n", machineCode.get(i).getAddress(), ix.substring(0, 2)));
					++i;
					buffer.add(String.format("%04x\t%s\n", machineCode.get(i).getAddress(), ix.substring(2, 4)));
				}else if(machineCode.get(i).getInst().getnOperand()==1)
				{
					++i;
					buffer.add(String.format("%04x\t%s\n", machineCode.get(i).getAddress(), machineCode.get(i-1).getOperand()));
				}
			}

			for(String s:buffer)
			{
				fw.write(s);
			}
		}
		catch(IOException e)
		{
			System.out.println(e);
		}
	}
}
