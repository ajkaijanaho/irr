/*  irr - inter-rater reliability calculator
    Copyright © 2014 Antti-Juhani Kaijanaho

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

    1. Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.

    2. Redistributions in binary form must reproduce the above
       copyright notice, this list of conditions and the following
       disclaimer in the documentation and/or other materials provided
       with the distribution.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
    CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
    INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
    MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
    DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS
    BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
    TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
    DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
    ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
    TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
    THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
    SUCH DAMAGE.
 */

package fi.jyu.mit.antkaij.irr;

/* Literature:

   Klaus Krippendorff (2013). Algorithm for boostrapping a
   distribution for cα.
   http://web.asc.upenn.edu/usr/krippendorff/Bootstrapping%20Revised(5).pdf

   Klaus Krippendorff (2011).  Computing Krippendorff's
   Alpha-Reliability.
   http://web.asc.upenn.edu/usr/krippendorff/mwebreliability5.pdf

   Klaus Krippendorff (1980).  Content Analysis.  An Introduction to
   Its Methodology.  Sage.

   References in comments are to Krippendorff 2011 unless otherwise
   indicated.

 */

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Random;

import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.String.format;

public class KrippendorffAlpha {
    public final String variableName;

    public final double value;

    private final double denominator;

    private final List<String> units;
    private final List<String> observers;
    private final List<String> values;

    private final int N;
    private final int m;
    private final int cN;

    private final HashMap<String,Integer> valInx
        = new HashMap<String,Integer>();

    // Krippendorff's n_{uc} aka n_{uk}
    private final int[][] valuesByUnits;

    /* Krippendorff's n_{u*} */
    private final int[] unitSums;

    // coincidence table
    private final double[][] coincidences;

    // Krippendorff's n_c
    private final int[] valSums;

    private final int totalSums; // Krippendorff's n

    public KrippendorffAlpha(DataMatrix dm) {
        this(dm, 20000);
    }

    public KrippendorffAlpha(DataMatrix dm, int resamples) {
        variableName = dm.variableName;

        units = dm.getUnits();
        observers = dm.getObservers();
        values = dm.getValues();

        N  = units.size();
        m  = observers.size();
        cN = values.size();

        for (int i = 0; i < cN; i++) valInx.put(values.get(i), i);

        // Construct the values-by-units table

        valuesByUnits = new int[N][cN];
        for (int u = 0; u < N; u++) {
            for (int o = 0; o < m; o++) {
                String vals = dm.getValue(u, o);
                if (vals == null || vals.isEmpty()) continue;
                int val = valInx.get(vals);
                valuesByUnits[u][val]++;
            }
        }

        unitSums = new int[N];
        for (int u = 0; u < N; u++) {
            int sum = 0;
            for (int c = 0; c < cN; c++) {
                sum += valuesByUnits[u][c];
            }
            unitSums[u] = sum;
        }

        // Construct the coincidence table.
        // Construction clarified by Krippendorff 1980 p. 140.

        coincidences = new double[cN][cN];
        for (int c = 0; c < cN; c++) {
            for (int k = 0; k < cN; k++) {
                double sum = 0;
                for (int u = 0; u < N; u++) {
                    int mu = unitSums[u];
                    if (mu <= 1) continue;
                    int nc = valuesByUnits[u][c];
                    int nk = valuesByUnits[u][k];
                    if (c == k) nk--;
                    sum += (double)(nc*nk)/(mu-1);
                }
                coincidences[c][k] = sum;
            }
        }

        valSums = new int[cN];
        for (int c = 0; c < cN; c++) {
            int sum = 0;
            for (int i = 0; i < N; i++) {
                if (unitSums[i] <= 1) continue;
                sum += valuesByUnits[i][c];
            }
            valSums[c] = sum;
        }

        {
            int sum = 0;
            for (int i = 0; i < N; i++) {
                if (unitSums[i] <= 1) continue;
                sum += unitSums[i];
            }
            totalSums = sum;
        }

        /* Compute the actual alpha, using the formula at Step D.4 in
           http://web.asc.upenn.edu/usr/krippendorff/mwebreliability5.pdf */
        
        double nominator = 0;
        for (int c = 0; c < cN; c++) {
            for (int k = 0; k < cN; k++) {
                // using nominal data metric
                if (k == c) continue;
                nominator += coincidences[c][k];
            }
        }
        nominator /= totalSums;

        long summ = 0;
        for (int c = 0; c < cN; c++) {
            long sum = 0;
            for (int k = 0; k < cN; k++) {
                // using nominal data metric
                if (c == k) continue;
                sum += valSums[k];
            }
            summ += valSums[c] * sum;
        }
        denominator = (double)summ / (totalSums*(totalSums - 1));

        value = 1 - nominator / denominator;

        if (resamples > 0) bootstrap(resamples);
    }

    private int Q() {
        int q = 0;
        for (int c = 0; c < cN; c++) {
            for (int k = 0; k < cN; k++) {
                if (coincidences[c][k] > 0) q++;
            }
        }
        return q;
    }

