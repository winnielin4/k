// Copyright (c) 2013-2018 K Team. All Rights Reserved.
package org.kframework.backend.java.builtins;

import java.math.RoundingMode;

import org.kframework.backend.java.kil.*;
import org.kframework.mpfr.BigFloat;
import org.kframework.mpfr.BinaryMathContext;

/**
 * Table of {@code public static} methods on builtin floats.
 *
 * @author: dwightguth
 */
public class BuiltinFloatOperations {

    /**
     * Get the {@link BinaryMathContext} object to use to compute the arithmetic operation.
     *
     * Currently only floats with the same precision and exponent range can be used in a calculation.
     * Users will have to cast floating point types manually using round() if they wish.
     */
    private static BinaryMathContext getMathContext(FloatToken term1, FloatToken term2) {
        getExponent(term1, term2);
        if (term1.bigFloatValue().precision() != term2.bigFloatValue().precision()) {
            throw new IllegalArgumentException("mismatch precision: "
                    + "first argument precision is represented on " + term1.bigFloatValue().precision() + " bits "
                    + "while second argument precision is represented on " + term2.bigFloatValue().precision() + "bits");
        }
        return getMathContext(term1);
    }

    /**
     * Get the {@link BinaryMathContext} object to use to compute the arithmetic operation. Uses
     * {@link RoundingMode#HALF_EVEN} and the precision and exponent range of the {@link FloatToken}.
     */
    private static BinaryMathContext getMathContext(FloatToken term) {
        return new BinaryMathContext(term.bigFloatValue().precision(), term.exponent());
    }

    /**
     * Get the number of bits of exponent to use to compute the arithmetic operation.
     *
     * Currently only floats with the same precision and exponent range can be used in a calculation.
     * Users will have to cast floating point types manually using round() if they wish.
     */
    private static int getExponent(FloatToken term1, FloatToken term2) {
        if (term1.exponent() != term2.exponent()) {
            throw new IllegalArgumentException("mismatch exponent: "
                + "first argument exponent is represented on " + term1.exponent() + " bits "
                + "while second argument exponent is represented on " + term2.exponent() + "bits");
        }
        return term1.exponent();
    }

    public static Term precision(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        return IntToken.of(term.bigFloatValue().precision());
    }

    public static Term exponent(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        BinaryMathContext mc = getMathContext(term);
        return IntToken.of(term.bigFloatValue().exponent(mc.minExponent, mc.maxExponent));
    }

    public static Term exponentBits(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        return IntToken.of(term.exponent());
    }

    public static Term sign(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        return BoolToken.of(term.bigFloatValue().sign());
    }

    public static Term significand(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        BinaryMathContext mc = getMathContext(term);
        return BitVector.of(term.bigFloatValue().significand(mc.minExponent, mc.maxExponent), mc.precision);
    }

    public static Term add(Term[] terms, TermContext context) {
        FloatToken term1 = (FloatToken) terms[0];
        FloatToken term2 = (FloatToken) terms[1];
        return FloatToken.of(term1.bigFloatValue().add(term2.bigFloatValue(),
                getMathContext(term1, term2)), getExponent(term1, term2));
    }

    public static Term sub(Term[] terms, TermContext context) {
        FloatToken term1 = (FloatToken) terms[0];
        FloatToken term2 = (FloatToken) terms[1];
         return FloatToken.of(term1.bigFloatValue().subtract(term2.bigFloatValue(),
                 getMathContext(term1, term2)), getExponent(term1, term2));
    }

    public static Term mul(Term[] terms, TermContext context) {
        FloatToken term1 = (FloatToken) terms[0];
        FloatToken term2 = (FloatToken) terms[1];
         return FloatToken.of(term1.bigFloatValue().multiply(term2.bigFloatValue(),
                 getMathContext(term1, term2)), getExponent(term1, term2));
    }

    public static Term div(Term[] terms, TermContext context) {
        FloatToken term1 = (FloatToken) terms[0];
        FloatToken term2 = (FloatToken) terms[1];
        return FloatToken.of(term1.bigFloatValue().divide(term2.bigFloatValue(),
                getMathContext(term1, term2)), getExponent(term1, term2));
    }

