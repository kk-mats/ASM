public class ASM
{
	public static void main(String[] args)
	{
		/*Assembler asm=new Assembler();
		if(asm.read("asm.txt"))
		{
			asm.write("memory.txt");
		}*/
		CP5 c5=new CP5("c5.txt");
		c5.assemble();
		c5.write("rom.txt");
	}
}

