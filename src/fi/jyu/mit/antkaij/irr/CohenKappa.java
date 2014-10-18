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

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import static java.lang.Math.min;
import static java.lang.Math.max;
import static java.lang.Math.sqrt;
import static java.util.Arrays.asList;

import static fi.jyu.mit.antkaij.irr.Util.cdf;
import static fi.jyu.mit.antkaij.irr.Util.cdfinv;

/* References:

   Jacob Cohen (1960). A Coefficient of Agreement for Nominal Scales.
   Educational and Psychological Measurement 20 (1), 37-46.

   Joseph L. Fleiss \& Jacob Cohen \& B. S. Everitt (1969).  Large
   Sample Standard Errors of Kappa and Weighted Kappa.  Psychological
   Bulletin 72 (5), 323-327.

 */

public class CohenKappa implements ReliabilityStatistic {
    public final String variableName;
    public final String judgeA;
    public final String judgeB;

    private final double value;
    private final double variance;
    private double se;


    public CohenKappa(DataMatrix dm, int A, int B) {
        variableName = dm.variableName;
        this.judgeA = dm.getObservers().get(A);
        this.judgeB = dm.getObservers().get(B);

        final int N = dm.getValues().size();
        final int M = dm.getUnits().size();

        int[][] matrix = new int[N][N];
        int n = 0;
        for (int u = 0; u < M; u++) {
            int a = dm.getValue(u,A);
            int b = dm.getValue(u,B);
            if (a < 0 || b < 0) continue;
            ++matrix[a][b];
            ++n;
        }
        if (n == 0) {
            value = Double.NaN;
            variance = Double.NaN;
            se = Double.NaN;
            return;
        }
        int[] fiA = new int[N];
        int[] fiB = new int[N];
        for (int v = 0; v < N; v++) {
            for (int w = 0; w < N; w++) {
                fiA[v] += matrix[w][v];
                fiB[v] += matrix[v][w];
            }
        }
        int fc = 0;
        int fo = 0;
        for (int v = 0; v < N; v++) {
            fc += fiA[v]*fiB[v]/n;
            fo += matrix[v][v];
        }

        value = (double)(fo - fc) / (n - fc);

        // Compute Eq. 13 in Fleiss et al 1969.

        final double po = (double)fo / n;
        final double pc = (double)fc / n;

        // compute the first term inside the braces
        double term1 = 0; 
        for (int i = 0; i < N; i++) {
            double toSq =
                (1 - pc) -
                ((double)fiA[i] / n + (double)fiB[i] / n) * (1 - po);
            term1 += (double)matrix[i][i] / n * toSq * toSq;
        }

        // compute the second term inside the braces
        double term2 = 0;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (i == j) continue;
                double toSq = (double)fiA[i] / n + (double)fiB[j] / n;

                term2 += (double)matrix[i][j] / n * toSq * toSq;
            }
        }
        term2 *= (1 - po) * (1 - po);

        // compute the sq root of the third term inside the braces
        double term3toSq = po * pc - 2 * pc + po;

        // compute the expression in braces
        double inBraces = term1 + term2 - (term3toSq * term3toSq);
        
        double toFourth = 1 - pc;
        variance = inBraces / (n * toFourth * toFourth * toFourth * toFourth);
        se = sqrt(variance);
    }

    public String name() { return "Cohen's Kappa"; }
    public String letter() { return "𝜅"; }
    public String variable() {
        return variableName + "(" + judgeA + "," + judgeB + ")";
    }
    public List<Double> thresholdValues() {
        return asList(0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1, 0.0);
    }
    public double pointEstimate() {
        return value;
    }

    public ConfidenceInterval confidenceInterval(double p) {
        double z = cdfinv(1-2*(1-p));
        return new ConfidenceInterval(p,
                                      max(-1, value - z * se),
                                      min(+1, value + z * se));
    }
    public double pValue(double minValue) {
        double z = (value - minValue) / se;
        return 1-cdf(z);
    }
    public void printAdditionalInfo(Writer w) throws IOException {
        w.write(String.format("variance = %.5f\n", variance));
    }
}
