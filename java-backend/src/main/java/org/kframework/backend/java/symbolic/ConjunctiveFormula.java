// Copyright (c) 2015-2019 K Team. All Rights Reserved.
package org.kframework.backend.java.symbolic;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.kframework.backend.java.builtins.BoolToken;
import org.kframework.backend.java.builtins.IntToken;
import org.kframework.backend.java.kil.Bottom;
import org.kframework.backend.java.kil.BuiltinList;
import org.kframework.backend.java.kil.BuiltinMap;
import org.kframework.backend.java.kil.CollectionInternalRepresentation;
import org.kframework.backend.java.kil.ConstrainedTerm;
import org.kframework.backend.java.kil.DataStructures;
import org.kframework.backend.java.kil.GlobalContext;
import org.kframework.backend.java.kil.JavaSymbolicObject;
import org.kframework.backend.java.kil.KItem;
import org.kframework.backend.java.kil.KLabel;
import org.kframework.backend.java.kil.KLabelConstant;
import org.kframework.backend.java.kil.KList;
import org.kframework.backend.java.kil.Kind;
import org.kframework.backend.java.kil.LocalRewriteTerm;
import org.kframework.backend.java.kil.Sort;
import org.kframework.backend.java.kil.SortSignature;
import org.kframework.backend.java.kil.Term;
import org.kframework.backend.java.kil.TermContext;
import org.kframework.backend.java.kil.Variable;
import org.kframework.backend.java.util.Constants;
import org.kframework.backend.java.util.CounterStopwatch;
import org.kframework.backend.java.util.FormulaContext;
import org.kframework.backend.java.util.RewriteEngineUtils;
import org.kframework.backend.java.util.StateLog;
import org.kframework.builtin.KLabels;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A conjunction of equalities (between terms with variables) and disjunctions
 *
 * @see org.kframework.backend.java.symbolic.Equality
 * @see org.kframework.backend.java.symbolic.DisjunctiveFormula
 */
public class ConjunctiveFormula extends Term implements CollectionInternalRepresentation {

    public static final String SEPARATOR = " /\\ ";

    public static ConjunctiveFormula of(GlobalContext global) {
        return new ConjunctiveFormula(
                ImmutableMapSubstitution.empty(),
                PersistentUniqueList.empty(),
                PersistentUniqueList.empty(),
                TruthValue.TRUE,
                global);
    }

    public static ConjunctiveFormula of(ConjunctiveFormula formula) {
        return new ConjunctiveFormula(
                formula.substitution,
                formula.equalities,
                formula.disjunctions,
                formula.truthValue,
                formula.falsifyingEquality,
                formula.global);
    }

    public static ConjunctiveFormula of(Substitution<Variable, Term> substitution, GlobalContext global) {
        return new ConjunctiveFormula(
                substitution,
                PersistentUniqueList.empty(),
                PersistentUniqueList.empty(),
                substitution.isEmpty() ? TruthValue.TRUE : TruthValue.UNKNOWN,
                global);
    }

    public static ConjunctiveFormula of(
            Substitution<Variable, Term> substitution,
            PersistentUniqueList<Equality> equalities,
            PersistentUniqueList<DisjunctiveFormula> disjunctions,
            GlobalContext global) {
        return new ConjunctiveFormula(
                substitution,
                equalities,
                disjunctions,
                substitution.isEmpty() && equalities.isEmpty() && disjunctions.isEmpty() ? TruthValue.TRUE : TruthValue.UNKNOWN,
                global);
    }

    private final Substitution<Variable, Term> substitution;
    private final PersistentUniqueList<Equality> equalities;
    private final PersistentUniqueList<DisjunctiveFormula> disjunctions;

    private final TruthValue truthValue;

    private final Equality falsifyingEquality;

    private transient final GlobalContext global;

    public ConjunctiveFormula(
            Substitution<Variable, Term> substitution,
            PersistentUniqueList<Equality> equalities,
            PersistentUniqueList<DisjunctiveFormula> disjunctions,
            TruthValue truthValue,
            Equality falsifyingEquality,
            GlobalContext global) {
        super(Kind.KITEM);

        // assert that there is no equality between an empty list and a list containing an element (the equality should be false instead)
        assert !equalities.stream().anyMatch(e -> e.leftHandSide() instanceof BuiltinList && ((BuiltinList) e.leftHandSide()).isEmpty() && e.rightHandSide() instanceof BuiltinList && !((BuiltinList) e.rightHandSide()).isEmpty() && ((BuiltinList) e.rightHandSide()).isElement(0));

        this.substitution = substitution;
        this.equalities = equalities;
        this.disjunctions = disjunctions;
        this.truthValue = truthValue;
        this.falsifyingEquality = falsifyingEquality;
        this.global = global;
    }

    public ConjunctiveFormula(
            Substitution<Variable, Term> substitution,
            PersistentUniqueList<Equality> equalities,
            PersistentUniqueList<DisjunctiveFormula> disjunctions,
            TruthValue truthValue,
            GlobalContext global) {
        this(substitution, equalities, disjunctions, truthValue, null, global);
    }

    public GlobalContext globalContext() {
        return global;
    }

    public Substitution<Variable, Term> substitution() {
        return substitution;
    }

    public PersistentUniqueList<Equality> equalities() {
        return equalities;
    }

    public PersistentUniqueList<DisjunctiveFormula> disjunctions() {
        return disjunctions;
    }

