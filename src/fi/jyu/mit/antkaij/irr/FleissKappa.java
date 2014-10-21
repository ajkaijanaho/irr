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

/* Reference for the statistic itself:

   Joseph L. Fleiss (1971).  Measuring Nominal Scale Agreement Among
   Many Raters. Psychological Bulletin 76 (5), 378-382.

   Regarding variance, that source is incorrect, as pointed out by

   Joseph L. Fleiss & John C. M. Nee & J. Richard Landis (1979).
   Large Sample Variance of Kappa in the Case of Different Sets of
   Raters.  Psychological Bulletin 86 (5), 974-977.

   Their formula, in turn, assumes random allocation and thus is only
   applicable to testing the null hypothesis 𝜅 = 0.  What I need here
   is, however, a variance formula that makes no such assumption, for
   constructing confidence intervals and for performing hypthesis
   testing with a variety of null hypotheses.  The following paper
   presents one such variance formula, and I use it here.

   Kilem Li Gwet (2008).  Computing inter-rater reliability and its
   variance in the presence of high agreement.  British Journal of
   Mathematical and Statistical Psychology 61 (1), 29-48.
   doi:10.1348/000711006X126600


 */

import static java.lang.Math.min;
import static java.lang.Math.max;
import static java.lang.Math.sqrt;
import static java.util.Arrays.asList;

import static fi.jyu.mit.antkaij.irr.Util.cdf;
import static fi.jyu.mit.antkaij.irr.Util.cdfinv;

public class FleissKappa implements ReliabilityStatistic {
    private final String variableName;
    private final double value;
    private final double variance;
    private final double se;
    private final int n;
    
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
        this.n = N;

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

        // Variance according to Gwet, formula 33.

        // Gwet's i is our i
        // Gwet's k is our j
        // Gwet's n is our N
        // Gwet's q is our k
        // Gwet's r is our n
        // Gwet's pe|pi is our Pe
        // Gwet's gamma(hat)pi is our value
        // I will ignore Gwet's f (treat it as f = 0)

        double vsum = 0;
        for (int i = 0; i < N; i++) {
            double pai = 0;
            double pei = 0;
            for (int j = 0; j < k; j++) {
                pai += (nij[i][j]*(nij[i][j]-1))/(n*(n-1));
                pei += nij[i][j]/n;
            }
            pei *= value;

            double gi = (pai - Pe) / (1 - Pe);

            double gistar = gi - 2 * (1 - value) * (pei - Pe) / (1 - Pe);
            vsum += (gistar - value) * (gistar - value);
        }
        variance =  vsum / (N*(N-1));
        se = sqrt(variance);
    }

    public String name()   { return "Fleiss' Kappa"; }
    public String letter() { return "𝜅"; }
    public String variable() { return variableName; }
    public List<Double> thresholdValues() {
        return asList(0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1, 0.0);
    }
    public double pointEstimate() { return value; }
    public ConfidenceInterval confidenceInterval(double p) {
        double z = cdfinv(1-(1-p)/2);
        return new ConfidenceInterval(p,
                                      max(-1, value - z * se),
                                      min(+1, value + z * se));
    }
    public PValue pValue(double minValue) {
        double z = (value - minValue) / se;
        return new PValue(1 - cdf(z), "z", z, "upper tail");
    }
    public void printAdditionalInfo(Writer w) throws IOException {
        w.write(String.format("variance = %.5f\n", variance));
        w.write(String.format("n = %d\n", n));
        if (n < 30) {
            w.write("\nWARNING: The sample is small.\n");
            w.write("The assumptions underlying the variance are probably " +
                    "invalid.\n");
            w.write("This means the CONFIDENCE INTERVALS and SIGNIFICANCE " +
                    "TESTS are\n");
            w.write("probably INVALID as well.\n");
        }
    }
}
