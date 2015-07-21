/**
 *  SumInt.java 
 *  This file is part of JaCoP.
 *
 *  JaCoP is a Java Constraint Programming solver. 
 *	
 *	Copyright (C) 2000-2008 Krzysztof Kuchcinski and Radoslaw Szymanek
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  Notwithstanding any other provision of this License, the copyright
 *  owners of this work supplement the terms of this License with terms
 *  prohibiting misrepresentation of the origin of this work and requiring
 *  that modified versions of this work be marked in reasonable ways as
 *  different from the original version. This supplement of the license
 *  terms is in accordance with Section 7 of GNU Affero General Public
 *  License version 3.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.jacop.constraints;

import java.util.ArrayList;
import java.util.HashMap;

import org.jacop.core.IntVar;
import org.jacop.core.IntDomain;
import org.jacop.core.Store;
import org.jacop.core.Var;

/**
 * SumInt constraint implements the weighted summation over several
 * variables . 
 *
 * sum(i in 1..N)(xi) = sum
 *
 * It provides the weighted sum from all variables on the list.
 * The weights are integers.
 *
 * This implementaiton is based on 
 * "Bounds Consistency Techniques for Long Linear Constraints"
 * by Warwick Harvey and Joachim Schimpf
 *
 * @author Krzysztof Kuchcinski
 * @version 4.3
 */

public class SumInt extends PrimitiveConstraint {

    Store store;
    
    static int idNumber = 1;

    boolean reified = true;

    /**
     * Defines relations
     */
    final static byte eq=0, le=1, lt=2, ne=3, gt=4, ge=5;

    /**
     * Defines negated relations
     */
    final static byte[] negRel= {ne, //eq=0, 
				 gt, //le=1, 
				 ge, //lt=2, 
				 eq, //ne=3, 
				 le, //gt=4, 
				 lt  //ge=5;
    };

    /**
     * It specifies what relations is used by this constraint
     */

    public byte relationType;

    /**
     * It specifies a list of variables being summed.
     */
    IntVar x[];

    /**
     * It specifies variable for the overall sum. 
     */
    IntVar sum;

    /**
     * It specifies the number of variables. 
     */
    int l;

    /**
     * It specifies "variability" of each variable
     */
    int[] I;
    
    /**
     * It specifies sum of lower bounds (min values) and sum of upper bounds (max values)
     */
    int sumXmin, sumXmax;

    /**
     * It specifies the arguments required to be saved by an XML format as well as 
     * the constructor being called to recreate an object from an XML format.
     */
    public static String[] xmlAttributes = {"list", "sum"};

    /**
     * @param list
     * @param weights
     * @param sum
     */
    public SumInt(Store store, IntVar[] list, String rel, IntVar sum) {

	commonInitialization(store, list, sum);
	this.relationType = relation(rel);
	

    }
	
    /**
     * It constructs the constraint SumInt. 
     * @param variables variables which are being multiplied by weights.
     * @param weights weight for each variable.
     * @param sum variable containing the sum of weighted variables.
     */
    public SumInt(Store store, ArrayList<? extends IntVar> variables,
			String rel, IntVar sum) {

		commonInitialization(store, variables.toArray(new IntVar[variables.size()]), sum);
		this.relationType = relation(rel);
    }


    private void commonInitialization(Store store, IntVar[] list, IntVar sum) {


	this.store = store;
	this.sum = sum;
	this.x = list;
	numberId = idNumber++;

	this.l = x.length;
	this.I = new int[l];
	    
	checkForOverflow();

	if (l <= 2)
	    queueIndex = 0;
	else
	    queueIndex = 1;

    }

    @Override
    public ArrayList<Var> arguments() {

	ArrayList<Var> variables = new ArrayList<Var>(x.length+1);

	for (Var v : x)
	    variables.add(v);

	variables.add(sum);

	return variables;
    }


    @Override
    public void consistency(Store store) {

	computeInit();

	do {

	    store.propagationHasOccurred = false;

	    switch (relationType) {
	    case eq:

		pruneLtEq();
		pruneGtEq();

		if (sumXmax == sumXmin && sum.singleton() && sum.value() == sumXmin) 
		    removeConstraint();

		break;

	    case le:
		pruneLtEq();

		if (sumXmax <= sum.min()) 
		    removeConstraint();
		break;

	    case lt:
		pruneLt();

		if (sumXmax < sum.min()) 
		    removeConstraint();
		break;
	    case ne:
		pruneNeq();

		if (sumXmin == sumXmax && sum.singleton() && sumXmin != sum.value())
		    removeConstraint();
		break;
	    case gt:

		pruneGt();

		if (sumXmin > sum.max())
		    removeConstraint();		
		break;
	    case ge:

		pruneGtEq();

		if (sumXmin >= sum.max())
		    removeConstraint();

		break;
	    }

	} while (store.propagationHasOccurred);
    }