    /**
     * Adds the side condition of a rule to this constraint. The side condition is represented
     * as a list of {@code Term}s that are expected to be equal to {@code BoolToken#TRUE}.
     */
    public ConjunctiveFormula addAll(List<Term> condition) {
        ConjunctiveFormula result = this;
        for (Term term : condition) {
            result = result.add(term, BoolToken.TRUE);
            if (result == null) {
                return null;
            }
        }
        return result;
    }

    public ConjunctiveFormula addAndSimplify(Object term, TermContext context) {
        return add(term).simplify(context);
    }

    public ConjunctiveFormula add(ConjunctiveFormula conjunction) {
        return add(conjunction.substitution)
                .addAll(conjunction.equalities)
                .addAll(conjunction.disjunctions);
    }

    public ConjunctiveFormula add(Substitution<Variable, Term> substitution) {
        return addAll(substitution.equalities(global));
    }

    public ConjunctiveFormula add(Equality equality) {
        /* simplify andBool */
        if (equality.leftHandSide() instanceof KItem
                && ((KItem) equality.leftHandSide()).kLabel().toString().endsWith("_andBool_")
                && equality.rightHandSide().equals(BoolToken.TRUE)) {
            return this
                    .add(((KList) ((KItem) equality.leftHandSide()).kList()).get(0), BoolToken.TRUE)
                    .add(((KList) ((KItem) equality.leftHandSide()).kList()).get(1), BoolToken.TRUE);
        }
        if (equality.rightHandSide() instanceof KItem
                && ((KItem) equality.rightHandSide()).kLabel().toString().endsWith("_andBool_")
                && equality.leftHandSide().equals(BoolToken.TRUE)) {
            return this
                    .add(((KList) ((KItem) equality.rightHandSide()).kList()).get(0), BoolToken.TRUE)
                    .add(((KList) ((KItem) equality.rightHandSide()).kList()).get(1), BoolToken.TRUE);
        }

        /* simplify orBool */
        if (equality.leftHandSide() instanceof KItem
                && ((KItem) equality.leftHandSide()).kLabel().toString().endsWith("_orBool_")
                && equality.rightHandSide().equals(BoolToken.FALSE)) {
            return this
                    .add(((KList) ((KItem) equality.leftHandSide()).kList()).get(0), BoolToken.FALSE)
                    .add(((KList) ((KItem) equality.leftHandSide()).kList()).get(1), BoolToken.FALSE);
        }
        if (equality.rightHandSide() instanceof KItem
                && ((KItem) equality.rightHandSide()).kLabel().toString().endsWith("_orBool_")
                && equality.leftHandSide().equals(BoolToken.FALSE)) {
            return this
                    .add(((KList) ((KItem) equality.rightHandSide()).kList()).get(0), BoolToken.FALSE)
                    .add(((KList) ((KItem) equality.rightHandSide()).kList()).get(1), BoolToken.FALSE);
        }

        /* simplify notBool */
        if (equality.leftHandSide() instanceof KItem
                && ((KItem) equality.leftHandSide()).kLabel().toString().endsWith("notBool_")
                && equality.rightHandSide() instanceof BoolToken) {
            return this.add(
                    ((KList) ((KItem) equality.leftHandSide()).kList()).get(0),
                    BoolToken.of(!((BoolToken) equality.rightHandSide()).booleanValue()));
        }
        if (equality.rightHandSide() instanceof KItem
                && ((KItem) equality.rightHandSide()).kLabel().toString().endsWith("notBool_")
                && equality.leftHandSide() instanceof BoolToken) {
            return this.add(
                    ((KList) ((KItem) equality.rightHandSide()).kList()).get(0),
                    BoolToken.of(!((BoolToken) equality.leftHandSide()).booleanValue()));
        }

        return new ConjunctiveFormula(
                substitution,
                equalities.plus(equality),
                disjunctions,
                truthValue != TruthValue.FALSE ? TruthValue.UNKNOWN : TruthValue.FALSE,
                falsifyingEquality,
                global);
    }

    /**
     * @return A new conjunctive formula that contains the same content minus the given equality, if it is present.
     * Returns the same formula if given equality is not present.
     */
    public static ConjunctiveFormula minus(@Nullable ConjunctiveFormula formula, @Nullable Equality equality) {
        if (formula == null || !formula.equalities().contains(equality)) {
            return formula;
        }
        return new ConjunctiveFormula(formula.substitution(),
                formula.equalities().minus(equality),
                formula.disjunctions(), formula.truthValue(), formula.globalContext());
    }

    public ConjunctiveFormula unsafeAddVariableBinding(Variable variable, Term term) {
        // these assertions are commented out for performance reasons
        //assert term.substituteAndEvaluate(substitution, context) == term;
        //assert !term.variableSet().contains(variable);
        Term previousTerm = substitution.get(variable);
        if (previousTerm == null) {
            return new ConjunctiveFormula(
                    substitution.plus(variable, term),
                    equalities,
                    disjunctions,
                    truthValue,
                    global);
        } else if (previousTerm.equals(term)) {
            return this;
        } else {
            return falsify(substitution, equalities, disjunctions, new Equality(previousTerm, term, global));
        }
    }

    public ConjunctiveFormula add(Term leftHandSide, Term rightHandSide) {
        if (leftHandSide instanceof KList && rightHandSide instanceof KList) {
            assert ((KList) leftHandSide).size() == ((KList) rightHandSide).size();
            ConjunctiveFormula formula = this;
            for (int i = 0; i < ((KList) leftHandSide).size(); i++) {
                formula = formula.add(((KList) leftHandSide).get(i), ((KList) rightHandSide).get(i));
            }
            return formula;
        }

        assert !(leftHandSide instanceof KList) && !(rightHandSide instanceof KList);
        return add(new Equality(leftHandSide, rightHandSide, global));
    }

