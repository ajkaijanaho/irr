# Measuring interrater agreement #

This piece of software implements a number of interrater agreement statistics.

## The problem ##

The basic problem that these statistics aim to solve is the evaluation of the reliability of an inexact method of measuring a particular variable.  Typically these variables do not describe physical processes but involve human factors or socially constructed reality. Equally typically, there is no gold-standard measurement method that everyone agrees gives correct answers.  Also typical is that the measurement process involves a human being making a subjective judgment based on predefined criteria.

The basic setup of this sort of measurement is that there are a number of _units_, each of which is assigned a single _value_ by a (usually human) _observer_.  In the general case, there may be several observers, who each assign a single value to those units they observe, but different observers may assign different values to the same unit.  In the general case, it is not required that all observers observe all units.  However, it is usually assumed that the values are mutually exclusive and exhaustive, meaning that a single observation results in exactly one value for one unit.

**Example**.  Consider a study in which research papers are classified as to the research method employed.  The papers being classified are the _units_.  The researchers (or research assistants) actually performing the classification are the _observers_.  _Values_ include controlled experiment and survey.  (In real life, this would be complicated by the fact that some papers may report more than one study, each having a different method; I will ignore that problem here.)

The problem with this sort of measurement is that depending on who is doing the observing (and on how tired they are et cetera), the same unit may not get the same value if the observation is repeated by asking another observer to make another observation, even if nothing has really changed, or even asking the same observer to make another observation of the same unit.  Thus, there is an inherent and very real measurement error involved.

A study using this sort of measurement process should always conduct a reliability evaluation exercise.  The standard way to do this is to test interrater agreement.  This means subjecting all of the units, or more commonly a sample of them, to multiple observations by different observers, and then evaluating how much they disagree.

I use on this page the term "interrater", though the literature also uses "intercoder".  There is a subtle difference in philosophy between these terms, but they are close enough to be equivalent for this page's purposes.

## Interrater agreement statistics ##

A number of statistics have been proposed in the literature that result in a single real number quantifying the disagreement between raters.  This program implements some of the better known statistics.

* Cohen's kappa works only if there are two observers who have each observed every unit once.  It discounts systematic disagreement about the value distribution.  It is inappropriate if there are more than two observers, and it is arguably inappropriate even in that case.

* Fleiss's kappa works if every unit has received the same number of observations, but it does not require that those observations have been made by the same set of observers for each unit.  It is inappropriate if the number of observations vary.

* Krippendorff's alpha makes no restrictions as to the manner the observations have been generated.

### Nominal, ordinal, and interval values ###

Different sets of values come with different ways to evaluate the severity of disagreement among observations.

* Sometimes all disagreements are equally bad.  In that case, we say that the data is _nominal_.

* Sometimes the values are naturally arranged into a sequence, and the severity of disagreement depends on how far apart the observed values are in that sequence.  In that case, we say that the data is _ordinal_.

* Sometimes the values are numeric, and the severity of disagreement is simply a matter of computing their difference.

There are other ways to evaluate the severity of disagreement, but these three are the only ones supported by this program at this time.

**Example**.  The three values _controlled experiment_, _survey_, and _other_, are nominal, since there is no preferred ordering between the three values.

**Example**. The three values _not an experiment_, _quasi-experiment_, and _experiment_, are ordinal, since there is a natural ordering of them (the one I listed them in).

This program supports nominal data for all three statistics.  For ordinal and interval data, only Krippendorff's alpha is currently supported.

Using a statistic meant for ordinal or interval data on nominal data, or a statistic meant for interval data on ordinal data, is erroneous, since it results in a too optimistic value of the statistic.  Conversely, using a statistic meant for nominal data on ordinal or interval data, or a statistic meant for ordinal data on interval data, is merely suboptimal (not erroneous).

### Interpreting the statistics ###

All three statistics are real numbers between -1 and 1.  Greater values indicate better agreement, smaller values indicate worse disagreement.  The value 0 indicates agreement purely by chance; the value 1 indicates perfect agreement.

A positive value that is much closer to 0 than 1 usually indicates too weak an agreement to be of much use; a negative value indicates this more emphatically.  Thus, a minimum required value should be agreed upon before a statistic is computed, and if a value smaller than that minimum of the statistic results, the measurement error should be considered too great to accept.  Krippendorff suggests a minimum value of 𝛼= .667 for considering a variable at all, and 𝛼= .8 for relying on a variable for firm conclusions.  However, the minimum value should be chosen for each study separately in light of how serious a problem it would be for the study to generate false data.

