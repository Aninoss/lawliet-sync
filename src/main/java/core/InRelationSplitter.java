package core;

import java.util.ArrayList;

public class InRelationSplitter {

    private final ArrayList<Integer> relationalElements = new ArrayList<>();
    private final int outMax;

    public InRelationSplitter(int outSum) {
        this.outMax = outSum;
    }

    public InRelationSplitter insert(int relational) {
        relationalElements.add(relational);
        return this;
    }

    public int[] process() {
        int[] out = new int[relationalElements.size()];
        double[] rest = new double[relationalElements.size()];
        int sum = getRelationalSum();
        int remaining = outMax;

        for (int i = 0; i < relationalElements.size(); i++) {
            int v = relationalElements.get(i) * outMax / sum;
            out[i] = v;
            rest[i] = (relationalElements.get(i) * outMax / (double)sum) - v;
            remaining -= v;
        }

        for(; remaining > 0; remaining--) {
            int index = posBiggest(rest);
            out[index]++;
            rest[index] = 0;
        }

        return out;
    }

    private int getRelationalSum() {
        return relationalElements.stream().mapToInt(e -> e).sum();
    }

    private int posBiggest(double[] arr) {
        int index = -1;
        double max = 0;

        for (int i = 0; i < arr.length; i++) {
            double v = arr[i];
            if (index < 0 || v > max) {
                max = v;
                index = i;
            }
        }

        return index;
    }


}
