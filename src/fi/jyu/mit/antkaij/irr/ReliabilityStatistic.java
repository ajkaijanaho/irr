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

interface ReliabilityStatistic {
    /** Letter used of the statistic in mathematical formulas. */
    String letter();
    /** Variable whose data has been analyzed. */
    String variable();
    /** List of statistic values for which significance tests should
     * be conducted. */
    List<Double> thresholdValues();
    /** The point estimate value of the statistic. */
    double pointEstimate();
    /** Construct a confidence interval for the specified significance
     * level. */
    ConfidenceInterval confidenceInterval(double p);
    /** Compute a significance-test probability for failure to reach
     * the minValue. */
    PValue pValue(double minValue);
    /** Print any additional info regarding the analysis. */
    void printAdditionalInfo(Writer w) throws IOException;
}
