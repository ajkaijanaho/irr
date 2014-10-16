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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import static java.lang.String.format;

public class KrippendorffAlpha {
    public final String variableName;

    public final double value;

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
            for (int k = c+1; k < cN; k++) {
                // using nominal data metric
                nominator += coincidences[c][k];
            }
        }

        long denominator = 0;
        for (int c = 0; c < cN; c++) {
            long sum = 0;
            for (int k = c+1; k < cN; k++) {
                // using nominal data metric
                sum += valSums[k];
            }
            denominator += valSums[c] * sum;
        }

        value = 1 - (totalSums - 1) * (nominator / denominator);
    }

    public void print(Writer w) throws IOException {
        w.write(format("Variable: %s\n", variableName));
        
        w.write(format("Krippendorff alpha = % .3f\n\n", value));

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
