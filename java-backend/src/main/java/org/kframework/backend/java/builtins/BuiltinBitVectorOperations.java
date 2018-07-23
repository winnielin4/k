// Copyright (c) 2014-2018 K Team. All Rights Reserved.
package org.kframework.backend.java.builtins;

import org.kframework.backend.java.kil.BuiltinList;
import org.kframework.backend.java.kil.Term;
import org.kframework.backend.java.kil.TermContext;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Table of {@code public static} methods on builtin fixed precision integers.
 *
 * @author AndreiS
 */
@SuppressWarnings("unchecked")
public final class BuiltinBitVectorOperations {

    public static Term construct(Term[] terms, TermContext context) {
        IntToken bitwidth = (IntToken) terms[0];
        IntToken value = (IntToken) terms[1];
        try {
            return BitVector.of(value.bigIntegerValue(), bitwidth.intValue());
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Term bitwidth(Term[] terms, TermContext context) {
        Term term = terms[0];
        if (term instanceof BitVector) {
            return IntToken.of(((BitVector)term).bitwidth());
        } else {
            Integer bitwidth = BitVector.getBitwidth(term.att());
            if (bitwidth == null) {
                return null;
            }
            return IntToken.of(bitwidth);
        }
    }

    public static Term zero(Term[] terms, TermContext context) {
        BitVector term = (BitVector) terms[0];
        return BoolToken.of(term.isZero());
    }

    public static Term svalue(Term[] terms, TermContext context) {
        BitVector term = (BitVector) terms[0];
        return IntToken.of(term.signedValue());
    }

    public static Term uvalue(Term[] terms, TermContext context) {
        BitVector term = (BitVector) terms[0];
        return IntToken.of(term.unsignedValue());
    }

    public static Term add(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.add(term2);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term sub(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.sub(term2);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term mul(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.mul(term2);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term sadd(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.sadd(term2, context);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term uadd(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.uadd(term2, context);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term ssub(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.ssub(term2, context);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term usub(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.usub(term2, context);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term smul(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.smul(term2, context);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term umul(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.umul(term2, context);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term sdiv(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.sdiv(term2, context);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term udiv(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.udiv(term2);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term srem(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.srem(term2, context);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term urem(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.urem(term2);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term and(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.and(term2);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term or(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.or(term2);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term xor(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.xor(term2);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term shl(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        IntToken term2 = (IntToken) terms[1];
        return term1.shl(term2);
    }

    public static Term ashr(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        IntToken term2 = (IntToken) terms[1];
        return term1.ashr(term2);
    }

    public static Term lshr(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        IntToken term2 = (IntToken) terms[1];
        return term1.lshr(term2);
    }

    public static Term slt(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.slt(term2);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term ult(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.ult(term2);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term sle(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.sle(term2);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term ule(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.ule(term2);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term sgt(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.sgt(term2);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term ugt(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.ugt(term2);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term sge(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.sge(term2);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term uge(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.uge(term2);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term eq(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.eq(term2);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term ne(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        if (term1.bitwidth() == term2.bitwidth()) {
            return term1.ne(term2);
        } else {
            throw bitwidthMismatchException(term1, term2);
        }
    }

    public static Term concatenate(Term[] terms, TermContext context) {
        BitVector term1 = (BitVector) terms[0];
        BitVector term2 = (BitVector) terms[1];
        return term1.concatenate(term2);
    }

    public static Term extract(Term[] terms, TermContext context) {
        BitVector term = (BitVector) terms[0];
        IntToken beginIndex = (IntToken) terms[1];
        IntToken endIndex = (IntToken) terms[2];
        return term.extract(beginIndex.intValue(), endIndex.intValue());
    }

    public static Term toDigits(Term[] terms, TermContext context) {
        BitVector term = (BitVector) terms[0];
        IntToken bitwidth = (IntToken) terms[1];
        IntToken count = (IntToken) terms[2];
        if (bitwidth.intValue() > 0 && bitwidth.intValue() * count.intValue() <= term.bitwidth) {
            List<Term> digits = ((List<BitVector>) term.toDigits(bitwidth.intValue(), count.intValue())).stream()
                    .map(t -> BuiltinListOperations.wrapListItem(t, context))
                    .collect(Collectors.toList());
            return BuiltinList.builder(context.global())
                    .addAll(digits)
                    .build();
        } else {
            return null;
        }
    }

    public static Term fromDigits(Term[] terms, TermContext context) {
        BuiltinList digitList = (BuiltinList) terms[0];
        if (digitList.isGround()) {
            List<BitVector> digits;
            try {
                // AndreiS: double cast because Java in its infinite wisdom does not allow to cast
                // List<Term> to List<BitVector>
                digits = (List<BitVector>) ((List) digitList.children);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException(digitList + " is not a list of bitvectors");
            }
            return BitVector.fromDigits(digits);
        } else {
            //throw new IllegalArgumentException(digitList + " contains list variables");
            return null;
        }
    }

    /**
     * Returns {@link IllegalArgumentException} containing the bit width mismatch details.
     */
    private static IllegalArgumentException bitwidthMismatchException(
            BitVector term1,
            BitVector term2) {
        return new IllegalArgumentException(
                "mismatch bit width: "
                + "first argument is represented on " + term1.bitwidth() + " bits "
                + "while second argument is represented on " + term2.bitwidth() + "bits");
    }

}