    public static Term rem(Term[] terms, TermContext context) {
        FloatToken term1 = (FloatToken) terms[0];
        FloatToken term2 = (FloatToken) terms[1];
        return FloatToken.of(term1.bigFloatValue().remainder(term2.bigFloatValue(),
                getMathContext(term1, term2)), getExponent(term1, term2));
    }

    public static Term pow(Term[] terms, TermContext context) {
        FloatToken term1 = (FloatToken) terms[0];
        FloatToken term2 = (FloatToken) terms[1];
        return FloatToken.of(term1.bigFloatValue().pow(term2.bigFloatValue(),
                getMathContext(term1, term2)), getExponent(term1, term2));
    }

    public static Term root(Term[] terms, TermContext context) {
        FloatToken term1 = (FloatToken) terms[0];
        IntToken term2 = (IntToken) terms[1];
        return FloatToken.of(term1.bigFloatValue().root(term2.intValue(),
                getMathContext(term1)), term1.exponent());
    }

    public static Term unaryMinus(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        return FloatToken.of(term.bigFloatValue().negate(
                getMathContext(term)), term.exponent());
    }

    public static Term abs(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        return FloatToken.of(term.bigFloatValue().abs(
                getMathContext(term)), term.exponent());
    }

    /**
     * Rounds {@code term} to the specfiied precision and exponent range.
     *
     * Method is undefined if either integer is less than 2 because 2 is the minimum precision and exponent.
     * Two exponents must be used to store zero/subnormal/infinity/nan, so 4 is the minimum number of distinct
     * exponents a float can have. MPFR does not support floats with 1 bit of precision.
     * bits of the float.
     */
    public static Term round(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        IntToken precision = (IntToken) terms[1];
        IntToken exponent = (IntToken) terms[2];
        if (precision.intValue() < 2 || exponent.intValue() < 2) {
            return null;
        }
        return FloatToken.of(term.bigFloatValue().round(
                new BinaryMathContext(precision.intValue(), exponent.intValue())),
                exponent.intValue());
    }

    public static Term exp(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        return FloatToken.of(term.bigFloatValue().exp(
                getMathContext(term)), term.exponent());
    }

    public static Term log(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        return FloatToken.of(term.bigFloatValue().log(
                getMathContext(term)), term.exponent());
    }

    public static Term sin(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        return FloatToken.of(term.bigFloatValue().sin(
                getMathContext(term)), term.exponent());
    }

    public static Term cos(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        return FloatToken.of(term.bigFloatValue().cos(
                getMathContext(term)), term.exponent());
    }

    public static Term tan(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        return FloatToken.of(term.bigFloatValue().tan(
                getMathContext(term)), term.exponent());
    }

    public static Term asin(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        return FloatToken.of(term.bigFloatValue().asin(
                getMathContext(term)), term.exponent());
    }

    public static Term acos(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        return FloatToken.of(term.bigFloatValue().acos(
                getMathContext(term)), term.exponent());
    }

    public static Term atan(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        return FloatToken.of(term.bigFloatValue().atan(
                getMathContext(term)), term.exponent());
    }

    public static Term atan2(Term[] terms, TermContext context) {
        FloatToken term1 = (FloatToken) terms[0];
        FloatToken term2 = (FloatToken) terms[1];
        return FloatToken.of(BigFloat.atan2(term1.bigFloatValue(), term2.bigFloatValue(),
                getMathContext(term1, term2)), getExponent(term1, term2));
    }

    public static Term max(Term[] terms, TermContext context) {
        FloatToken term1 = (FloatToken) terms[0];
        FloatToken term2 = (FloatToken) terms[1];
        return FloatToken.of(BigFloat.max(term1.bigFloatValue(), term2.bigFloatValue(),
                getMathContext(term1, term2)), getExponent(term1, term2));
    }

