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
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;

import static java.util.Arrays.asList;
import static java.lang.Math.round;

public class IRR {
    private static void print(ReliabilityStatistic stat) throws IOException {
        System.out.printf("======= %s =======\n", stat.name());
        System.out.printf("Variable: %s\n", stat.variable());
        System.out.printf("%s = % .3f\n\n",
                          stat.letter(),
                          stat.pointEstimate());
        System.out.print("Confidence intervals:\n");
        for (int pper : asList(95, 99)) {
            ConfidenceInterval ci
                = stat.confidenceInterval((double)pper/100);
            System.out.printf("%d %% CI % .3f to % .3f",
                              pper, ci.min, ci.max);
            if (ci.note != null) {
                System.out.printf(" (%s)", ci.note);
            }
            System.out.print("\n");
        }
        System.out.print("\n");
        System.out.print("Significance tests:\n");
        for (double val : stat.thresholdValues()) {
            PValue p = stat.pValue(val);
            System.out.printf("%s ≤ %5.3f p = %5.3f",
                              stat.letter(),
                              val, p.p);
            if (p.sName != null) {
                if (p.note != null) {
                    System.out.printf(" (%s = % 7.3f, %s)",
                                      p.sName, p.sValue, p.note);
                } else {
                    System.out.printf(" (%s = % 7.3f)", p.sName, p.sValue);
                }
            } else if (p.note != null) {
                    System.out.printf(" (%s)", p.note);
            }
            System.out.print("\n");
        }
        System.out.print("\n");
        OutputStreamWriter w = new OutputStreamWriter(System.out);
        stat.printAdditionalInfo(w);
        w.flush();
        System.out.print("\n");
    }

    public static void main(String[] args) throws IOException {
        try (LineNumberReader r =
             new LineNumberReader(new java.io.InputStreamReader(System.in))) {
                while (true) {
                    DataMatrix dm = DataMatrix.parse(r);
                    if (dm == null) break;
                    print(new KrippendorffAlpha(dm));
                    print(new FleissKappa(dm));
                    final int m = dm.getObservers().size();
                    for (int i = 0; i < m; i++) {
                        for (int j = i+1; j < m; j++) {
                            print(new CohenKappa(dm, i, j));
                        }
                    }
                }
            }
    }
}
