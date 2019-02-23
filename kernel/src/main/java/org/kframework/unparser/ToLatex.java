// Copyright (c) 2019 K Team. All Rights Reserved.
package org.kframework.unparser;

import org.kframework.kore.InjectedKLabel;
import org.kframework.kore.K;
import org.kframework.kore.KApply;
import org.kframework.kore.KAs;
import org.kframework.kore.KRewrite;
import org.kframework.kore.KSequence;
import org.kframework.kore.KToken;
import org.kframework.kore.KVariable;
import org.kframework.utils.errorsystem.KEMException;

import java.io.IOException;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;

import java.util.Optional;

/**
 * Writes a KAST term to the LaTeX format.
 */
public class ToLatex {

    public static byte[] apply(K k) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            apply(new DataOutputStream(out), k);
            return out.toByteArray();
        } catch (IOException e) {
            throw KEMException.criticalError("Could not write K term to LaTeX", e, k);
        }
    }

    public static void apply(DataOutputStream out, K k) throws IOException {
        if (k instanceof KToken) {
            KToken tok = (KToken) k;
            out.write(tok.s().getBytes());

        } else if (k instanceof KApply) {
            KApply app = (KApply) k;

            out.write(("\\" + app.klabel().name()).getBytes());

            for (K item : app.klist().asIterable()) {
                out.write("{".getBytes());
                ToLatex.apply(out, item);
                out.write("}".getBytes());
            }

        } else if (k instanceof KSequence) {
            KSequence kseq = (KSequence) k;

            out.write("KSequence unimplemented".getBytes());

        } else if (k instanceof KVariable) {
            KVariable var = (KVariable) k;

            Optional<String> origName = var.att().getOptional("originalName");
            if (origName.isPresent()) {
                out.write(origName.get().getBytes());
            } else {
                out.write(var.name().getBytes());
            }

        } else if (k instanceof KRewrite) {
            KRewrite rew = (KRewrite) k;

            out.write("KRewrite unimplemented".getBytes());

        } else if (k instanceof KAs) {
            KAs alias = (KAs) k;

            out.write("KAs unimplemented".getBytes());

        } else if (k instanceof InjectedKLabel) {
            KAs alias = (KAs) k;

            out.write("InjectedKLabel unimplemented".getBytes());

        }
    }
}
