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

import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.sqrt;


public class Util {
    public static double cdf(double x) {
        /* George Marsaglia (2004). Evaluating the Normal Distribution.
           Journal of Statistical Software 11 (4).
           http://www.jstatsoft.org/v11/i05
        */

        if (x < -8) return 0;
        if (x >  8) return 1;

        double s=x;
        double t=0;
        double b=x;
        double q=x*x;
        double i=1;
        while(s!=t) {
            t = s;
            i += 2;
            b *= q/i;
            s += b;
        }
        return .5+s*exp(-.5*q-.91893853320467274178);
    }

    public static double cdfinv(double p) {
        /* A fairly faithful transcription of

           Algorithm AS241 
           The Percentage Points of the Normal Distribution
           by Michael J. Wichura

           Journal of the Royal Statistical Society
           Series C (Applied Statistics)
           37 (3), 477-484
           http://www.jstor.org/stable/2347330

         */

        final double zero = 0;
        final double one = 1;
        final double half = one/2;
        final double split1 = 0.425;
        final double split2 = 5.0;
        final double const1 = 0.180625;
        final double const2 = 1.6;

        final double a0 = 3.3871328727963666080;
        final double a1 = 1.3314166789178437745e2;
        final double a2 = 1.9715909503065514427e3;
        final double a3 = 1.3731693765509461125e4;
        final double a4 = 4.5921953931549871457e4;
        final double a5 = 6.7265770927008700853e4;
        final double a6 = 3.3430575583588128105e4;
        final double a7 = 2.5090809287301226727e3;

        final double b1 = 4.2313330701600911252e1;
        final double b2 = 6.8718700749205790830e2;
        final double b3 = 5.3941960214247511077e3;
        final double b4 = 2.1213794301586595867e4;
        final double b5 = 3.9307895800092710610e4;
        final double b6 = 2.8729085735721942674e4;
        final double b7 = 5.2264952788528545610e3;

        final double c0 = 1.42343711074968357734;
        final double c1 = 4.63033784615654529590;
        final double c2 = 5.76949722146069140550;
        final double c3 = 3.64784832476320460504;
        final double c4 = 1.27045825245236838258;
        final double c5 = 2.41780725177450611770e-1;
        final double c6 = 2.27238449892691845833e-2;
        final double c7 = 7.74545014278341407640e-4;

        final double d1 = 2.05319162663775882187;
        final double d2 = 1.67638483018380384940;
        final double d3 = 6.89767334985100004550e-1;
        final double d4 = 1.48103976427480074590e-1;
        final double d5 = 1.51986665636164571966e-2;
        final double d6 = 5.47593808499534494600e-4;
        final double d7 = 1.05075007164441684324e-9;


        final double e0 = 6.65790464350110377720;
        final double e1 = 5.46378491116411436990;
        final double e2 = 1.78482653991729133580;
        final double e3 = 2.96560571828504891230e-1;
        final double e4 = 2.65321895265761230930e-2;
        final double e5 = 1.24266094738807843860e-3;
        final double e6 = 2.71155556874348757815e-5;
        final double e7 = 2.01033439929228813265e-7;

        final double f1 = 5.99832206555887937690e-1;
        final double f2 = 1.36929880922735805310e-1;
        final double f3 = 1.48753612908506148525e-2;
        final double f4 = 7.86869131145613259100e-4;
        final double f5 = 1.84631831751005468180e-5;
        final double f6 = 1.42151175831644588870e-7;
        final double f7 = 2.04426310338993978564e-15;
        
        double q = p - half;
        double r;
        if (abs(q) <= split1) {
            r = const1 - q * q;
            return q *
                (((((((a7 * r + a6) * r + a5) * r + a4) * r + a3)
                   * r + a2) * r + a1) * r + a0) /
                (((((((b7 * r + b6) * r + b5) * r + b4) * r + b3)
                   * r + b2) * r + b1) * r + one);
        }
        if (q < 0) r = p; else r = one - p;
        if (r <= 0) throw new IllegalArgumentException("Argument out of range");
        r = sqrt(-log(r));
        double rv;
        if (r <= split2) {
            r = r - const2;
            rv =
                (((((((c7 * r + c6) * r + c5) * r + c4) * r + c3)
                   * r + c2) * r + c1) * r + c0) /
                (((((((d7 * r + d6) * r + d5) * r + d4) * r + d3)
                   * r + d2) * r + d1) * r + one);
        } else {
            r = r - split2;
            rv =
                (((((((e7 * r + e6) * r + e5) * r + e4) * r + e3)
                   * r + e2) * r + e1) * r + e0) /
                (((((((f7 * r + f6) * r + f5) * r + f4) * r + f3)
                   * r + f2) * r + f1) * r + one);
        }
        return q < 0 ? -rv : rv;
    }
}