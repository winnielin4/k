// Copyright (c) 2013-2018 K Team. All Rights Reserved.
package org.kframework.backend.java.builtins;

import org.kframework.backend.java.kil.BuiltinMap;
import org.kframework.backend.java.kil.BuiltinSet;
import org.kframework.backend.java.kil.JavaSymbolicObject;
import org.kframework.backend.java.kil.KItem;
import org.kframework.backend.java.kil.KLabelInjection;
import org.kframework.backend.java.kil.KList;
import org.kframework.backend.java.kil.MetaVariable;
import org.kframework.backend.java.kil.Term;
import org.kframework.backend.java.kil.TermContext;
import org.kframework.backend.java.kil.Variable;
import org.kframework.backend.java.symbolic.ConjunctiveFormula;
import org.kframework.backend.java.symbolic.CopyOnWriteTransformer;
import org.kframework.backend.java.symbolic.PatternMatcher;

import java.util.HashSet;
import java.util.Set;


/**
 * Table of {@code public static} methods for builtin meta K operations.
 *
 * @author AndreiS
 */
public class MetaK {

    /**
     * Checks if two given {@link Term}s can be unified.
     *
     * @param term1
     *            the first term
     * @param term2
     *            the second term
     * @param context
     *            the term context
     * @return {@link BoolToken#TRUE} if the two terms can be unified;
     *         {@link BoolToken#FALSE} if they cannot be unified; otherwise,
     *         {@code null}
     */
    public static Term unifiable(Term[] terms, TermContext context) {
        Term term1 = terms[0];
        Term term2 = terms[1];
        ConjunctiveFormula constraint = ConjunctiveFormula.of(context.global());
        if (term1 instanceof KList && term2 instanceof KList) {
            if (((KList) term1).size() != ((KList) term2).size()) {
                return null;
            }
            for (int i = 0; i < ((KList) term1).size(); i++) {
                constraint = constraint.add(((KList) term1).get(i), ((KList) term2).get(i));
            }
        } else {
            constraint = constraint.add(term1, term2);
        }
        constraint = constraint.simplify(context);
        if (constraint.isFalse()) {
            return BoolToken.FALSE;
        }

        BoolToken result = BoolToken.FALSE;
        for (ConjunctiveFormula solution : constraint.getDisjunctiveNormalForm().conjunctions()) {
            solution = solution.simplify(context);
            if (solution.isSubstitution()) {
                return BoolToken.TRUE;
            } else if (!solution.isFalse()) {
                /* when none of the solutions is true and at least one of the
                 * them is not certainly false, the result is unknown */
                result = null;
            }
        }
        return result;
    }

    /**
     * Checks if the subject term matches the pattern.
     *
     * @param subject
     *            the subject term
     * @param pattern
     *            the pattern term
     * @param context
     *            the term context
     * @return {@link BoolToken#TRUE} if the two terms can be matched;
     *         otherwise, {@link BoolToken#FALSE}
     */
    public static BoolToken matchable(Term subject, Term pattern, TermContext context) {
        return PatternMatcher.matchable(subject, pattern, context) ? BoolToken.TRUE
                : BoolToken.FALSE;
    }

    /**
     * Renames {@link MetaVariable}s of a given {@link Term} to fresh {@link Variable}s if they appear also in
     * a given {@link BuiltinSet} of {@link MetaVariable}s.
     *
     *
     * @param term
     *            the given term
     * @param builtinSet
     *            the given set of meta variables
     * @param context
     *            the term context
     * @return the resulting term if the renaming succeeds; or the original term
     *         if the given {@code BuiltinSet} has a frame or contains not only
     *         {@code MetaVariable}s
     */
    public static Term rename(Term[] terms, TermContext context) {
        Term term = terms[0];
        BuiltinSet builtinSet = (BuiltinSet) terms[1];
        if (builtinSet.hasFrame() /* || !builtinSet.operations().isEmpty() */) {
            return term;
        }

        Set<Variable> variables = new HashSet<>();
        for (Term element : builtinSet.elements()) {
            if (!(element instanceof MetaVariable)) {
                return term;
            }

            variables.add(new Variable((MetaVariable) element));
        }

        term = (Term) term.accept(new CopyOnWriteTransformer() {
            @Override
            public JavaSymbolicObject<Term> transform(MetaVariable metaVariable) {
                return new Variable(metaVariable);
            }
        });

        return term.substitute(Variable.rename(variables));
    }

    /**
     * Renames all {@link Variable}s inside a given {@link Term} to unique fresh names.
     *
     * @param term
     *            the given term
     * @param context
     *            the term context
     * @return the resulting term after renaming
     */
    public static Term renameVariables(Term[] terms, TermContext context) {
        Term term = terms[0];
        Set<Variable> variables = term.variableSet();
        return term.substitute(Variable.rename(variables));
    }

    public static Term freezeVariables(Term[] terms, TermContext context) {
        Term termToFreeze = terms[0];
        Term termWithBoundVars = terms[1];
        BuiltinSet variables = (BuiltinSet) trueVariables(new Term[] {termWithBoundVars}, context);
        return (Term) termToFreeze.accept(new CopyOnWriteTransformer(context) {
            @Override
            public JavaSymbolicObject<Term> transform(Variable variable) {
                if (!variables.contains(variable)) {
                    return new MetaVariable(variable);
                }
                return variable;
            }
        });
    }

    /**
     * Returns all {@link Variable}s inside a given {@link Term} as a
     * {@link BuiltinSet} of {@link MetaVariable}s.
     */
    public static Term variables(Term[] terms, TermContext context) {
        Term term = terms[0];
        BuiltinSet.Builder builder = BuiltinSet.builder(context.global());
        for (Variable variable : term.variableSet()) {
            builder.add(new MetaVariable(variable));
        }
        return (BuiltinSet) builder.build();
    }

    /**
     * Returns all {@link Variable}s inside a given {@link Term} as a
     * {@link BuiltinSet}.
     */
    public static Term trueVariables(Term[] terms, TermContext context) {
        Term term = terms[0];
        BuiltinSet.Builder builder = BuiltinSet.builder(context.global());
        builder.addAll(term.variableSet());
        return (BuiltinSet) builder.build();
    }

    public static Term variablesMap(Term[] terms, TermContext context) {
        Term term = terms[0];
        BuiltinMap.Builder builder = BuiltinMap.builder(context.global());
        for (Variable variable : term.variableSet()) {
            builder.put(new MetaVariable(variable), variable);
        }
        return builder.build();
    }

    /**
     * Returns the K label of a specified {@link KItem}.
     *
     * @param kItem
     *            the specified {@code KItem}
     * @param context
     *            the term context
     * @return the K label
     */
    public static Term getKLabel(Term[] terms, TermContext context) {
        KItem kItem = (KItem) terms[0];
        // TODO(AndreiS): handle KLabel variables
        return KItem.of(new KLabelInjection(kItem.kLabel()), KList.EMPTY, context.global(),
            kItem.att());
    }

    public static Term configuration(Term[] terms, TermContext context) {
        //return KLabelInjection.injectionOf(context.getTopTerm(), context.global());
        return context.getTopTerm();
    }

    public static Term isConcrete(Term[] terms, TermContext context) {
        Term term = terms[0];
        return BoolToken.of(term.isConcrete());
    }
}
