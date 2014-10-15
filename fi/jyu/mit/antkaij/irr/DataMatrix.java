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
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static java.util.Arrays.asList;
import static java.util.Arrays.copyOfRange;
import static java.util.Collections.unmodifiableList;


/** The data matrix for a single variable. */
public class DataMatrix {
    public final String variableName;
    private final String[] observers;

    private final ArrayList<String> units = new ArrayList<String>();

    private final ArrayList<ArrayList<String>> matrix
        = new ArrayList<ArrayList<String>>();

    private final TreeSet<String> values = new TreeSet<String>();

    public String getValue(int unit, int observer) {
        ArrayList<String> obs = matrix.get(unit);
        if (obs == null) return null;
        if (observer >= obs.size()) return null;
        return obs.get(observer);
    }

    public List<String> getUnits() {
        return unmodifiableList(units);
    }
    public List<String> getObservers() {
        return unmodifiableList(asList(observers));
    }

    public List<String> getValues() {
        return unmodifiableList(new ArrayList<String>(values));
    }
    
    private void addUnit(String name, String[] observations) {
        units.add(name);
        matrix.add(new ArrayList<String>(asList(observations)));
        values.addAll(asList(observations));
        values.remove("");
    }

    private DataMatrix(String variableName,
                       String[] observers) {
        this.variableName = variableName;
        this.observers = observers;
    }
                       
    public static DataMatrix parse(LineNumberReader r) throws IOException {
        String line = r.readLine();
        while (line != null && line.matches(",*")) line = r.readLine();
        if (line == null) return null;
        String[] hdr = line.split(",");
        if (hdr.length == 0) throw new RuntimeException("hdr.length == 0" +
                                                        " on line " +
                                                        r.getLineNumber());
        DataMatrix dm = new DataMatrix(hdr[0],
                                       copyOfRange(hdr, 1, hdr.length));
        line = r.readLine();
        while (line != null && line.length() > 0 && line.charAt(0) != ',') {
            String[] li = line.split(",");
            if (li.length == 0) throw new RuntimeException("li.length == 0" +
                                                           " on line " +
                                                           r.getLineNumber());
            if (li.length > 1) {
                dm.addUnit(li[0], copyOfRange(li, 1, li.length));
            }
            line = r.readLine();
        }
        return dm;
    }

    private static void printField(Writer w, String s, int n)
        throws IOException {
        w.write(s);
        n -= s.length();
        for (int i = 0; i < n; i++) {
            w.write(' ');
        }
    }

    public void print(Writer w) throws IOException {
        ArrayList<Integer> cols = new ArrayList<Integer>();
        {
            int fstcol = variableName.length();
            for (String u : units) {
                if (u.length() > fstcol) fstcol = u.length();
            }
            cols.add(fstcol);
        }
        for (int i = 0; i < observers.length; i++) {
            int col = observers[i].length();
            for (ArrayList<String> al : matrix) {
                if (al.size() <= i) continue;
                int len = al.get(i).length();
                if (len > col) col = len;
            }
            cols.add(col);
        }
        printField(w, variableName, cols.get(0));
        for (int i = 0; i < observers.length; i++) {
            String s = observers[i];
            w.write(" ");
            printField(w, s, cols.get(i+1));
        }
        w.write("\n");
        for (int i = 0; i < units.size(); i++) {
            printField(w, units.get(i), cols.get(0));
            ArrayList<String> al = matrix.get(i);
            for (int j = 0; j < al.size(); j++) {
                w.write(" ");
                printField(w, al.get(j), cols.get(j+1));
            }
            w.write("\n");
        }
        w.write("Values:");
        for (String v : values) {
            w.write("\t");
            w.write(v);
        }
        w.write("\n");
    }

}