If the statistic of a variable falls below the agreed minimum value, this should normally be considered to indicate a problem in the standards used by the observers.  This should in the ordinary case cause the existing measurements to be disregarded and usually prompt a redesign of the standards and a new round of fresh measurement.  It is thus usually a good idea to pilot the measurement standards before embarking on applying it to the full dataset.

In some extraordinary cases, a statistic value below the agreed minimum value may reflect an idiosyncratic behavior of the statistic instead of a real problem in the measurement process.  The discussion of when that is likely to happen is beyond the scope of this page.

A study should always compute and report the appropriate statistic for each variable separately, and not compute any sort of aggregate.  When more than one variable is analyzed together, the smallest value of a statistic among the variables should be considered the value of the statistic for them together.

### Confidence intervals and significance tests ###

This program reports confidence intervals and a number of (one-tailed) statistical significance tests for each statistic computed.  These estimate the effect of sampling error caused by conducting the reliability evaluation exercise on a random sample of units that is much smaller than the full set of units; if the exercise was conducted on the full set of units, the confidence intervals and significance tests are meaningless.  Similarly, they become very unreliable if the sample is very small (roughly, less than 30 units for the kappas and less than 30 total observations for the alpha).

The proper way to interpret the confidence intervals (assuming a large enough random sample that is much smaller than the full set of units) is to see if they lie completely above the agreed minimum value or not.  Similarly, the proper way to interpret the significance tests is to see if the p-value associated with the agreed minimum value falls below a predetermined threshold.

Note that some other statistical software will compute a p-value for kappa only under the null hypothesis that the kappa is zero; because of that, many researchers report such significance tests.  However, since positive values close to zero are about as useless as the zero value itself, this is not a proper way to evaluate the significance of kappa.  Instead, as I suggested in the previous paragraph, the null hypothesis should be that the kappa is below the agreed minimum value.

## Input format ##

Data to this program should be input as a spreadsheet saved in CSV (comma-separated values) format.

The data for a variable is presented by having the rows represent units and the columns represent observers.  The first row should give names or other identifiers for the observers, and the first column should give names or other identifiers for the units.  The topmost leftmost cell names the variable.  The cell at the intersection of row belonging to observer O and column belonging to unit U either is empty or contains the value given by O to U.

If a variable has ordinal data, then there should be a declaration row immediately above the row that contains the name of the variable.  The leftmost cell of the declaration row must contain `ORDINAL` (all capitals), and the possible values must then be listed on subsequent cells in the correct order.

If a variable has interval data, then there should be a declaration row immediately above the row that contains the name of the variable.  The leftmost cell of the declaration row must contain `INTERVAL` (all capitals).  All other cells on that row must be empty.

For nominal data, no declaration row is used.

More than one variable may be given in the same file by separating them by at least one empty row.

For example, the following spreadsheet contains two variables, one with ordinal data and one with nominal data.

<table>
  <tr><td>ORDINAL</td><td>NEX</td><td>NF</td><td>EX</td><td>CEX</td><td>RCT</td><td></td></tr>
  <tr><td>type</td><td>Carl</td><td>Elaine</td><td>Rowan</td><td>William</td><td>Susan</td><td>Zoë</td></tr>
  <tr><td>01_gannon</td><td>NF</td><td>CEX</td><td>CEX</td><td>CEX</td><td>CEX</td><td>RCT</td></tr>
  <tr><td>02_sadowski</td><td>NEX</td><td>NEX</td><td>NEX</td><td>NEX</td><td>NEX</td><td></td></tr>
  <tr><td>03_madeyski</td><td>NF</td><td>NF</td><td>CEX</td><td></td><td>CEX</td><td>CEX</td></tr>
  <tr><td>04_saal</td><td>NEX</td><td>NEX</td><td>NEX</td><td>NEX</td><td>NEX</td><td>CEX</td></tr>
  <tr><td>05_flanagan</td><td></td><td>NEX</td><td>NEX</td><td>NEX</td><td>NEX</td><td>EX</td></tr>
  <tr><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
  <tr><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
  <tr><td>topic</td><td>Greg</td><td>Leah</td></tr>
  <tr><td>01_gannon</td><td>SEQ</td><td>SYN</td></tr>
  <tr><td>02_sadowski</td><td>MUL</td><td></td></tr>
  <tr><td>03_madeyski</td><td>AOI</td><td>AOI</td></tr>
  <tr><td>04_saal</td><td>USG</td><td>USG</td></tr>
  <tr><td>05_flanagan</td><td>MUL</td><td>MUL</td></tr>