    public static Term min(Term[] terms, TermContext context) {
        FloatToken term1 = (FloatToken) terms[0];
        FloatToken term2 = (FloatToken) terms[1];
        return FloatToken.of(BigFloat.min(term1.bigFloatValue(), term2.bigFloatValue(),
                getMathContext(term1, term2)), getExponent(term1, term2));
    }

    /**
     * Floating point equality. Uses {@link BigFloat#equalTo(BigFloat)} and not {@link BigFloat#equals(Object)}
     * in order to preserve the behavior that -0.0 ==Float 0.0 and NaN =/=Float NaN. ==K can be used to compare
     * identity on floating point numbers.
     */
    public static Term eq(Term[] terms, TermContext context) {
        FloatToken term1 = (FloatToken) terms[0];
        FloatToken term2 = (FloatToken) terms[1];
        return BoolToken.of(term1.bigFloatValue().equalTo(term2.bigFloatValue()));
    }

    public static Term gt(Term[] terms, TermContext context) {
        FloatToken term1 = (FloatToken) terms[0];
        FloatToken term2 = (FloatToken) terms[1];
        return BoolToken.of(term1.bigFloatValue().greaterThan(term2.bigFloatValue()));
    }

    public static Term ge(Term[] terms, TermContext context) {
        FloatToken term1 = (FloatToken) terms[0];
        FloatToken term2 = (FloatToken) terms[1];
        return BoolToken.of(term1.bigFloatValue().greaterThanOrEqualTo(term2.bigFloatValue()));
    }

    public static Term lt(Term[] terms, TermContext context) {
        FloatToken term1 = (FloatToken) terms[0];
        FloatToken term2 = (FloatToken) terms[1];
        return BoolToken.of(term1.bigFloatValue().lessThan(term2.bigFloatValue()));
    }

    public static Term le(Term[] terms, TermContext context) {
        FloatToken term1 = (FloatToken) terms[0];
        FloatToken term2 = (FloatToken) terms[1];
        return BoolToken.of(term1.bigFloatValue().lessThanOrEqualTo(term2.bigFloatValue()));
    }

    public static Term int2float(Term[] terms, TermContext context) {
        IntToken term = (IntToken) terms[0];
        IntToken precision = (IntToken) terms[1];
        IntToken exponent = (IntToken) terms[2];
        return FloatToken.of(new BigFloat(term.bigIntegerValue(),
                new BinaryMathContext(precision.intValue(), exponent.intValue())), exponent.intValue());
    }

    /**
     * Rounds {@code term} to an integer by truncating it. Function is only
     * defined on ordinary numbers (i.e. not NaN or infinity).
     */
    public static Term float2int(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        return IntToken.of(term.bigFloatValue().rint(getMathContext(term)
                .withRoundingMode(RoundingMode.DOWN)).toBigIntegerExact());
    }

    public static Term ceil(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        return FloatToken.of(term.bigFloatValue().rint(getMathContext(term)
                .withRoundingMode(RoundingMode.CEILING)), term.exponent());
    }

    public static Term floor(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        return FloatToken.of(term.bigFloatValue().rint(getMathContext(term)
                .withRoundingMode(RoundingMode.FLOOR)), term.exponent());
    }

    public static Term isNaN(Term[] terms, TermContext context) {
        FloatToken term = (FloatToken) terms[0];
        return BoolToken.of(term.bigFloatValue().isNaN());
    }

    public static Term maxValue(Term[] terms, TermContext context) {
        IntToken precision = (IntToken) terms[0];
        IntToken exponentBits = (IntToken) terms[1];
        BinaryMathContext mc = new BinaryMathContext(precision.intValue(), exponentBits.intValue());
        return FloatToken.of(BigFloat.maxValue(mc.precision, mc.maxExponent), exponentBits.intValue());
    }

    public static Term minValue(Term[] terms, TermContext context) {
        IntToken precision = (IntToken) terms[0];
        IntToken exponentBits = (IntToken) terms[1];
        BinaryMathContext mc = new BinaryMathContext(precision.intValue(), exponentBits.intValue());
        return FloatToken.of(BigFloat.minValue(mc.precision, mc.minExponent), exponentBits.intValue());
    }
}
