// Copyright (c) 2014-2018 K Team. All Rights Reserved.
package org.kframework.backend.java.builtins;


import org.kframework.backend.java.kil.*;
import org.kframework.backend.java.kil.Term;
import org.kframework.backend.java.symbolic.UserSubstitutionTransformer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Table of {@code public static} methods on builtin user-level substitution.
 *
 * @author: TraianSF
 */
public class BuiltinSubstitutionOperations {

    public static Term userSubstitution(Term[] terms, TermContext context) {
        Term term = terms[0];
        Term substitute = terms[1];
        Term variable = terms[2];
        return KLabelInjection.injectionOf(UserSubstitutionTransformer.userSubstitution(Collections.singletonMap(variable, substitute), term, context), context.global());
    }

    public static Term userSingletonSubstitutionKore(Term[] terms, TermContext context) {
        Term term = terms[0];
        Term substitute = terms[1];
        Term variable = terms[2];
        return UserSubstitutionTransformer.userSubstitution(Collections.singletonMap(variable, substitute), term, context);
    }

    public static Term userSubstitutionKore(Term[] terms, TermContext context) {
        Term term = terms[0];
        BuiltinMap substitution = (BuiltinMap) terms[1];
        if (!substitution.isConcreteCollection()) {
            throw new IllegalArgumentException();
        }

        return UserSubstitutionTransformer.userSubstitution(substitution.getEntries(), term, context);
    }

}