</table>

In CSV format, the same data looks like this:

    ORDINAL,NEX,NF,EX,CEX,RCT,
    type,Carl,Elaine,Rowan,William,Susan,Zoë
    01_gannon,NF,CEX,CEX,CEX,CEX,RCT
    02_sadowski,NEX,NEX,NEX,NEX,NEX,
    03_madeyski,NF,NF,CEX,,CEX,CEX
    04_saal,NEX,NEX,NEX,NEX,NEX,CEX
    05_flanagan,,NEX,NEX,NEX,NEX,EX
    ,,,,,,
    ,,,,,,
    topic,Greg,Leah
    01_gannon,SEQ,SYN
    02_sadowski,MUL,
    03_madeyski,AOI,AOI
    04_saal,USG,USG
    05_flanagan,MUL,MUL




## Literature ##

Note that this is not an exhaustive bibliography.

### General ###

* Ron Artstein & Massimo Poesio (2008).  Inter-coder Agreement for Computational Linguistics.  Computational Linguistics 34 (4), 555-596.  [Full text available](http://dx.doi.org/10.1162/coli.07-034-R2)
* Guangchao Charles Feng (2014). Intercoder reliability indices.  Disuse, misuse, and abuse.  Quality & Quantity 48 (3), 1083-1815.  [Full text available](http://dx.doi.org/10.1007/s11135-013-9956-8)
* Andrew F. Hayes & Klaus Krippendorff (2007).  Answering the Call for a Standard Reliability Measure for Coding Data.  Communication Methods and Measures 1 (1), 77-89. [Full text available](http://dx.doi.org/10.1080/19312450709336664)
* Klaus Krippendorff (2004).  Reliability in Content Analysis.  Some Common Misconceptions and Recommendations.  Human Communication Research 30 (3), 411-433. [Full text available](http://dx.doi.org/10.1111/j.1468-2958.2004.tb00738.x)
* Klaus Krippendorff (2013).  Content Analysis.  An Introduction to Its Methodology.  3rd ed.  SAGE. [JYKDOK](https://jykdok.linneanet.fi/vwebv/holdingsInfo?bibId=1238803)
* Matthew Lombard & Jennifer Snyder-Duch & Cheryl Campanella Bracken (2002).  Content Analysis in Mass Communication.  Assessment and Reporting of Intercoder Reliability.  Human Communication Research 28 (4) 587-604. [Full text available](http://dx.doi.org/10.1111/j.1468-2958.2002.tb00826.x)

### Cohen's Kappa ###

* Jacob Cohen (1960). A Coefficient of Agreement for Nominal Scales.  Educational and Psychological Measurement 20 (1), 37-46.  [JYKDOK](https://jykdok.linneanet.fi/vwebv/holdingsInfo?bibId=184401)
* Joseph L. Fleiss & Jacob Cohen & B. S. Everitt (1969).  Large Sample Standard Errors of Kappa and Weighted Kappa.  Psychological Bulletin 72 (5), 323-327. [Full text available](http://bib.fi/xLWgsg/global)
* Julius Sim & Chris C. Wright (2005).  The Kappa Statistic in Reliability Studies.  Use, Interpretation, and Sample Size Requirements.  Physical Therapy  85 (3), 257-268. [Full text available](http://ptjournal.apta.org/content/85/3/257)

### Fleiss's Kappa ###

* Joseph L. Fleiss (1971).  Measuring Nominal Scale Agreement Among Many Raters. Psychological Bulletin 76 (5), 378-382. [Full text available](http://bib.fi/ReHhpg/global)
* Kilem Li Gwet (2008).  Computing inter-rater reliability and its variance in the presence of high agreement.  British Journal of Mathematical and Statistical Psychology 61 (1), 29-48. [Full text available](http://dx.doi.org/10.1348/000711006X126600)

### Krippendorff's Alpha ###

* Klaus Krippendorff (2008).  Systematic and Random Disagreement and the Reliability of Nominal Data.  Communication Methods and Measures 2 (4), 323-338.  [Full text available](http://dx.doi.org/10.1080/19312450802467134)
* Klaus Krippendorff 2013 (see under General, above)
* [Documents on Krippendorff's website](http://web.asc.upenn.edu/usr/krippendorff/dogs.html)
