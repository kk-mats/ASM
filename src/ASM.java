public class ASM
{
	public static void main(String[] args)
	{
		Assembler asm=new Assembler();
		if(asm.read("asm.txt"))
		{
			asm.write("memory.txt");
		}
	}
}