    public ConjunctiveFormula add(DisjunctiveFormula disjunction) {
        return new ConjunctiveFormula(
                substitution,
                equalities,
                disjunctions.plus(disjunction),
                truthValue != TruthValue.FALSE ? TruthValue.UNKNOWN : TruthValue.FALSE,
                falsifyingEquality,
                global);
    }

    @SuppressWarnings("unchecked")
    public ConjunctiveFormula add(Object term) {
        if (term instanceof ConjunctiveFormula) {
            return add((ConjunctiveFormula) term);
        } else if (term instanceof Substitution) {
            return add((Substitution) term);
        } else if (term instanceof Equality) {
            return add((Equality) term);
        } else if (term instanceof DisjunctiveFormula) {
            return add((DisjunctiveFormula) term);
        } else {
            assert false : "invalid argument found: " + term;
            return null;
        }
    }

    public ConjunctiveFormula addAll(Iterable<?> terms) {
        ConjunctiveFormula result = this;
        for (Object term : terms) {
            result = result.add(term);
            if (result == null) {
                return null;
            }
        }
        return result;
    }

    public TruthValue truthValue() {
        return truthValue;
    }

    public boolean isTrue() {
        return truthValue == TruthValue.TRUE;
    }

    public boolean isFalseExtended() {
        return isFalse() || isFalseFromContradictions();
    }

    public boolean isFalse() {
        return truthValue == TruthValue.FALSE;
    }

    /**
     * @return true if formula contains both some element "T" and "T ==K false".
     */
    public boolean isFalseFromContradictions() {
        Set<Term> elements = new HashSet<>(getKComponents());
        return equalities.parallelStream()
                .anyMatch(eq -> eq.rightHandSide().equals(BoolToken.FALSE) && elements.contains(eq.leftHandSide()));
    }

    public boolean isUnknown() {
        return truthValue == TruthValue.UNKNOWN;
    }

    /**
     * Removes specified variable bindings from this constraint.
     * <p>
     * Note: this method should only be used to garbage collect useless
     * bindings. It is called to remove all bindings of the rewrite rule
     * variables after building the rewrite result.
     */
    public ConjunctiveFormula removeBindings(Set<Variable> variablesToRemove) {
        return ConjunctiveFormula.of(
                substitution.minusAll(variablesToRemove),
                equalities,
                disjunctions,
                global);
    }


    public ConjunctiveFormula simplify() {
        return simplify(false, true, TermContext.builder(global).build(), false);
    }

    /**
     * Simplifies this conjunctive formula as much as possible.
     * Decomposes equalities by using unification.
     */
    public ConjunctiveFormula simplify(TermContext context) {
        return simplify(false, true, context, false);
    }

    /**
     * Similar to {@link ConjunctiveFormula#simplify(TermContext)} except that equalities
     * between builtin data structures will remain intact if they cannot be
     * resolved completely.
     */
    public ConjunctiveFormula simplifyBeforePatternFolding(TermContext context, boolean logFailures) {
        return simplify(false, false, context, logFailures);
    }

    public ConjunctiveFormula simplifyModuloPatternFolding(TermContext context) {
        return simplify(true, true, context, false);
    }

    private ConjunctiveFormula simplify(boolean patternFolding, boolean partialSimplification,
                                        TermContext context, boolean logFailures) {
        ConjunctiveFormula cachedResult = global.formulaCache
                .cacheGet(this, patternFolding, partialSimplification, context);
        if (cachedResult != null) {
            return cachedResult;
        }

        ConjunctiveFormula result = simplifyImpl(patternFolding, partialSimplification, context, logFailures);
        global.formulaCache.cachePut(this, patternFolding, partialSimplification, context, result);
        return result;
    }

