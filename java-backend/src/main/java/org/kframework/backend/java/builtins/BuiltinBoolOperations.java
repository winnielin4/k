// Copyright (c) 2013-2018 K Team. All Rights Reserved.
package org.kframework.backend.java.builtins;


import org.kframework.backend.java.kil.Term;
import org.kframework.backend.java.kil.TermContext;

/**
 * Table of {@code public static} methods on builtin boolean values.
 *
 * @author: AndreiS
 */
public class BuiltinBoolOperations {

    public static Term not(Term[] terms, TermContext context) {
        BoolToken term = (BoolToken) terms[0];
        return BoolToken.of(!term.booleanValue());
    }

    public static Term and(Term[] terms, TermContext context) {
        BoolToken term1 = (BoolToken) terms[0];
        BoolToken term2 = (BoolToken) terms[1];
        return BoolToken.of(term1.booleanValue() && term2.booleanValue());
    }

    public static Term andThen(Term[] terms, TermContext context) {
        Term term1 = terms[0];
        Term term2 = terms[1];
        if (term1 instanceof BoolToken) {
            BoolToken boolToken1 = (BoolToken) term1;
            return boolToken1.booleanValue() ? term2 : BoolToken.FALSE;
        } else if (term2 instanceof BoolToken) {
            BoolToken boolToken2 = (BoolToken) term2;
            return boolToken2.booleanValue() ? term1 : BoolToken.FALSE;
        } else {
            return null;
        }
    }

    public static Term or(Term[] terms, TermContext context) {
        BoolToken term1 = (BoolToken) terms[0];
        BoolToken term2 = (BoolToken) terms[1];
        return BoolToken.of(term1.booleanValue() || term2.booleanValue());
    }

    public static Term orElse(Term[] terms, TermContext context) {
        Term term1 = terms[0];
        Term term2 = terms[1];
        if (term1 instanceof BoolToken) {
            BoolToken boolToken1 = (BoolToken) term1;
            return boolToken1.booleanValue() ? BoolToken.TRUE : term2;
        } else if (term2 instanceof BoolToken) {
            BoolToken boolToken2 = (BoolToken) term2;
            return boolToken2.booleanValue() ? BoolToken.TRUE : term1;
        } else {
            return null;
        }
    }

    public static Term xor(Term[] terms, TermContext context) {
        BoolToken term1 = (BoolToken) terms[0];
        BoolToken term2 = (BoolToken) terms[1];
        return BoolToken.of(term1.booleanValue() ^ term2.booleanValue());
    }

    public static Term implies(Term[] terms, TermContext context) {
        BoolToken term1 = (BoolToken) terms[0];
        BoolToken term2 = (BoolToken) terms[1];
        return BoolToken.of(!term1.booleanValue() || term2.booleanValue());
    }

    public static Term eq(Term[] terms, TermContext context) {
        BoolToken term1 = (BoolToken) terms[0];
        BoolToken term2 = (BoolToken) terms[1];
        return BoolToken.of(term1.booleanValue() == term2.booleanValue());
    }

    public static Term ne(Term[] terms, TermContext context) {
        BoolToken term1 = (BoolToken) terms[0];
        BoolToken term2 = (BoolToken) terms[1];
        return BoolToken.of(term1.booleanValue() != term2.booleanValue());
    }

}
