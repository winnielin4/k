// Copyright (c) 2013-2018 K Team. All Rights Reserved.
package org.kframework.backend.java.builtins;

import java.math.BigInteger;

import org.apache.commons.lang3.StringUtils;
import org.kframework.backend.java.kil.Sort;
import org.kframework.backend.java.kil.Term;
import org.kframework.backend.java.kil.TermContext;
import org.kframework.backend.java.kil.Token;
import org.kframework.compile.FloatBuiltin;
import org.kframework.utils.StringUtil;

/**
 * Table of {@code public static} methods on builtin strings.
 *
 * @author: DwightG
 */

public class BuiltinStringOperations {

    public static Term add(Term[] terms, TermContext context) {
        StringToken term1 = (StringToken) terms[0];
        StringToken term2 = (StringToken) terms[1];
        return StringToken.of(term1.stringValue() + term2.stringValue());
    }

    public static Term eq(Term[] terms, TermContext context) {
        StringToken term1 = (StringToken) terms[0];
        StringToken term2 = (StringToken) terms[1];
        return BoolToken.of(term1.stringValue().compareTo(term2.stringValue()) == 0);
    }

    public static Term ne(Term[] terms, TermContext context) {
        StringToken term1 = (StringToken) terms[0];
        StringToken term2 = (StringToken) terms[1];
        return BoolToken.of(term1.stringValue().compareTo(term2.stringValue()) != 0);
    }

    public static Term gt(Term[] terms, TermContext context) {
        StringToken term1 = (StringToken) terms[0];
        StringToken term2 = (StringToken) terms[1];
        return BoolToken.of(term1.stringValue().compareTo(term2.stringValue()) > 0);
    }

    public static Term ge(Term[] terms, TermContext context) {
        StringToken term1 = (StringToken) terms[0];
        StringToken term2 = (StringToken) terms[1];
        return BoolToken.of(term1.stringValue().compareTo(term2.stringValue()) >= 0);
    }

    public static Term lt(Term[] terms, TermContext context) {
        StringToken term1 = (StringToken) terms[0];
        StringToken term2 = (StringToken) terms[1];
        return BoolToken.of(term1.stringValue().compareTo(term2.stringValue()) < 0);
    }

    public static Term le(Term[] terms, TermContext context) {
        StringToken term1 = (StringToken) terms[0];
        StringToken term2 = (StringToken) terms[1];
        return BoolToken.of(term1.stringValue().compareTo(term2.stringValue()) <= 0);
    }

    public static Term len(Term[] terms, TermContext context) {
        StringToken term = (StringToken) terms[0];
        return IntToken.of(term.stringValue().codePointCount(0, term.stringValue().length()));
    }

    public static Term ord(Term[] terms, TermContext context) {
        StringToken term = (StringToken) terms[0];
        if (term.stringValue().codePointCount(0, term.stringValue().length()) != 1) {
            return null;
        }
        return IntToken.of(term.stringValue().codePointAt(0));
    }