    /**
     * Simplifies this conjunctive formula as much as possible.
     * Decomposes equalities by using unification.
     */
    private ConjunctiveFormula simplifyImpl(boolean patternFolding, boolean partialSimplification, TermContext context,
                                            boolean logFailures) {
        assert !isFalse();
        ConjunctiveFormula originalTopConstraint = context.getTopConstraint();
        Substitution<Variable, Term> substitution = this.substitution;
        PersistentUniqueList<Equality> equalities = this.equalities;
        PersistentUniqueList<DisjunctiveFormula> disjunctions = this.disjunctions;

        try {
            boolean change;
            do {
                change = false;
                PersistentUniqueList<Equality> pendingEqualities = PersistentUniqueList.empty();
                for (int i = 0; i < equalities.size(); ++i) {
                    Equality equality = equalities.get(i);

                    //Any equality should be evaluated in the context of other entries but not itself, otherwise information
                    //loss can happen. Details: https://github.com/kframework/k-legacy/pull/2399#issuecomment-360680618
                    context.setTopConstraint(minus(originalTopConstraint, equality));

                    Term leftHandSide = equality.leftHandSide().substituteAndEvaluate(substitution, context);
                    Term rightHandSide = equality.rightHandSide().substituteAndEvaluate(substitution, context);
                    equality = new Equality(leftHandSide, rightHandSide, global);
                    if (equality.isTrue()) {
                        // delete
                    } else if (equality.truthValue() == TruthValue.FALSE) {
                        // conflict
                        return falsify(substitution, equalities, disjunctions, equality);
                    } else {
                        if (equality.isSimplifiableByCurrentAlgorithm()) {
                            // (decompose + conflict)*
                            FastRuleMatcher unifier = new FastRuleMatcher(global, 1);
                            ConjunctiveFormula unificationConstraint = unifier.unifyEquality(leftHandSide, rightHandSide, patternFolding, partialSimplification, false, context, logFailures);
                            if (unificationConstraint.isFalse()) {
                                return falsify(
                                        substitution,
                                        equalities,
                                        disjunctions,
                                        new Equality(
                                                unifier.unificationFailureLeftHandSide(),
                                                unifier.unificationFailureRightHandSide(),
                                                global));
                            }

                            // TODO(AndreiS): fix this in a general way
                            if (unificationConstraint.equalities.contains(equality)) {
                                pendingEqualities = pendingEqualities.plus(equality);
                                continue;
                            }

                            equalities = equalities.plusAll(i + 1, unificationConstraint.equalities);
                            equalities = equalities.plusAll(i + 1, unificationConstraint.substitution.equalities(global));
                            disjunctions = disjunctions.plusAll(unificationConstraint.disjunctions);
                        } else if (leftHandSide instanceof Variable && rightHandSide instanceof Variable
                                && leftHandSide.sort().equals(rightHandSide.sort())) {
                            // eliminate: special case when both the left-hand-side and the right-hand-side can be eliminated
                            ImmutableMapSubstitution<Variable, Term> eliminationSubstitution;
                            if (leftHandSide.toString().compareTo(rightHandSide.toString()) < 0) {
                                eliminationSubstitution = ImmutableMapSubstitution.singleton((Variable) leftHandSide, rightHandSide);
                            } else {
                                eliminationSubstitution = ImmutableMapSubstitution.singleton((Variable) rightHandSide, leftHandSide);
                            }

                            substitution = ImmutableMapSubstitution.composeAndEvaluate(
                                    substitution,
                                    eliminationSubstitution,
                                    context);
                            change = true;
                            if (substitution.isFalse(global)) {
                                return falsify(substitution, equalities, disjunctions, equality);
                            }
                        } else if (leftHandSide instanceof Variable
                                && !rightHandSide.variableSet().contains(leftHandSide)) {
                            // eliminate
                            ImmutableMapSubstitution<Variable, Term> eliminationSubstitution = getSubstitution(
                                    (Variable) leftHandSide,
                                    rightHandSide);
                            if (eliminationSubstitution == null) {
                                pendingEqualities = pendingEqualities.plus(equality);
                                continue;
                            }

                            substitution = ImmutableMapSubstitution.composeAndEvaluate(
                                    substitution,
                                    eliminationSubstitution,
                                    context);
                            change = true;
                            if (substitution.isFalse(global)) {
                                return falsify(substitution, equalities, disjunctions, equality);
                            }
                        } else if (rightHandSide instanceof Variable
                                && !leftHandSide.variableSet().contains(rightHandSide)) {
                            // swap + eliminate
                            ImmutableMapSubstitution<Variable, Term> eliminationSubstitution = getSubstitution(
                                    (Variable) rightHandSide,
                                    leftHandSide);
                            if (eliminationSubstitution == null) {
                                pendingEqualities = pendingEqualities.plus(equality);
                                continue;
                            }

                            substitution = ImmutableMapSubstitution.composeAndEvaluate(
                                    substitution,
                                    eliminationSubstitution,
                                    context);
                            change = true;
                            if (substitution.isFalse(global)) {
                                return falsify(substitution, equalities, disjunctions, equality);
                            }
                        } else if (leftHandSide instanceof Variable
                                && rightHandSide.isNormal()
                                && rightHandSide.variableSet().contains(leftHandSide)) {
                            // occurs
                            return falsify(substitution, equalities, disjunctions, equality);
                        } else if (rightHandSide instanceof Variable
                                && leftHandSide.isNormal()
                                && leftHandSide.variableSet().contains(rightHandSide)) {
                            // swap + occurs
                            return falsify(substitution, equalities, disjunctions, equality);
                        } else {
                            // unsimplified equation
                            pendingEqualities = pendingEqualities.plus(equality);
                        }
                    }
                }
                equalities = pendingEqualities;
            } while (change);

            return ConjunctiveFormula.of(substitution, equalities, disjunctions, global);
        } finally {
            context.setTopConstraint(originalTopConstraint);
        }
    }

    private ConjunctiveFormula falsify(
            Substitution<Variable, Term> substitution,
            PersistentUniqueList<Equality> equalities,
            PersistentUniqueList<DisjunctiveFormula> disjunctions,
            Equality equality) {
        if ((RuleAuditing.isAuditBegun() || global.javaExecutionOptions.debugZ3)
                && !(equality.leftHandSide() instanceof BoolToken && equality.rightHandSide() instanceof BoolToken)) {
            System.err.format("Unification failure: %s does not unify with %s\n",
                    equality.leftHandSide(), equality.rightHandSide());
        }
        return new ConjunctiveFormula(
                substitution,
                equalities,
                disjunctions,
                TruthValue.FALSE,
                equality,
                global);
    }

