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

/* References:

   Joseph L. Fleiss (1971).  Measuring Nominal Scale Agreement Among
   Many Raters. Psychological Bulletin 76 (5), 378-382.

   (Note that the variance formula given by Fleiss 1971 is incorrect.)

   Joseph L. Fleiss & John C. M. Nee & J. Richard Landis (1979).
   Large Sample Variance of Kappa in the Case of Different Sets of
   Raters.  Psychological Bulletin 86 (5), 974-977.

 */

import static java.util.Arrays.asList;

public class FleissKappa implements ReliabilityStatistic {
    private final String variableName;
    private final double value;

    public FleissKappa(DataMatrix dm) {
        variableName = dm.variableName;

        final int uN = dm.getUnits().size();
        final int n = dm.getObservers().size();
        final int k = dm.getValues().size();

        int N = 0;
        int[][] nij = new int[uN][k];
        OUTER: for (int u = 0; u < uN; u++) {
            for (int o = 0; o < n; o++) {
                if (dm.getValue(u,o) < 0) continue OUTER;
            }
            for (int o = 0; o < n; o++) {
                int j = dm.getValue(u, o);
                ++nij[N][j];
            }
            ++N;
        }

        double Pbar = 0;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < k; j++) {
                Pbar += nij[i][j]*nij[i][j];
            }
        }
        Pbar = (Pbar - N*n) / (N*n*(n-1));

        double Pe = 0;
        for (int j = 0; j < k; j++) {
            long sum = 0;
            for (int i = 0; i < N; i++) {
                sum += nij[i][j];
            }
            double pj = (double)sum / (n*N);
            Pe += pj*pj;
        }

        value = (Pbar - Pe) / (1 - Pe);
    }

    public String name()   { return "Fleiss' Kappa"; }
    public String letter() { return "𝜅"; }
    public String variable() { return variableName; }
    public List<Double> thresholdValues() {
        return asList(0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1, 0.0);
    }
    public double pointEstimate() { return value; }
    public ConfidenceInterval confidenceInterval(double p) {
        return new ConfidenceInterval(p,
                                      Double.NaN,
                                      Double.NaN,
                                      "not implemented");
    }
    public PValue pValue(double minValue) {
        return new PValue(Double.NaN, "not implemented");
    }
    public void printAdditionalInfo(Writer w) throws IOException {
    }
}