    public static Term chr(Term[] terms, TermContext context) {
        IntToken term = (IntToken) terms[0];
        //safe because we know it's in the unicode code range or it will throw
        int codePoint = term.intValue();
        try {
            StringUtil.throwIfSurrogatePair(codePoint);
            char[] chars = Character.toChars(codePoint);
            return StringToken.of(new String(chars));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Term substr(Term[] terms, TermContext context) {
        StringToken term = (StringToken) terms[0];
        IntToken start = (IntToken) terms[1];
        IntToken end = (IntToken) terms[2];
        int beginOffset = term.stringValue().offsetByCodePoints(0, start.intValue());
        int endOffset = term.stringValue().offsetByCodePoints(0, end.intValue());
        try {
            return StringToken.of(term.stringValue().substring(beginOffset, endOffset));
        } catch (StringIndexOutOfBoundsException e) {
            return null;
        }
    }

    public static Term find(Term[] terms, TermContext context) {
        StringToken term1 = (StringToken) terms[0];
        StringToken term2 = (StringToken) terms[1];
        IntToken idx = (IntToken) terms[2];
        int offset = term1.stringValue().offsetByCodePoints(0, idx.intValue());
        int foundOffset = term1.stringValue().indexOf(term2.stringValue(), offset);
        return IntToken.of((foundOffset == -1 ? -1 : term1.stringValue().codePointCount(0, foundOffset)));
    }

    public static Term rfind(Term[] terms, TermContext context) {
        StringToken term1 = (StringToken) terms[0];
        StringToken term2 = (StringToken) terms[1];
        IntToken idx = (IntToken) terms[2];
        int offset = term1.stringValue().offsetByCodePoints(0, idx.intValue());
        int foundOffset = term1.stringValue().lastIndexOf(term2.stringValue(), offset);
        return IntToken.of((foundOffset == -1 ? -1 : term1.stringValue().codePointCount(0, foundOffset)));
    }

    public static Term findChar(Term[] terms, TermContext context) {
        StringToken term1 = (StringToken) terms[0];
        StringToken term2 = (StringToken) terms[1];
        IntToken idx = (IntToken) terms[2];
        int offset = term1.stringValue().offsetByCodePoints(0, idx.intValue());
        int foundOffset = StringUtil.indexOfAny(term1.stringValue(), term2.stringValue(), offset);
        return IntToken.of((foundOffset == -1 ? -1 : term1.stringValue().codePointCount(0, foundOffset)));
    }

    public static Term rfindChar(Term[] terms, TermContext context) {
        StringToken term1 = (StringToken) terms[0];
        StringToken term2 = (StringToken) terms[1];
        IntToken idx = (IntToken) terms[2];
        int offset = term1.stringValue().offsetByCodePoints(0, idx.intValue());
        int foundOffset = StringUtil.lastIndexOfAny(term1.stringValue(), term2.stringValue(), offset);
        return IntToken.of((foundOffset == -1 ? -1 : term1.stringValue().codePointCount(0, foundOffset)));
    }

    public static Term string2int(Term[] terms, TermContext context) {
        StringToken term = (StringToken) terms[0];
        try {
            return IntToken.of(term.stringValue());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Term string2base(Term[] terms, TermContext context) {
        StringToken term = (StringToken) terms[0];
        IntToken base = (IntToken) terms[1];
        return IntToken.of(new BigInteger(term.stringValue(), base.intValue()));
    }

    public static Term base2string(Term[] terms, TermContext context) {
        IntToken integer = (IntToken) terms[0];
        IntToken base = (IntToken) terms[1];
        return StringToken.of(integer.bigIntegerValue().toString(base.intValue()));
    }

    public static Term string2float(Term[] terms, TermContext context) {
        StringToken term = (StringToken) terms[0];
        try {
            return FloatToken.of(term.stringValue());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Term float2string(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        return StringToken.of(FloatBuiltin.printKFloat(term.bigFloatValue(), term.bigFloatValue()::toString));
    }

    public static Term floatFormat(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        StringToken format = (StringToken) terms[1];
        return StringToken.of(FloatBuiltin.printKFloat(term.bigFloatValue(), () -> term.bigFloatValue().toString(format.stringValue())));
    }

    public static Term int2string(Term[] terms, TermContext context) {
        IntToken term = (IntToken) terms[0];
        return StringToken.of(term.bigIntegerValue().toString());
    }
/*
    // when we support java 7
    public static Term name(StringToken term) {
        if (term.stringValue().codePointCount(0, term.stringValue().length()) != 1) {
            throw new IllegalArgumentException();
        }
        String name = Character.getName(term.stringValue().codePointAt(0));
        if (name == null) {
            throw new IllegalArgumentExceptino();
        }
        return StringToken.of(name);
    }
*/
    public static Term category(Term[] terms, TermContext context) {
        StringToken term = (StringToken) terms[0];
        if (term.stringValue().codePointCount(0, term.stringValue().length()) != 1) {
            throw new IllegalArgumentException();
        }
        int cat = Character.getType(term.stringValue().codePointAt(0));
        assert cat >= 0 && cat < 128 : "not a byte???";
        return StringToken.of(StringUtil.getCategoryCode((byte)cat));
    }

    public static Term directionality(Term[] terms, TermContext context) {
        StringToken term = (StringToken) terms[0];
        if (term.stringValue().codePointCount(0, term.stringValue().length()) != 1) {
            throw new IllegalArgumentException();
        }
        byte cat = Character.getDirectionality(term.stringValue().codePointAt(0));
        return StringToken.of(StringUtil.getDirectionalityCode(cat));
    }

    public static Term token2string(Term[] terms, TermContext context) {
        UninterpretedToken token = (UninterpretedToken) terms[0];
        return StringToken.of(token.javaBackendValue());
    }

    public static Term string2token(Term[] terms, TermContext context) {
        StringToken sort = (StringToken) terms[0];
        StringToken value = (StringToken) terms[1];
        return Token.of(Sort.parse(sort.stringValue()), value.stringValue());
    }

    /**
     * Replaces all occurrences of a string within another string.
     *
     * @param text
     *            the string to search and replace in
     * @param search
     *            the string to search for
     * @param replacement
     *            the string to replace it with
     * @param context
     *            the term context
     * @return the text with any replacements processed
     */
    public static Term replaceAll(Term[] terms, TermContext context) {
        StringToken text = (StringToken) terms[0];
        StringToken searchString = (StringToken) terms[1];
        StringToken replacement = (StringToken) terms[2];
        return StringToken.of(StringUtils.replace(text.stringValue(),
                searchString.stringValue(), replacement.stringValue()));
    }

    /**
     * Replaces all occurrences of a string within another string, for the first
     * max values of the search string.
     *
     * @param text
     *            the string to search and replace in
     * @param search
     *            the string to search for
     * @param replacement
     *            the string to replace it with
     * @param max
     *            the maximum number of occurrences to be replaced
     * @param context
     *            the term context
     * @return the text with any replacements processed
     */
    public static Term replace(Term[] terms, TermContext context) {
        StringToken text = (StringToken) terms[0];
        StringToken searchString = (StringToken) terms[1];
        StringToken replacement = (StringToken) terms[2];
        IntToken max = (IntToken) terms[3];
        return StringToken.of(StringUtils.replace(text.stringValue(),
                searchString.stringValue(), replacement.stringValue(),
                max.intValue()));
    }

    /**
     * Replaces the first occurrence of a string within another string.
     *
     * @param text
     *            the string to search and replace in
     * @param search
     *            the string to search for
     * @param replacement
     *            the string to replace it with
     * @param context
     *            the term context
     * @return the text with any replacements processed
     */
    public static Term replaceFirst(Term[] terms, TermContext context) {
        StringToken text = (StringToken) terms[0];
        StringToken searchString = (StringToken) terms[1];
        StringToken replacement = (StringToken) terms[2];
        return StringToken.of(StringUtils.replaceOnce(text.stringValue(),
                searchString.stringValue(), replacement.stringValue()));
    }

    /**
     * Counts how many times the substring appears in another string.
     *
     * @param text
     *            the string to search in
     * @param substr
     *            the substring to search for
     * @param context
     *            the term context
     * @return the number of occurrences
     */
    public static Term countOccurences(Term[] terms, TermContext context) {
        StringToken text = (StringToken) terms[0];
        StringToken substr = (StringToken) terms[1];
        return IntToken.of(StringUtils.countMatches(text.stringValue(),
                substr.stringValue()));
    }
}