    @Override
    public void notConsistency(Store store) {
		
	computeInit();

	do {

	    store.propagationHasOccurred = false;

	    switch (negRel[relationType]) {
	    case eq:

		pruneLtEq();
		pruneGtEq();

		if (sumXmax == sumXmin && sum.singleton() && sumXmin == sum.value()) 
		    removeConstraint();

		break;

	    case le:
		pruneLtEq();

		if (sumXmax <= sum.min()) 
		    removeConstraint();
		break;

	    case lt:
		pruneLt();

		if (sumXmax < sum.min()) 
		    removeConstraint();
		break;
	    case ne:
		pruneNeq();

		if (sumXmin == sumXmax && sum.singleton() && sumXmin == sum.value())
		    removeConstraint();
		break;
	    case gt:

		pruneGt();

		if (sumXmin > sum.max())
		    removeConstraint();		
		break;
	    case ge:

		pruneGtEq();

		if (sumXmin >= sum.max())
		    removeConstraint();

		break;
	    }

	} while (store.propagationHasOccurred);
    }

    @Override
    public int getConsistencyPruningEvent(Var var) {

	// If consistency function mode
	if (consistencyPruningEvents != null) {
	    Integer possibleEvent = consistencyPruningEvents.get(var);
	    if (possibleEvent != null)
		return possibleEvent;
	}
	return IntDomain.BOUND;
    }

    @Override
    public int getNestedPruningEvent(Var var, boolean mode) {

	// If consistency function mode
	if (mode) {
	    if (consistencyPruningEvents != null) {
		Integer possibleEvent = consistencyPruningEvents.get(var);
		if (possibleEvent != null)
		    return possibleEvent;
	    }
	    return IntDomain.BOUND;
	}

	// If notConsistency function mode
	else {
	    if (notConsistencyPruningEvents != null) {
		Integer possibleEvent = notConsistencyPruningEvents.get(var);
		if (possibleEvent != null)
		    return possibleEvent;
	    }
	    return IntDomain.BOUND;
	}
    }

    @Override
    public int getNotConsistencyPruningEvent(Var var) {

	// If notConsistency function mode
	if (notConsistencyPruningEvents != null) {
	    Integer possibleEvent = notConsistencyPruningEvents.get(var);
	    if (possibleEvent != null)
		return possibleEvent;
	}
	return IntDomain.BOUND;	
    }

    @Override
    public void impose(Store store) {

	if (x == null)
	    return;

	reified = false;

	for (Var V : x)
	    V.putModelConstraint(this, getConsistencyPruningEvent(V));

	sum.putModelConstraint(this, getConsistencyPruningEvent(sum));

	store.addChanged(this);
	store.countConstraint();
    }

    private void computeInit() {
        int f = 0, e = 0;
        int min, max;

        for (int i = 0; i < l; i++) {
            min = x[i].min();
            max = x[i].max();
            f += min;
            e += max;
            I[i] = (max - min);
        }
	
        sumXmin = f;
        sumXmax = e;
    }

    private void pruneLtEq() {

	sum.domain.inMin(store.level, sum, sumXmin);

        int min, max;
	int sMax = sum.max();

        for (int i = 0; i < l; i++) {
            if (I[i] > (sMax - sumXmin)) {
                min = x[i].min();
                max = min + I[i];
                if (pruneMax(x[i], sMax - sumXmin + min)) {
                    int newMax = x[i].max();
                    sumXmax -= max - newMax;
                    I[i] = newMax - min;
                }
            }
        }
    }

    private void pruneGtEq() {

	sum.domain.inMax(store.level, sum, sumXmax);

        int min, max;
	int sMin = sum.min();
	
        for (int i = 0; i < l; i++) {
            if (I[i] > -(sMin - sumXmax)) {
                max = x[i].max();
                min = max - I[i];
                if (pruneMin(x[i], (sMin - sumXmax + max))) {
                    int newMin = x[i].min();
                    sumXmin += newMin - min;
                    I[i] = max - newMin;
                }
            }
        }
    }

    private void pruneLt() {

	sum.domain.inMin(store.level, sum, sumXmin + 1);
	
        int min, max;
	int sMax = sum.max();
	
        for (int i = 0; i < l; i++) {
            if (I[i] >= sMax - sumXmin) {
                min = x[i].min();
                max = min + I[i];
                if (pruneMax(x[i], sMax - sumXmin + min - 1)) {
                    int newMax = x[i].max();
                    sumXmax -= max - newMax;
                    I[i] = newMax - min;
                }
            }
        }
    }

    private void pruneGt() {

	sum.domain.inMax(store.level, sum, sumXmax - 1);

        int min, max;
	int sMin = sum.min();
	
        for (int i = 0; i < l; i++) {
            if (I[i] >= -(sMin - sumXmax)) {
                max = x[i].max();
                min = max - I[i];
                if (pruneMin(x[i], sMin - sumXmax + max + 1)) {
                    int nmin = x[i].min();
                    sumXmin += nmin - min;
                    I[i] = max - nmin;
                }
            }
        }
    }

