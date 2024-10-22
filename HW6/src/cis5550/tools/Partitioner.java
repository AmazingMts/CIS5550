package cis5550.tools;

import java.util.*;

public class Partitioner {

    public class Partition {
        public String kvsWorker;
        public String fromKey;
        public String toKeyExclusive;
        public String assignedFlameWorker;

        Partition(String kvsWorkerArg, String fromKeyArg, String toKeyExclusiveArg, String assignedFlameWorkerArg) {
            kvsWorker = kvsWorkerArg;
            fromKey = fromKeyArg;
            toKeyExclusive = toKeyExclusiveArg;
            assignedFlameWorker = assignedFlameWorkerArg;
        }

        Partition(String kvsWorkerArg, String fromKeyArg, String toKeyExclusiveArg) {
            kvsWorker = kvsWorkerArg;
            fromKey = fromKeyArg;
            toKeyExclusive = toKeyExclusiveArg;
            assignedFlameWorker = null;
        }

        public String toString() {
            return "[kvs:" + kvsWorker + ", keys: " + (fromKey == null ? "" : fromKey) + "-"
                    + (toKeyExclusive == null ? "" : toKeyExclusive) + ", flame: " + assignedFlameWorker + "]";
        }
    };

    boolean sameIP(String a, String b) {
        String aPcs[] = a.split(":");
        String bPcs[] = b.split(":");
        return aPcs[0].equals(bPcs[0]);
    }

    Vector<String> flameWorkers;
    Vector<Partition> partitions;
    boolean alreadyAssigned;
    int keyRangesPerWorker;

    public Partitioner() {
        partitions = new Vector<Partition>();
        flameWorkers = new Vector<String>();
        alreadyAssigned = false;
        keyRangesPerWorker = 1;
    }

    public void setKeyRangesPerWorker(int keyRangesPerWorkerArg) {
        keyRangesPerWorker = keyRangesPerWorkerArg;
    }

    public void addKVSWorker(String kvsWorker, String fromKeyOrNull, String toKeyOrNull) {
        partitions.add(new Partition(kvsWorker, fromKeyOrNull, toKeyOrNull));
    }

    public void addFlameWorker(String worker) {
        flameWorkers.add(worker);
    }

    public Vector<Partition> assignPartitions() {
        if (alreadyAssigned || (flameWorkers.size() < 1) || partitions.size() < 1)
            return null;

        Random rand = new Random();

        /*
         * let's figure out how many partitions we need based on keyRangesPerWorker and
         * the number of flame workers
         */
        int requiredNumberOfPartitions = flameWorkers.size() * this.keyRangesPerWorker;

        // let's sort the partitions by key so we can identify a partition using binary
        // search
        partitions.sort((e1, e2) -> {
            if (e1.fromKey == null) {
                return -1;
            } else if (e2.fromKey == null) {
                return 1;
            } else {
                return e1.fromKey.compareTo(e2.fromKey);
            }
        });

        // create a hashset of current split points to avoid creating empty partitions
        // (unlikely)
        HashSet<String> currSplits = new HashSet<>();
        for (int i = 1; i < partitions.size(); i++) { // assume that first partition has fromKey == null
            currSplits.add(partitions.elementAt(i).fromKey);
        }

        int additionalSplitsNeededPerOriginalPartition = (int) Math.ceil(requiredNumberOfPartitions / partitions.size())
                - 1;

        if (additionalSplitsNeededPerOriginalPartition > 0) {
            Vector<Partition> allPartitions = new Vector<>();

            for (int i = 0; i < partitions.size(); i++) {
                Partition p = partitions.get(i);
                int count = 0;
                String fromKey = p.fromKey;
                String toKeyExclusive = p.toKeyExclusive;
                ArrayList<String> newSplits = new ArrayList<String>();

                while (count < additionalSplitsNeededPerOriginalPartition) {
                    String split = null;

                    do {
                        split = rand.ints(97, 123).limit(5)
                                .collect(StringBuilder::new, StringBuilder::appendCodePoint,
                                        StringBuilder::append)
                                .toString();
                    } while (currSplits.contains(split) && ((fromKey != null &&
                            split.compareTo(fromKey) < 0)
                            || (toKeyExclusive != null &&
                                    split.compareTo(toKeyExclusive) >= 0)));

                    count += 1;

                    currSplits.add(split);
                    newSplits.add(split);
                }

                newSplits.sort((e1, e2) -> e1.compareTo(e2));
                allPartitions.add(new Partition(p.kvsWorker, fromKey, newSplits.get(0)));
                for (int j = 0; j < newSplits.size() - 1; j++) {
                    allPartitions.add(new Partition(p.kvsWorker, newSplits.get(j),
                            newSplits.get(j + 1)));
                }
                allPartitions.add(new Partition(p.kvsWorker, newSplits.get(newSplits.size() - 1),
                        toKeyExclusive));

            }
            partitions = allPartitions;
        }

        /*
         * Now we'll try to evenly assign partitions to workers, giving preference to
         * workers on the same host
         */

        int numAssigned[] = new int[flameWorkers.size()];
        for (int i = 0; i < numAssigned.length; i++)
            numAssigned[i] = 0;

        for (int i = 0; i < partitions.size(); i++) {
            int bestCandidate = 0;
            int bestWorkload = 9999;
            for (int j = 0; j < numAssigned.length; j++) {
                if ((numAssigned[j] < bestWorkload) || ((numAssigned[j] == bestWorkload)
                        && sameIP(flameWorkers.elementAt(j), partitions.elementAt(i).kvsWorker))) {
                    bestCandidate = j;
                    bestWorkload = numAssigned[j];
                }
            }

            numAssigned[bestCandidate]++;
            partitions.elementAt(i).assignedFlameWorker = flameWorkers.elementAt(bestCandidate);
        }

        /* Finally, we'll return the partitions to the caller */

        alreadyAssigned = true;
        return partitions;
    }

    public static void main(String args[]) {
        Partitioner p = new Partitioner();
        p.setKeyRangesPerWorker(1);

        p.addKVSWorker("10.0.0.1:1001", null, "ggggg"); // last kvs worker, id = "xxxxx"
        p.addKVSWorker("10.0.0.2:1002", "ggggg", "mmmmm"); //1st kvs worker, id = "ggggg"
        p.addKVSWorker("10.0.0.3:1003", "mmmmm", "sssss"); //2nd kvs worker, id = "mmmmm"
        p.addKVSWorker("10.0.0.4:1004", "sssss", "xxxxx"); //3rd kvs worker, id = "sssss"
        p.addKVSWorker("10.0.0.1:1001", "xxxxx", null); // last kvs worker, id = "xxxxx"

        p.addFlameWorker("10.0.0.1:2001");
        p.addFlameWorker("10.0.0.2:2002");
        p.addFlameWorker("10.0.0.3:2003");
        p.addFlameWorker("10.0.0.4:2004");

        Vector<Partition> result = p.assignPartitions();
        for (Partition x : result)
            System.out.println(x);
    }
}
