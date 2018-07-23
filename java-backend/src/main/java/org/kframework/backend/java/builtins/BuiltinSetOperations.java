// Copyright (c) 2013-2018 K Team. All Rights Reserved.
package org.kframework.backend.java.builtins;

import org.kframework.backend.java.kil.Bottom;
import org.kframework.backend.java.kil.BuiltinSet;
import org.kframework.backend.java.kil.DataStructures;
import org.kframework.backend.java.kil.Sort;
import org.kframework.backend.java.kil.Term;
import org.kframework.backend.java.kil.TermContext;

import com.google.common.collect.Sets;

import java.util.Set;
import java.util.stream.Collectors;


/**
 * Table of {@code public static} methods on builtin sets.
 *
 * @author AndreiS
 */
public class BuiltinSetOperations {

    public static Term constructor(Term[] terms, TermContext context) {
        Term set1 = terms[0];
        Term set2 = terms[1];
        if (set1.sort() != Sort.SET || set2.sort() != Sort.SET) {
            throw new IllegalArgumentException();
        }
        return BuiltinSet.concatenate(context.global(), set1, set2);
    }

    public static Term unit(Term[] terms, TermContext context) {
        return BuiltinSet.builder(context.global()).build();
    }

    public static Term element(Term[] terms, TermContext context) {
        Term element = terms[0];
        BuiltinSet.Builder builder = BuiltinSet.builder(context.global());
        builder.add(element);
        return builder.build();
    }

    public static Term intersection(Term[] terms, TermContext context) {
        BuiltinSet set1 = (BuiltinSet) terms[0];
        BuiltinSet set2 = (BuiltinSet) terms[1];
        if (!set1.isGround() || !set2.isGround()) {
            return null;
        }

        BuiltinSet.Builder builder = BuiltinSet.builder(context.global());
        builder.addAll(Sets.intersection(set1.elements(), set2.elements()));
        return builder.build();
    }

    public static Term difference(Term[] terms, TermContext context) {
        Term set = terms[0];
        BuiltinSet removeBuiltinSet = (BuiltinSet) terms[1];
        if (!removeBuiltinSet.isConcreteCollection()) {
            return null;
        }

        if (removeBuiltinSet.isEmpty()) {
            return set;
        }

        if (!(set instanceof BuiltinSet)) {
            return null;
        }
        BuiltinSet builtinSet = (BuiltinSet) set;

        BuiltinSet.Builder builder = BuiltinSet.builder(context.global());
        builder.concatenate(builtinSet);

        Set<Term> pendingRemoveSet = removeBuiltinSet.elements().stream()
                .filter(element -> !builder.remove(element))
                .collect(Collectors.toSet());

        if (!builtinSet.isConcreteCollection() && !pendingRemoveSet.isEmpty()) {
            return DataStructures.setDifference(builder.build(), pendingRemoveSet, context);
        } else {
            return builder.build();
        }
    }

    public static Term in(Term[] terms, TermContext context) {
        Term element = terms[0];
        BuiltinSet set = (BuiltinSet) terms[1];
        if (set.contains(element)) {
            return BoolToken.TRUE;
        } else if (element.isGround() && set.isGround()) {
            return BoolToken.FALSE;
        } else if (set.isEmpty()) {
            return BoolToken.FALSE;
        } else {
            return null;
        }
    }

    public static Term inclusion(Term[] terms, TermContext context) {
        BuiltinSet set1 = (BuiltinSet) terms[0];
        BuiltinSet set2 = (BuiltinSet) terms[1];
        if (!set1.isGround() || !set2.isGround()) {
            return null;
        }

        return BoolToken.of(set2.elements().containsAll(set1.elements()));
    }

    public static Term choice(Term[] terms, TermContext context) {
        BuiltinSet set = (BuiltinSet) terms[0];
        if (!set.elements().isEmpty()) {
            return set.elements().iterator().next();
        } else if (set.isEmpty()) {
            return Bottom.BOTTOM;
        } else {
            return null;
        }
    }

}