    private static final int precision = 10000;
    private static final int base = -precision;
    private static int toFP(double x) {
        int rv = (int)round(x*precision)-base;
        if (rv < 0) return 0;
        if (rv >= precision-base) return precision-base;
        return rv;
    }

    private static double fromFP(int fp) {
        return (double)(fp+base)/precision;
    }

    private int[] nalpha;
    private int nalpha_divisor;

    /* Krippendorff 2013 Second Step

       Krippendorff 2013 is annoyingly unclear about the correct
       construction of f.  Read naturally, f is a constant function of
       r evaluating to a function of c and k; it is obvious from how
       it is used later that this is not the intended reading.

       It seems to me the intended reading is this:

       1. Find the smallest c and k such that SUM SUM o_gh / n.. >= r

       2. f(r) := delta_ck / M De for these c and k.
    */
    private double f(double r, double invMDe) {
        double sum = 0;
        int c = 0, k = 0;
        OUTER: for (c = 0; c < cN; c++) {
            for (k = 0; k < cN; k++) {
                sum += coincidences[c][k];
                if (sum/totalSums >= r) break OUTER;
            }
        }

        // using nominal data metric
        return  c == k ? 0 : invMDe;
    }


    // Based on Krippendorff 2013.
    private void bootstrap(int X) {
        // First Step
        final int M = min(25*Q(), totalSums*(m-1)/2);

        // Second Step (partial; see f above for the rest)
        final double invMDe = 1.0/(M*denominator);

        // Third Step
        nalpha = new int[precision-base+1];
        final Random rand = new Random();
        for (int i = 0; i < X; i++) {
            double alpha = 1;
            for (int j = 0; j < M; j++) {
                double r = rand.nextDouble();
                alpha -= f(r, invMDe);
            }
            ++nalpha[toFP(alpha)];
            if (i % (X/20) == 0) {
                System.err.printf("Bootstrap progress %d %%\r", i*100/X);
            }
        }
        System.err.print("                        \r");


        // Fourth Step
        int nx = 0;
        {
            int count = 0;
            for (int c = 0; c < cN; c++) {
                if (coincidences[c][c] > 0) {
                    count++;
                    if (count == 2) break;
                }
            }
            switch (count) {
            case 0:
                break;
            case 1:
                nx = nalpha[toFP(1)];
                nalpha[toFP(1)] = 0;
                break;
            case 2:
                double sum = 0;
                for (int c = 0; c < cN; c++) {
                    sum += pow((double)coincidences[c][c] / totalSums,
                               M);
                }
                nx = (int)round(X*sum);
                nalpha[toFP(1)] -= nx;
                break;
            default:
                System.err.println("Internal Error in Fourth Step");
                System.exit(1);
            }
        }
        
        // Fifth Step
        nalpha_divisor = X - nx;
    }

    public void printDistribution(Writer w) throws IOException {
        w.write("Bootstrapped probability distribution:\n");
        for (int i = -10; i < 10; i++) {
            double low = i / 10.0;
            double high = low + 0.1;
            int lowi = toFP(low);
            int highi = toFP(high);
            if (i == 10) highi++;
            long sum = 0;
            for (int j = lowi; j < highi; j++) {
                sum += nalpha[j];
            }
            double p = (double)sum / nalpha_divisor;
            w.write(format("α ∈ [% 3.1f..% 3.1f%c ", low, high,
                           i == 9 ? ']' : ')'));
            if (p >= 0.01) {
                double op = p;
                while (p > 0) {
                    w.write("*");
                    p -= 1.0/60;
                }
                w.write(format(" %4.2f", op));
            }
            w.write("\n");
        }
    }

    public void print(Writer w) throws IOException {
        w.write("======= KRIPPENDORFF'S ALPHA RELIABILITY =======\n");
        w.write(format("Variable: %s\n", variableName));
        
        w.write(format("α = % .3f\n\n", value));

        printDistribution(w);
        w.write("\n");

        /*
        w.write("Values by units\n");
        for (String s : units) w.write("\t" + s);
        w.write("\n");
        for (int c = 0; c < cN; c++) {
            w.write(values.get(c));
            for (int u = 0; u < N; u++) {
                int count = valuesByUnits[u][c];
                w.write("\t");
                if (count > 0) w.write(""+count);
            }
            w.write("\t");
            w.write(""+valSums[c]);
            w.write("\n");
        }
        for (int i = 0; i < N; i++) {
            w.write("\t");
            w.write(""+unitSums[i]);
        }
        w.write("\t");
        w.write(""+totalSums);
        w.write("\n");
        */

        w.write("Coincidences\n");
        for (String s : values) w.write("\t" + s);
        for (int c = 0; c < cN; c++) {
            w.write("\n" + values.get(c));
            for (int k = 0; k < cN; k++) {
                w.write(format("\t%6.3f", coincidences[c][k]));
            }
        }
        w.write("\n");
    }
}
