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

   Klaus Krippendorff (2011).  Computing Krippendorff's
   Alpha-Reliability.
   http://web.asc.upenn.edu/usr/krippendorff/mwebreliability5.pdf

   Klaus Krippendorff (1980).  Content Analysis.  An Introduction to
   Its Methodology.  Sage.

   References in comments are to Krippendorff 2011 unless otherwise
   indicated.

 */

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class KrippendorffAlpha {
    public final double value;

    public KrippendorffAlpha(DataMatrix dm) {
        final List<String> units = dm.getUnits();
        final List<String> observers = dm.getObservers();
        final List<String> values = dm.getValues();

        final int N  = units.size();
        final int m  = observers.size();
        final int vN = values.size();

        final HashMap<String,Integer> valInx = new HashMap<String,Integer>();
        for (int i = 0; i < vN; i++) valInx.put(values.get(i), i);

        // Krippendorff's n_{uc} aka n_{uk}
        final int[][] valuesByUnits = new int[N][vN];
        for (int u = 0; u < N; u++) {
            for (int o = 0; o < m; o++) {
                String vals = dm.getValue(u, o);
                if (vals == null || vals.isEmpty()) continue;
                int val = valInx.get(vals);
                valuesByUnits[u][val]++;
            }
        }
        final int[] unitSums = new int[N]; /* Krippendorff's n_{u*} */
        for (int u = 0; u < N; u++) {
            int sum = 0;
            for (int c = 0; c < vN; c++) {
                sum += valuesByUnits[u][c];
            }
            unitSums[u] = sum;
        }

        // coincidence table
        // construction clarified by Krippendorff 1980 p. 140
        final double[][] coincidences = new double[vN][vN];
        for (int c = 0; c < vN; c++) {
            for (int k = 0; k < vN; k++) {
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

        // Krippendorff's n_{*c}
        final int[] valSums = new int[vN];
        for (int c = 0; c < vN; c++) {
            int sum = 0;
            for (int i = 0; i < N; i++) {
                if (unitSums[i] <= 1) continue;
                sum += valuesByUnits[i][c];
            }
            valSums[c] = sum;
        }

        int totalSums = 0; // Krippendorff's n_{**}
        for (int i = 0; i < N; i++) {
            if (unitSums[i] <= 1) continue;
            totalSums += unitSums[i];
        }

        /* Compute the actual alpha, using the formula at Step E.4 in
           http://web.asc.upenn.edu/usr/krippendorff/mwebreliability5.pdf */
        
        double nominator = 0;
        for (int u = 0; u < N; u++) {
            long sum = 0;
            for (int c = 0; c < vN; c++) {
                for (int k = c+1; k < vN; k++) {
                    // using nominal data metric
                    sum += valuesByUnits[u][c] * valuesByUnits[u][k];
                }
            }
            if (sum != 0) nominator += (double)sum / (unitSums[u]-1);
        }

        long denominator = 0;
        for (int c = 0; c < vN; c++) {
            long sum = 0;
            for (int k = c+1; k < vN; k++) {
                // using nominal data metric
                sum += valSums[k];
            }
            denominator += valSums[c] * sum;
        }

        value = 1 - (totalSums - 1) * (nominator / denominator);
        
        System.out.print("VALUES BY UNITS\n");
        for (String s : units) System.out.print("\t" + s);
        System.out.print("\n");
        for (int c = 0; c < vN; c++) {
            System.out.print(values.get(c));
            for (int u = 0; u < N; u++) {
                int count = valuesByUnits[u][c];
                System.out.print("\t");
                if (count > 0) System.out.print(count);
            }
            System.out.print("\t");
            System.out.print(valSums[c]);
            System.out.print("\n");
        }
        for (int i = 0; i < N; i++) {
            System.out.print("\t");
            System.out.print(unitSums[i]);
        }
        System.out.print("\t");
        System.out.print(totalSums);
        System.out.print("\n");
        
        System.out.print("COINCIDENCES\n");
        for (String s : values) System.out.print("\t" + s);
        for (int c = 0; c < vN; c++) {
            System.out.print("\n" + values.get(c));
            for (int k = 0; k < vN; k++) {
                System.out.printf("\t%2.3f", coincidences[c][k]);
            }
        }
        System.out.print("\n");
    }
}
