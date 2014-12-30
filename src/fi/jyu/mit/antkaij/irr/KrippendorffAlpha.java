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
import static java.util.Arrays.asList;
import static java.util.Arrays.sort;

public class KrippendorffAlpha implements ReliabilityStatistic {
    public final String variableName;

    public final double value;

    private final double denominator;

    private final int scaleType;

    private final List<String> values;

    private final int N;
    private final int m;
    private final int cN;

    private final int X;
    private final int M;

    private final HashMap<String,Integer> valInx
        = new HashMap<String,Integer>();

    // Krippendorff's n_{uc} aka n_{uk}
    private final int[][] valuesByUnits;

    /* Krippendorff's n_{u*} */
    private final int[] unitSums;

    // coincidence table
    private final double[][] coincidences;

    // expected table
    private final double[][] expecteds;

    // Krippendorff's n_c
    private final int[] valSums;

    private final int totalSums; // Krippendorff's n

    public static class Maker implements MakeReliabilityStatistic {
        public String name() { return "Krippendorff's alpha-reliability"; }

        private final DataMatrix dm;
        public Maker(DataMatrix dm) {
            this.dm = dm;
        }
        public ReliabilityStatistic mk() {
            return new KrippendorffAlpha(dm);
        }

    }

    private final double[][] deltaSq;

    public KrippendorffAlpha(DataMatrix dm) {
        this(dm, 20000);
    }

