// Copyright (c) 2014-2018 K Team. All Rights Reserved.
package org.kframework.backend.java.builtins;

import static org.junit.Assert.*;

import org.junit.Test;
import org.kframework.backend.java.kil.Term;
import org.kframework.backend.java.kil.TermContext;
import org.mockito.Mock;

public class BuiltinFloatOperationsTest {

    @Mock
    TermContext context;

    @Test
    public void testLoadLibrary() {
        FloatToken result = (FloatToken) BuiltinFloatOperations.add(new Term[] {FloatToken.of("1.0"), FloatToken.of("2.0")}, context);
        assertEquals(FloatToken.of("3.0"), result);
    }
}
