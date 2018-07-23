// Copyright (c) 2013-2018 K Team. All Rights Reserved.
package org.kframework.backend.java.builtins;

import org.kframework.backend.java.kil.BuiltinList;
import org.kframework.backend.java.kil.BuiltinMap;
import org.kframework.backend.java.kil.BuiltinSet;
import org.kframework.backend.java.kil.Definition;
import org.kframework.backend.java.kil.KCollection;
import org.kframework.backend.java.kil.KItem;
import org.kframework.backend.java.kil.KLabelConstant;
import org.kframework.backend.java.kil.KList;
import org.kframework.backend.java.kil.Sort;
import org.kframework.backend.java.kil.Term;
import org.kframework.backend.java.kil.TermContext;
import org.kframework.backend.java.kil.Token;

/**
 * Utility class for checking sort membership predicates.
 *
 * @author AndreiS
 */
public class SortMembership {

    /**
     * Evaluates a sort membership predicate with respect to a given
     * {@link org.kframework.kil.loader.Context}.
     *
     * @param kItem
     *            the sort membership predicate
     * @param definition
     *            the definition
     * @return {@link BoolToken#TRUE} if the predicate is true; or
     *         {@link BoolToken#FALSE} if the predicate is false; otherwise, the
     *         {@code kItem} itself if the evaluation gets stuck
     */
    public static Term check(KItem kItem, Definition definition) {
        assert kItem.kLabel() instanceof KLabelConstant;
        assert kItem.kList() instanceof KList
                && ((KList) kItem.kList()).concreteSize() == 1
                && ((KList) kItem.kList()).isConcreteCollection() : "unexpected argument: " + kItem.kList();

        Sort predicateSort = ((KLabelConstant) kItem.kLabel()).getPredicateSort();
        if (!definition.allSorts().contains(predicateSort)) {
            return kItem;
        }

        Term term = ((KList) kItem.kList()).getContents().get(0);
        Sort termSort = term.sort();
        if (term.isExactSort()) {
            return definition.subsorts().isSubsortedEq(predicateSort, termSort) ? BoolToken.TRUE : BoolToken.FALSE;
        } else if (definition.subsorts().isSubsortedEq(predicateSort, termSort)) {
            return BoolToken.TRUE;
        } else if (!definition.subsorts().hasCommonSubsort(predicateSort, termSort)) {
            return BoolToken.FALSE;
        } else {
            return kItem;
        }
    }
}