    private void pruneNeq() {

        if (sumXmin == sumXmax) 
	    sum.domain.inComplement(store.level, sum, sumXmin);

        int min, max;

        for (int i = 0; i < l; i++) {
	    min = x[i].min();
	    max = min + I[i];

	    if (pruneNe(x[i], sum.min() - sumXmax + max, sum.max() - sumXmin + min)) {
		int newMin = x[i].min();
		int newMax = x[i].max();
		sumXmin += newMin - min;
		sumXmax += newMax - max;
		I[i] = newMax - newMin;
	    }
        }
    }

    private boolean pruneMin(IntVar x, int min) {
	if (min > x.min()) {
	    x.domain.inMin(store.level, x, min);
	    return true;
	}
	else
	    return false;
    }

    private boolean pruneMax(IntVar x, int max) {
	if (max < x.max()) {
	    x.domain.inMax(store.level, x, max);
	    return true;
	}
	else
	    return false;
    }

    private boolean pruneNe(IntVar x, int min, int max) {

	if (min == max) {
	    boolean boundsChanged = false;
	    if (min == x.min() || max == x.max())
		boundsChanged = true;

	    x.domain.inComplement(store.level, x, min);

	    return boundsChanged;
	}

	return false;
    }

    public boolean satisfiedEq() {

        int sMin = 0, sMax = 0;
	
        for (int i = 0; i < l; i++) {
            sMin += x[i].min();
            sMax += x[i].max();
	}

	return sMin == sMax && sMin == sum.min() && sMin == sum.max();
    }

    public boolean satisfiedNeq() {

        int sMax = 0, sMin = 0;
	
        for (int i = 0; i < l; i++) {
            sMin += x[i].min();
            sMax += x[i].max();
        }

        return sMin > sum.max() || sMax < sum.min();
    }

    public boolean satisfiedLtEq() {

	int sMax = 0;
	
        for (int i = 0; i < l; i++) {
            sMax += x[i].max();
        }

        return  sMax <= sum.min();
    }

    public boolean satisfiedLt() {

	int sMax = 0;
	
        for (int i = 0; i < l; i++) {
            sMax += x[i].max();
        }

        return  sMax < sum.min();
    }

    public boolean satisfiedGtEq() {

	int sMin = 0;
	
        for (int i = 0; i < l; i++) {
            sMin += x[i].min();
        }

        return  sMin >= sum.max();
    }

    public boolean satisfiedGt() {

	int sMin = 0;
	
        for (int i = 0; i < l; i++) {
            sMin += x[i].min();
        }

        return  sMin > sum.max();
    }

    @Override
    public void removeConstraint() {
	for (Var v : x)
	    v.removeConstraint(this);
	sum.removeConstraint(this);
    }

    @Override
    public boolean satisfied() {

	return entailed(relationType);

    }

    @Override
    public boolean notSatisfied() {

	return entailed(negRel[relationType]);

    }

    private boolean entailed(int rel) {

	switch (rel) {
	case eq:
	    return satisfiedEq();
	case le:
	    return satisfiedLtEq();
	case lt:
	    return satisfiedLt();
	case ne:
	    return satisfiedNeq();
	case gt:
	    return satisfiedGt();
	case ge:
	    return satisfiedGtEq();
	}

	return false;
    }

    public byte relation(String r) {
	if (r.equals("==")) 
	    return eq;
	else if (r.equals("=")) 
	    return eq;
	else if (r.equals("<"))
	    return lt;
	else if (r.equals("<="))
	    return le;
	else if (r.equals("=<"))
	    return le;
	else if (r.equals("!="))
	    return ne;
	else if (r.equals(">"))
	    return gt;
	else if (r.equals(">="))
	    return ge;
	else if (r.equals("=>"))
	    return ge;
	else {
	    System.err.println ("Wrong relation symbol in SumInt constraint " + r + "; assumed ==");
	    return eq;
	}
    }

    public String rel2String() {
	switch (relationType) {
	case eq : return "==";
	case lt : return "<";
	case le : return "<=";
	case ne : return "!=";
	case gt : return ">";
	case ge : return ">=";
	}

	return "?";
    }

    void checkForOverflow() {

	int sMin=0, sMax=0;
	for (int i=0; i<x.length; i++) {
	    int n1 = x[i].min();
	    int n2 = x[i].max();

	    sMin = add(sMin, n1);
	    sMax = add(sMax, n2);
	}
    }

    @Override
    public String toString() {

	StringBuffer result = new StringBuffer( id() );
	result.append(" : SumInt( [ ");

	for (int i = 0; i < l; i++) {
	    result.append(x[i]);
	    if (i < l - 1)
		result.append(", ");
	}
	result.append("], ");
	
	result.append(rel2String()).append(", ").append(sum).append( " )" );

	return result.toString();

    }

    @Override
    public void increaseWeight() {
	if (increaseWeight) {
	    for (Var v : x) v.weight++;
	}
	sum.weight++;
    }
}
