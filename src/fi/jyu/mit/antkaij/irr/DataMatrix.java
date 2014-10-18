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
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import static java.util.Arrays.asList;
import static java.util.Arrays.copyOfRange;
import static java.util.Collections.unmodifiableList;


/** The data matrix for a single variable. */
public class DataMatrix {
    public final String variableName;
    private final String[] observers;

    private final String[] units;

    private final int[][] matrix;

    private final String[] values;

    public int getValue(int unit, int observer) {
        return matrix[unit][observer];
    }

    public List<String> getUnits() {
        return unmodifiableList(asList(units));
    }
    public List<String> getObservers() {
        return unmodifiableList(asList(observers));
    }

    public List<String> getValues() {
        return unmodifiableList(asList(values));
    }
    
    private DataMatrix(String variableName,
                       String[] observers,
                       String[] units,
                       String[] values,
                       int[][] matrix) {
        this.variableName = variableName;
        this.observers = observers;
        this.units = units;
        this.values = values;
        this.matrix = matrix;
    }
                       
    public static DataMatrix parse(LineNumberReader r) throws IOException {
        String line = r.readLine();
        while (line != null && line.matches(",*")) line = r.readLine();
        if (line == null) return null;
        String[] hdr = line.split(",");
        if (hdr.length == 0) throw new RuntimeException("hdr.length == 0" +
                                                        " on line " +
                                                        r.getLineNumber());
        final String variableName = hdr[0];
        final String[] observers = copyOfRange(hdr, 1, hdr.length);

        final ArrayList<String> units = new ArrayList<String>();
        final ArrayList<String> values = new ArrayList<String>();
        final ArrayList<ArrayList<Integer>> rows =
            new ArrayList<ArrayList<Integer>>();
        final HashMap<String,Integer> valueIndex =
            new HashMap<String,Integer>();

        line = r.readLine();
        while (line != null && line.length() > 0 && line.charAt(0) != ',') {
            String[] li = line.split(",");
            if (li.length == 0) throw new RuntimeException("li.length == 0" +
                                                           " on line " +
                                                           r.getLineNumber());
            if (li.length > 1) {
                int u = units.size();
                units.add(li[0]);
                ArrayList<Integer> row = new ArrayList<Integer>();
                rows.add(row);
                for (int i = 1; i < observers.length + 1; i++) {
                    if (i >= li.length || li[i].isEmpty()) {
                        row.add(-1);
                        continue;
                    }
                    Integer val = valueIndex.get(li[i]);
                    if (val == null) {
                        val = values.size();
                        values.add(li[i]);
                        valueIndex.put(li[i], val);
                    }
                    row.add(i-1, val);
                }
            }
            line = r.readLine();
        }
        int[][] matrix = new int[units.size()][observers.length];
        for (int u = 0; u < units.size(); u++) {
            for (int o = 0; o < observers.length; o++) {
                matrix[u][o] = rows.get(u).get(o);
            }
        }
        return new DataMatrix(variableName,
                              observers,
                              units.toArray(new String[units.size()]),
                              values.toArray(new String[values.size()]),
                              matrix);
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
        int cols[] = new int[observers.length+1];
        {
            int fstcol = variableName.length();
            for (String u : units) {
                if (u.length() > fstcol) fstcol = u.length();
            }
            cols[0] = fstcol;
        }
        for (int o = 0; o < observers.length; o++) {
            int col = observers[o].length();
            for (int u = 0; u < units.length; u++) {
                int v = matrix[u][o];
                int len = v >= 0 ? values[v].length() : 0;
                if (len > col) col = len;
            }
            cols[o+1] = col;
        }
        printField(w, variableName, cols[0]);
        for (int o = 0; o < observers.length; o++) {
            String s = observers[o];
            w.write(" ");
            printField(w, s, cols[o+1]);
        }
        w.write("\n");
        for (int u = 0; u < units.length; u++) {
            printField(w, units[u], cols[0]);
            for (int o = 0; o < observers.length; o++) {
                w.write(" ");
                int v = matrix[u][o];
                printField(w, (v >= 0 ? values[v] : ""), cols[o+1]);
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