    /**
     * Apply projection lemmas over equalities:
     *     proj(X,0,N) == proj(Y,0,N) /\ ... /\ proj(X,N-1,N) == proj(Y,N-1,N)
     *   iff
     *     X == Y
     */
    public ConjunctiveFormula applyProjectionLemma() {
        // Map: (X,Y) ==> N ==> {0,1,...,N-1}
        Map<Equality, Map<Integer, Set<Integer>>> projEqualitiesMap = new HashMap<>();
        for (Equality equality : this.equalities) {
            if (equality.leftHandSide() instanceof KItem && equality.rightHandSide() instanceof KItem) {
                KLabelConstant lKLabel = (KLabelConstant) ((KItem) equality.leftHandSide()).kLabel(); // proj
                KLabelConstant rKLabel = (KLabelConstant) ((KItem) equality.rightHandSide()).klabel(); // proj

                KList lKList = (KList) ((KItem) equality.leftHandSide()).kList(); // X, I, N
                KList rKList = (KList) ((KItem) equality.rightHandSide()).kList(); // Y, I, N

                if (lKLabel.isProjection() && lKLabel.equals(rKLabel)) {
                    List<Integer> projAtt = lKLabel.getProjectionAtt();
                    int idxVector = projAtt.get(0); // X
                    int idxElem = projAtt.get(1); // I
                    int idxSize = projAtt.get(2); // N
                    int sizeKList = 3;
                    if (lKList.size() == sizeKList && rKList.size() == sizeKList) {
                        if (lKList.get(idxElem) instanceof IntToken && rKList.get(idxElem) instanceof IntToken
                                && lKList.get(idxSize) instanceof IntToken && rKList.get(idxSize) instanceof IntToken) {
                            int lElem = ((IntToken) lKList.get(idxElem)).intValue(); // I
                            int rElem = ((IntToken) rKList.get(idxElem)).intValue(); // I
                            int lSize = ((IntToken) lKList.get(idxSize)).intValue(); // N
                            int rSize = ((IntToken) rKList.get(idxSize)).intValue(); // N
                            if (lElem == rElem && lSize == rSize) {
                                Equality newEquality = new Equality(lKList.get(idxVector), rKList.get(idxVector), global); // X == Y
                                Map<Integer, Set<Integer>> projEqualities = projEqualitiesMap.get(newEquality); // N ==> {...,I,...}
                                if (projEqualities == null) {
                                    projEqualities = new HashMap<>();
                                    Set<Integer> elemSet = new HashSet<>(); // {0,1,...,N-1}
                                    for (int i = 0; i < lSize; i++) { elemSet.add(i); }
                                    projEqualities.put(lSize, elemSet);
                                    projEqualitiesMap.put(newEquality, projEqualities);
                                }
                                projEqualities.get(lSize).remove(lElem); // N ==> {...}
                            }
                        }
                    }
                }
            }
        }

        PersistentUniqueList<Equality> equalities = this.equalities;
        for (Equality equality : projEqualitiesMap.keySet()) {
            for (int size :projEqualitiesMap.get(equality).keySet()) {
                if (projEqualitiesMap.get(equality).get(size).isEmpty()) {
                    equalities = equalities.plus(equality);
                }
            }
        }

        return new ConjunctiveFormula(substitution, equalities, disjunctions, truthValue, falsifyingEquality, global);
    }

    /**
     * For any abstraction function symbol #f:
     *
     *   #f(X) == #f(Y)  =>  X == Y
     *
     * even if #f is NOT injective.
     */
    public ConjunctiveFormula resolveMatchingSymbols(Set<String> matchingSymbols) {
        PersistentUniqueList<Equality> resultEqualities = PersistentUniqueList.empty();
        PersistentUniqueList<Equality> newEqualities = this.equalities;

        while (!newEqualities.isEmpty()) {
            PersistentUniqueList<Equality> curEqualities = newEqualities;
            newEqualities = PersistentUniqueList.empty();

            for (Equality equality : curEqualities) {
                if (equality.leftHandSide() instanceof KItem && equality.rightHandSide() instanceof KItem) {
                    KLabelConstant lKLabel = (KLabelConstant) ((KItem) equality.leftHandSide()).kLabel(); // #f
                    KLabelConstant rKLabel = (KLabelConstant) ((KItem) equality.rightHandSide()).klabel(); // #f

                    if (lKLabel.equals(rKLabel) && matchingSymbols.contains(lKLabel.name())) {
                        KList lKList = (KList) ((KItem) equality.leftHandSide()).kList(); // X
                        KList rKList = (KList) ((KItem) equality.rightHandSide()).kList(); // Y

                        if (lKList.size() == rKList.size()) {
                            for (int i = 0; i < lKList.size(); ++i) {
                                Equality newEquality = new Equality(lKList.get(i), rKList.get(i), global); // X == Y
                                newEqualities = newEqualities.plus(newEquality);
                            }
                            continue;
                        }
                    }
                }
                //anything but continue above
                resultEqualities = resultEqualities.plus(equality);
            }
        }

        if (resultEqualities.equals(this.equalities)) {
            return this;
        } else {
            return new ConjunctiveFormula(substitution, resultEqualities, disjunctions, truthValue, falsifyingEquality, global);
        }
    }

