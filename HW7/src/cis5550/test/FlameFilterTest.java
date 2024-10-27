package cis5550.test;

import cis5550.flame.*;
import java.util.*;

public class FlameFilterTest {
    public static void run(FlameContext ctx, String args[]) throws Exception {
        // Step 1: Initialize input list with the args provided
        LinkedList<String> list = new LinkedList<>();
        for (String arg : args) {
            list.add(arg);
        }

        // Step 2: Define the filter condition (e.g., keep strings that start with 'a')
        List<String> out = ctx.parallelize(list)
                .filter(s -> s.startsWith("a")) // Adjust lambda as needed
                .collect();

        // Step 3: Sort the output to make it easier to verify the order
        Collections.sort(out);

        // Step 4: Format the output as a comma-separated string
        String result = String.join(",", out);

        // Step 5: Output the result
        ctx.output(result);
    }
}
