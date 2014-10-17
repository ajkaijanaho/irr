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

public class IRR {
    public static void main(String[] args) throws IOException {
        try (LineNumberReader r =
             new LineNumberReader(new java.io.InputStreamReader(System.in))) {
                while (true) {
                    DataMatrix dm = DataMatrix.parse(r);
                    if (dm == null) break;
                    ReliabilityStatistic stat = new KrippendorffAlpha(dm);
                    System.out.printf("======= %s =======\n",
                                      stat.name());
                    System.out.printf("Variable: %s\n", stat.variable());
                    System.out.printf("%s = % .3f\n\n",
                                      stat.letter(),
                                      stat.pointEstimate());
                    System.out.print("Confidence intervals:\n");
                    for (int pperc : asList(95, 99)) {
                        ConfidenceInterval ci =
                            stat.confidenceInterval((100-pperc) / 100.0);
                        System.out.printf("%d %% CI % .3f to % .3f\n",
                                          pperc, ci.min, ci.max);
                    }
                    System.out.print("\n");
                    System.out.print("Significance tests:\n");
                    for (double val : stat.thresholdValues()) {
                        System.out.printf("%s < %.3f p = %.3f\n",
                                          stat.letter(),
                                          val, stat.pValue(val));
                    }
                    System.out.print("\n");
                    OutputStreamWriter w = new OutputStreamWriter(System.out);
                    stat.printAdditionalInfo(w);
                    w.flush();
                    System.out.print("\n");
                }
            }
    }
}