    public ImmutableMapSubstitution<Variable, Term> getSubstitution(Variable variable, Term term) {
        if (RewriteEngineUtils.isSubsortedEq(variable, term, global.getDefinition())) {
            return ImmutableMapSubstitution.singleton(variable, term);
        } else if (term instanceof KItem && ((KItem) term).kLabel() instanceof KLabelConstant && ((KItem) term).kList() instanceof KList
                && ((KLabelConstant) ((KItem) term).kLabel()).isConstructor()
                && ((KList) ((KItem) term).kList()).getContents().stream().allMatch(Variable.class::isInstance)) {
            /**
             * Hack for a special case of order-sorted unification. If the term is an overloaded klabel applied to a klist of variables,
             * and the sort of the term if too generic (i.e. not equal or subsorted to the sort of the variable),
             * then it may be possible that one of the overloaded signatures may give the term a sort compatible with that of the variable.
             * In that case, the variables in the klist are substituted with variables of the appropriate sorts.
             */
            KItem kItem = (KItem) term;
            KLabelConstant kLabelConstant = (KLabelConstant) kItem.kLabel();
            KList kList = (KList) kItem.kList();
            List<Sort> sorts = kItem.possibleSorts().stream()
                    .filter(s -> global.getDefinition().subsorts().isSubsortedEq(variable.sort(), s))
                    .collect(Collectors.toList());
            if (sorts.size() != 1) {
                return null;
            }
            SortSignature signature = kLabelConstant.signatures().stream()
                    .filter(s -> global.getDefinition().subsorts().isSubsortedEq(variable.sort(), s.result()))
                    .findAny().get();
            ImmutableMapSubstitution<Variable, Term> substitution = ImmutableMapSubstitution.empty();
            for (int i = 0; i < kList.size(); i++) {
                substitution = substitution.plus((Variable) kList.get(i), Variable.getAnonVariable(signature.parameters().get(i)));
                if (substitution == null) {
                    return null;
                }
            }
            substitution = substitution.plus(
                    variable,
                    KItem.of(kLabelConstant, KList.concatenate(substitution.keySet().stream().collect(Collectors.toList())), global));
            return substitution;
        } else if (term instanceof Variable) {
            if (RewriteEngineUtils.isSubsortedEq(term, variable, global.getDefinition())) {
                return ImmutableMapSubstitution.singleton((Variable) term, variable);
            } else {
                Sort leastSort = global.getDefinition().subsorts().getGLBSort(
                        variable.sort(),
                        term.sort());
                assert leastSort != null;

                Variable freshVariable = Variable.getAnonVariable(leastSort);
                return ImmutableMapSubstitution.<Variable, Term>singleton(variable, freshVariable)
                        .plus((Variable) term, freshVariable);
            }
        } else {
            return null;
        }
    }

    public ConjunctiveFormula resolveNonDeterministicLookups() {
        ConjunctiveFormula result = this;
        for (Equality equality : equalities) {
            result = resolveNonDeterministicLookup(result, equality.leftHandSide());
            result = resolveNonDeterministicLookup(result, equality.rightHandSide());
        }
        return result;
    }

    private ConjunctiveFormula resolveNonDeterministicLookup(ConjunctiveFormula result, Term term) {
        if (DataStructures.isLookup(term)
                && DataStructures.getLookupBase(term) instanceof BuiltinMap
                && ((BuiltinMap) DataStructures.getLookupBase(term)).isConcreteCollection()) {
            result = result.add(new DisjunctiveFormula((
                    (BuiltinMap) DataStructures.getLookupBase(term)).getEntries().keySet().stream()
                        .map(key -> new Equality(DataStructures.getLookupKey(term), key, global))
                        .filter(e -> !e.isFalse())
                        .map(e -> ConjunctiveFormula.of(global).add(e))
                        .collect(Collectors.toList()),
                    global));
        }
        return result;
    }

    /**
     * Checks if this constraint is a substitution of the given variables.
     * <p>
     * This method is useful for checking if narrowing happens.
     */
    public boolean isMatching(Set<Variable> variables) {
        return isSubstitution() && substitution.keySet().equals(variables);
    }

    public boolean isSubstitution() {
        return equalities.stream().allMatch(e -> e.leftHandSide() instanceof LocalRewriteTerm)
                && disjunctions.isEmpty();
    }

    public ConjunctiveFormula orientSubstitution(Set<Variable> variables) {
        if (substitution.keySet().containsAll(variables)) {
            return this;
        }

        /* compute the pre-images of each variable in the co-domain of the substitution */
        Multimap<Variable, Variable> equivalenceClasses = HashMultimap.create();
        substitution.entrySet().stream()
                .filter(e -> e.getValue() instanceof Variable)
                .forEach(e -> equivalenceClasses.put((Variable) e.getValue(), e.getKey()));

        Substitution<Variable, Variable> orientationSubstitution = ImmutableMapSubstitution.empty();
        for (Map.Entry<Variable, Collection<Variable>> entry : equivalenceClasses.asMap().entrySet()) {
            if (variables.contains(entry.getKey())) {
                Optional<Variable> replacement = entry.getValue().stream()
                        .filter(v -> v.sort().equals(entry.getKey().sort()))
                        .filter(v -> !variables.contains(v))
                        .findAny();
                if (replacement.isPresent()) {
                    orientationSubstitution = orientationSubstitution
                            .plus(entry.getKey(), replacement.get())
                            .plus(replacement.get(), entry.getKey());
                }
            }
        }

        TermContext context = TermContext.builder(global).build();
        return ((ConjunctiveFormula) substituteWithBinders(orientationSubstitution)).simplify(context);
    }

    public ConjunctiveFormula expandPatterns(boolean narrowing, TermContext context) {
        return new ConstrainedTerm(Bottom.BOTTOM, this, context).expandPatterns(narrowing).constraint();
    }