    public KrippendorffAlpha(DataMatrix dm, int resamples) {
        scaleType = dm.scaleType;
        if (scaleType != dm.NOMINAL_SCALE &&
            scaleType != dm.ORDINAL_SCALE) {
            throw new UnsupportedDataException("Only nominal and ordinal " +
                                               "scale currently implemented " +
                                               "for Krippendorff's Alpha");
        }

        variableName = dm.variableName;

        values = dm.getValues();

        N  = dm.getUnits().size();
        m  = dm.getObservers().size();
        cN = values.size();

        for (int i = 0; i < cN; i++) valInx.put(values.get(i), i);

        // Construct the values-by-units table

        valuesByUnits = new int[N][cN];
        for (int u = 0; u < N; u++) {
            for (int o = 0; o < m; o++) {
                int val = dm.getValue(u, o);
                if (val < 0) continue;
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

        expecteds = new double[cN][cN];
        for (int c = 0; c < cN; c++) {
            for (int k = 0; k < cN; k++) {
                if (c == k) {
                    expecteds[c][k] = (double)valSums[c] * (valSums[k]-1)
                        / (totalSums - 1);
                } else {
                    expecteds[c][k] = (double)valSums[c] * valSums[k]
                        / (totalSums - 1);
                }
            }
        }

        deltaSq = new double[cN][cN];
        switch (scaleType) {
        case DataMatrix.NOMINAL_SCALE:
            for (int c = 0; c < cN; c++) {
                for (int k = 0; k < cN; k++) {
                    deltaSq[c][k] = c != k ? 1 : 0;
                }
            }
            break;
        case DataMatrix.ORDINAL_SCALE:
            for (int c = 0; c < cN; c++) {
                for (int k = 0; k < cN; k++) {
                    double sum = 0;
                    if (c < k) {
                        for (int g = c; g <= k; g++) sum += valSums[g];
                    } else {
                        for (int g = k; g <= c; g++) sum += valSums[g];
                    }
                    sum -= (valSums[c] + valSums[k]) / 2.0;
                    deltaSq[c][k] = sum*sum;
                }
            }
            break;
        default:
            throw new Error("unsupported scale");
        }

        /* Compute the actual alpha, using the formula at Step D.4 in
           http://web.asc.upenn.edu/usr/krippendorff/mwebreliability5.pdf */
        
        double nominator = 0;
        for (int c = 0; c < cN; c++) {
            for (int k = 0; k < cN; k++) {
                nominator += coincidences[c][k] * deltaSq[c][k];
            }
        }
        nominator /= totalSums;

        double denominator = 0;
        for (int c = 0; c < cN; c++) {
            for (int k = 0; k < cN; k++) {
                denominator += expecteds[c][k] * deltaSq[c][k];
            }
        }
        denominator /= totalSums;
        this.denominator = denominator;

        value = 1 - nominator / denominator;

        X = resamples;
        
        M = resamples > 0 ? min(25*Q(), totalSums*(m-1)/2) : 0;

        if (resamples > 0) bootstrap();
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

    private double[] samples;
    private int p_divisor;

    /* Krippendorff 2013 Second Step

       Krippendorff 2013 is annoyingly unclear about the correct
       construction of f.  Read naturally, f is a constant function of
       r evaluating to a function of c and k; it is obvious from how
       it is used later that this is not the intended reading.

       It seems to me the intended reading is this:

       1. Find the smallest c and k such that SUM SUM o_gh / n.. >= r

       2. f(r) := delta_ck / M De for these c and k.
    */
    private double f(double r, double MDe) {
        double sum = 0;
        int c = 0, k = 0;
        OUTER: for (c = 0; c < cN; c++) {
            for (k = 0; k < cN; k++) {
                sum += coincidences[c][k];
                if (sum/totalSums >= r) break OUTER;
            }
        }

        return deltaSq[c][k] / MDe;
    }


    // Based on Krippendorff 2013.
    private void bootstrap() {
        // First Step
        // Compute M; computed in the constructor

        // Second Step (partial; see f above for the rest)
        final double MDe = M*denominator;

        // Third Step
        samples = new double[X];
        final Random rand = new Random();
        for (int i = 0; i < X; i++) {
            double alpha = 1;
            for (int j = 0; j < M; j++) {
                double r = rand.nextDouble();
                alpha -= f(r, MDe);
            }
            samples[i] = alpha;
            if (i % (X/20) == 0) {
                System.err.printf("Bootstrap progress %d %%\r", i*100/X);
            }
        }
        System.err.print("                        \r");

        sort(samples);

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
                nx = samples.length;
                while (nx > 0 && samples[nx-1] >= 1) {
                    samples[--nx] = Double.NaN;
                }
                nx = samples.length - nx;
                break;
            case 2:
                double sum = 0;
                for (int c = 0; c < cN; c++) {
                    sum += pow((double)coincidences[c][c] / totalSums,
                               M);
                }
                nx = (int)round(X*sum);
                for (int i = 0; i < nx; i++) {
                    int inx = samples.length - i - 1;
                    if (samples[inx] < 1) break;
                    samples[inx] = Double.NaN;
                }
                break;
            default:
                System.err.println("Internal Error in Fourth Step");
                System.exit(1);
            }
        }
        
        // Fifth Step
        p_divisor = X - nx;
    }

    public PValue pValue(double minimumAlpha) {
        long count = 0;
        for (int i = 0; i < samples.length; i++) {
            if (samples[i] >= minimumAlpha) break;
            ++count;
        }
        return new PValue((double)count / p_divisor, "bootstrapped");
    }
    public ConfidenceInterval confidenceInterval(final double p) {
        final long plow = round((1-p)/2*p_divisor);
        final long phigh = round((1-(1-p)/2)*p_divisor);
        int i;
        for (i = 0; i < samples.length; i++) {
            if (i >= plow) break;
        }
        double min = samples[i];
        for (/* i */; i < samples.length; i++) {
            if (i > phigh) break;
        }
        double max = samples[i > 0 ? i-1 : 0];
        return new ConfidenceInterval(p, min, max);
    }

    public void printDistribution(Writer w) throws IOException {
        w.write(format("Bootstrapped sampling distribution (X = %d, M = %d):\n",
                       X, M));
        int highX10 = -9;
        int count = 0;
        for (int i = 0; /**/; i++) {
            if (i >= samples.length || samples[i] > highX10 / 10.0) {
                double p = (double)count / p_divisor;
                w.write(format("𝛼 ∈ %c% 3.1f, % 3.1f] ",
                               highX10 == -9 ? '[' : ']',
                               (highX10-1)/10.0,
                               highX10/10.0));
                if (p >= 0.01) {
                    double op = p;
                    while (p > 0) {
                        w.write("*");
                        p -= 1.0/60;
                    }
                    w.write(format(" %4.2f", op));
                }
                w.write("\n");

                count = 0;
                ++highX10;
                if (i >= samples.length) break;
            }
            if (!Double.isNaN(samples[i])) ++count;
        }
    }

    public String letter() { return "𝛼"; }
    public String variable() { return variableName; }
    public double pointEstimate() { return value; }

    public List<Double> thresholdValues() {
        return asList(0.9,0.8,0.7,0.667,0.6,0.5,0.4,0.3,0.2,0.1,0.0);
    }

    public void printAdditionalInfo(Writer w) throws IOException {
        if (X > 0) {
            printDistribution(w);
            w.write("\n");
        }

        w.write("Data is ");
        switch (scaleType) {
        case DataMatrix.NOMINAL_SCALE:
            w.write("nominal");
            break;
        case DataMatrix.ORDINAL_SCALE:
            w.write("ordinal");
            break;
        default:
            throw new Error("unsupported scale");
        }
        w.write(".\n\n");

        w.write("Data counts:\n");
        w.write(format(" %5d units\n", N));
        w.write(format(" %5d observers\n", m));
        w.write(format(" %5d values\n", cN));
        w.write(format(" %5d observations\n", totalSums));
        w.write("\n");

        w.write("Coincidences\n");
        for (String s : values) w.write(format("\t%6s", s));
        for (int c = 0; c < cN; c++) {
            w.write("\n" + values.get(c));
            double sum = 0;
            for (int k = 0; k < cN; k++) {
                sum += coincidences[c][k];
                w.write(format("\t%6.3f", coincidences[c][k]));
            }
            w.write(format("\t%6.3f", sum));            
        }
        w.write("\n");
        w.write("\n");
        w.write("Expected coincidences:\n");
        for (String s : values) w.write(format("\t%10s", s));
        for (int c = 0; c < cN; c++) {
            w.write("\n" + values.get(c));
            for (int k = 0; k < cN; k++) {
                w.write(format("\t%10.3f", expecteds[c][k]));
            }
        }
        w.write("\n");
        w.write("\n");
        w.write("Delta\n");
        for (String s : values) w.write(format("\t%10s", s));
        for (int c = 0; c < cN; c++) {
            w.write("\n" + values.get(c));
            for (int k = 0; k < cN; k++) {
                w.write(format("\t%10.3f", deltaSq[c][k]));
            }
        }
        w.write("\n");
        w.write("\n");
    }

}
