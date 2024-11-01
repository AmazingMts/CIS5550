package cis5550.test;

import cis5550.flame.*;

public class FlameOutput {
	public static void run(FlameContext ctx, String args[]) throws Exception {
		ctx.output("Worked");
		ctx.output(", and the arguments are: ");
		for (int i=0; i<args.length; i++)
			ctx.output(((i>0) ? "," : "")+args[i]);
	}
}