    public DisjunctiveFormula getDisjunctiveNormalForm() {
        if (disjunctions.isEmpty()) {
            return new DisjunctiveFormula(PersistentUniqueList.singleton(this), global);
        }

        ConjunctiveFormula result = ConjunctiveFormula.of(
                substitution,
                equalities,
                PersistentUniqueList.empty(),
                global);

        List<Set<ConjunctiveFormula>> collect = disjunctions.stream()
                .map(disjunction -> disjunction.conjunctions().stream()
                        .map(ConjunctiveFormula::getDisjunctiveNormalForm)
                        .map(DisjunctiveFormula::conjunctions)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet()))
                .collect(Collectors.toList());
        return new DisjunctiveFormula(Sets.cartesianProduct(collect).stream()
                .map(result::addAll)
                .collect(Collectors.toList()), global);
    }

    public boolean checkUnsat(FormulaContext formulaContext) {
        formulaContext.z3Profiler.newRequest();
        boolean unsat = global.constraintOps.checkUnsat(this, formulaContext);
        if (global.javaExecutionOptions.debugZ3) {
            formulaContext.printUnsat(this, unsat, false);
        }
        return unsat;
    }

    public boolean smartImplies(ConjunctiveFormula constraint) {

        /* TODO: from org.kframework.backend.java.kil.ConstrainedTerm.matchImplies
        context.setTopConstraint(data.constraint);
        constraint = (ConjunctiveFormula) constraint.evaluate(context);

        Set<Variable> rightOnlyVariables = Sets.difference(constraint.variableSet(), variableSet());
        constraint = constraint.orientSubstitution(rightOnlyVariables);

        ConjunctiveFormula leftHandSide = data.constraint;
        ConjunctiveFormula rightHandSide = constraint.removeBindings(rightOnlyVariables);
        rightHandSide = (ConjunctiveFormula) rightHandSide.substitute(leftHandSide.substitution());
        if (!leftHandSide.implies(rightHandSide, rightOnlyVariables)) {
            return null;
        }
         */

        constraint = (ConjunctiveFormula) constraint.substitute(this.substitution());
        return implies(constraint, Collections.emptySet(),
                new FormulaContext(FormulaContext.Kind.EquivImplication, null, constraint.global));
    }

    /**
     * Checks if {@code this} implies {@code rightHandSide}, assuming that {@code existentialQuantVars}
     * are existentially quantified.
     */
    public boolean implies(ConjunctiveFormula rightHandSide, Set<Variable> existentialQuantVars, FormulaContext formulaContext) {
        // TODO(AndreiS): this can prove "stuff -> false", it needs fixing
        assert !rightHandSide.isFalse();

        LinkedList<Pair<ConjunctiveFormula, ConjunctiveFormula>> implications = new LinkedList<>();
        implications.add(Pair.of(this, rightHandSide));
        while (!implications.isEmpty()) {
            Pair<ConjunctiveFormula, ConjunctiveFormula> implication = implications.remove();
            ConjunctiveFormula left = implication.getLeft();
            ConjunctiveFormula right = implication.getRight();
            if (left.isFalseExtended()) {
                continue;
            }

            if (global.javaExecutionOptions.debugFormulas) {
                System.err.format("\nAttempting to prove:\n================= \n\t%s\n  implies \n\t%s\n", left, right);
            }

            right = right.orientSubstitution(existentialQuantVars);
            right = left.simplifyConstraint(right);
            right = right.orientSubstitution(existentialQuantVars);
            if (right.isTrue() || (right.equalities().isEmpty() && existentialQuantVars.containsAll(right.substitution().keySet()))) {
                if (global.javaExecutionOptions.debugFormulas) {
                    System.err.println("Implication proved by simplification");
                }
                continue;
            }

            IfThenElseFinder ifThenElseFinder = new IfThenElseFinder();
            right.accept(ifThenElseFinder);
            if (!ifThenElseFinder.result.isEmpty()) {
                KItem ite = ifThenElseFinder.result.get(0);
                // TODO (AndreiS): handle KList variables
                Term condition = ((KList) ite.kList()).get(0);
                if (global.javaExecutionOptions.debugFormulas) {
                    System.err.format("Split on %s\n", condition);
                }
                TermContext context = TermContext.builder(global).build();
                implications.add(Pair.of(left.add(condition, BoolToken.TRUE).simplify(context), right));
                implications.add(Pair.of(left.add(condition, BoolToken.FALSE).simplify(context), right));
                continue;
            }

            //Removing LHS substitution because it's not used to build Z3 query anyway.
            //Improves Z3 cache efficiency.
            ConjunctiveFormula leftWithoutSubst = ConjunctiveFormula.of(ImmutableMapSubstitution.empty(),
                    left.equalities(), left.disjunctions(), left.globalContext());
            global.stateLog.log(StateLog.LogEvent.IMPLICATION, leftWithoutSubst, right);
            if (!impliesSMT(leftWithoutSubst, right, existentialQuantVars, formulaContext)) {
                if (global.javaExecutionOptions.debugFormulas) {
                    System.err.println("Failure!");
                }
                return false;
            } else {
                if (global.javaExecutionOptions.debugFormulas) {
                    System.err.println("Proved!");
                }
            }
        }

        return true;
    }

    /**
     * Simplifies the given constraint by eliding the equalities and substitution entries that are
     * implied by this constraint.
     */
    private ConjunctiveFormula simplifyConstraint(ConjunctiveFormula constraint) {
        Substitution<Variable, Term> simplifiedSubstitution = constraint.substitution.minusAll(
                Maps.difference(constraint.substitution, substitution).entriesInCommon().keySet());
        Predicate<Equality> inConstraint = equality -> !equalities().contains(equality)
                && !equalities().contains(new Equality(equality.rightHandSide(), equality.leftHandSide(), global));
        PersistentUniqueList<Equality> simplifiedEqualities = PersistentUniqueList.from(
                constraint.equalities().stream().filter(inConstraint).collect(Collectors.toList()));
        ConjunctiveFormula simplifiedConstraint = ConjunctiveFormula.of(
                simplifiedSubstitution,
                simplifiedEqualities,
                constraint.disjunctions,
                constraint.global);

        if (simplifiedConstraint.isTrue()) {
            return simplifiedConstraint;
        }

        // TODO(AndreiS): investigate what this code does
//        Map<Term, Term> substitution = Maps.newLinkedHashMap();
//        for (Equality e1:equalities()) {
//            if (e1.rightHandSide().isGround()) {
//                substitution.put(e1.leftHandSide(), e1.rightHandSide());
//            }
//            if (e1.leftHandSide().isGround()) {
//                substitution.put(e1.rightHandSide(), e1.leftHandSide());
//            }
//        }
//        simplifiedConstraint = (SymbolicConstraint) TermSubstitutionTransformer
//                .substitute(simplifiedConstraint, substitution, context);

        return simplifiedConstraint;
    }

    private static final Map<Triple<ConjunctiveFormula, ConjunctiveFormula, Set<Variable>>, Boolean> impliesSMTCache = Collections.synchronizedMap(new HashMap<>());

    public static CounterStopwatch impliesStopwatch = new CounterStopwatch("impliesSMT");

    /**
     * Checks if {@code left} implies {@code right}, assuming that {@code existentialQuantVars}
     * are existentially quantified.
     */
    private static boolean impliesSMT(
            ConjunctiveFormula left,
            ConjunctiveFormula right,
            Set<Variable> existentialQuantVars,
            FormulaContext formulaContext) {
        impliesStopwatch.start();
        formulaContext.z3Profiler.newRequest();
        try {
            Triple<ConjunctiveFormula, ConjunctiveFormula, Set<Variable>> triple = Triple.of(left, right, existentialQuantVars);
            boolean cached = true;
            if (!impliesSMTCache.containsKey(triple)) {
                impliesSMTCache.put(triple,
                        left.global.constraintOps.impliesSMT(left, right, existentialQuantVars, formulaContext));
                cached = false;
            }
            Boolean result = impliesSMTCache.get(triple);

            if (left.globalContext().javaExecutionOptions.debugZ3) {
                formulaContext.printImplication(left, right, result, cached);
            }
            return result;
        } finally {
            impliesStopwatch.stop();
        }
    }

    public boolean hasMapEqualities() {
        for (Equality equality : equalities) {
            if (equality.leftHandSide() instanceof BuiltinMap
                    || equality.rightHandSide() instanceof BuiltinMap) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isExactSort() {
        return true;
    }

    @Override
    public boolean isSymbolic() {
        return true;
    }

    @Override
    public Sort sort() {
        return Sort.BOOL;
    }

    @Override
    public List<Term> getKComponents() {
        Stream<Term> stream = Stream.concat(
                Stream.concat(
                        substitution.equalities(global).stream().map(Equality::toK),
                        equalities.stream().map(Equality::toK)),
                disjunctions.stream().map(DisjunctiveFormula::toKore));
        return stream.collect(Collectors.toList());
    }

    @Override
    public KLabel constructorLabel() {
        return KLabelConstant.of(KLabels.ML_AND, global.getDefinition());
    }

    @Override
    public KItem unit() {
        return KItem.of(KLabelConstant.of(KLabels.ML_TRUE, global.getDefinition()), KList.EMPTY, global);
    }

    @Override
    protected int computeHash() {
        int hashCode = 1;
        hashCode = hashCode * Constants.HASH_PRIME + substitution.hashCode();
        hashCode = hashCode * Constants.HASH_PRIME + equalities.hashCode();
        hashCode = hashCode * Constants.HASH_PRIME + disjunctions.hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof ConjunctiveFormula)) {
            return false;
        }

        ConjunctiveFormula conjunction = (ConjunctiveFormula) object;
        return substitution.equals(conjunction.substitution)
                && equalities.equals(conjunction.equalities)
                && disjunctions.equals(conjunction.disjunctions);
    }

    @Override
    public String toString() {
        return toKore().toString();
    }

    public String toStringMultiline() {
        Map<String, List<String>> nameToStreamMap = ImmutableMap.<String, List<String>>builder()
                .put("substitutions:", substitution.equalities(global).stream()
                        .map(equality -> equality.toK().toString()).sorted().collect(Collectors.toList()))
                .put("equalities:", equalities.stream()
                        .map(equality -> equality.toK().toString()).sorted().collect(Collectors.toList()))
                .put("disjunctions:", disjunctions.stream()
                        .map(disjunctiveFormula -> disjunctiveFormula.toKore().toString()).sorted()
                        .collect(Collectors.toList()))
                .build();

        StringBuilder sb = new StringBuilder();
        sb.append("ConjunctiveFormula(");
        for (String cat : nameToStreamMap.keySet()) {
            if (!nameToStreamMap.get(cat).isEmpty()) {
                sb.append("\n  ").append(cat);
                for (String line : nameToStreamMap.get(cat)) {
                    sb.append("\n    ").append(line);
                }
            }
        }
        sb.append("\n)");
        return sb.toString();
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public JavaSymbolicObject accept(Transformer transformer) {
        return transformer.transform(this);
    }

}
