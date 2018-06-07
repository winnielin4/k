// Copyright (c) 2013-2016 K Team. All Rights Reserved.
package org.kframework.backend.java.util;

import com.google.common.collect.ImmutableSet;
import com.sun.jna.Pointer;
import org.kframework.backend.java.z3.*;
import org.kframework.main.GlobalOptions;
import org.kframework.utils.OS;
import org.kframework.utils.errorsystem.KEMException;
import org.kframework.utils.errorsystem.KExceptionManager;
import org.kframework.utils.file.FileUtil;
import org.kframework.utils.options.SMTOptions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Set;

/**
 * @author Traian
 */
public class Z3Wrapper {

    private static final int Z3_RESTART_LIMIT = 3;

    private static final Set<String> Z3_QUERY_RESULTS = ImmutableSet.of("unknown", "sat", "unsat");

    public final String SMT_PRELUDE;
    private final SMTOptions options;
    private final GlobalOptions globalOptions;
    private final KExceptionManager kem;
    private final FileUtil files;

    public Z3Wrapper(
            SMTOptions options,
            KExceptionManager kem,
            GlobalOptions globalOptions,
            FileUtil files) {
        this.options = options;
        this.kem = kem;
        this.globalOptions = globalOptions;
        this.files = files;

        String defaultPrelude = "(set-option :auto-config false)\n(set-option :smt.mbqi false)\n"
                + "(declare-fun pow256          () Int)\n(assert (>= pow256           115792089237316195423570985008687907853269984665640564039457584007913129639936))\n(assert (<= pow256           115792089237316195423570985008687907853269984665640564039457584007913129639936))\n"
                + "(declare-fun pow255          () Int)\n(assert (>= pow255            57896044618658097711785492504343953926634992332820282019728792003956564819968))\n(assert (<= pow255            57896044618658097711785492504343953926634992332820282019728792003956564819968))\n"
                + "(declare-fun pow160          () Int)\n(assert (>= pow160                                        1461501637330902918203684832716283019655932542976))\n(assert (<= pow160                                        1461501637330902918203684832716283019655932542976))\n"
                + "(declare-fun minSInt128      () Int)\n(assert (>= minSInt128                                             -170141183460469231731687303715884105728))\n(assert (<= minSInt128                                             -170141183460469231731687303715884105728))\n"
                + "(declare-fun maxSInt128      () Int)\n(assert (>= maxSInt128                                              170141183460469231731687303715884105727))\n(assert (<= maxSInt128                                              170141183460469231731687303715884105727))\n"
                + "(declare-fun minSFixed128x10 () Int)\n(assert (>= minSFixed128x10                              -1701411834604692317316873037158841057280000000000))\n(assert (<= minSFixed128x10                              -1701411834604692317316873037158841057280000000000))\n"
                + "(declare-fun maxSFixed128x10 () Int)\n(assert (>= maxSFixed128x10                               1701411834604692317316873037158841057270000000000))\n(assert (<= maxSFixed128x10                               1701411834604692317316873037158841057270000000000))\n"
                + "(declare-fun minSInt256      () Int)\n(assert (>= minSInt256       -57896044618658097711785492504343953926634992332820282019728792003956564819968))\n(assert (<= minSInt256       -57896044618658097711785492504343953926634992332820282019728792003956564819968))\n"
                + "(declare-fun maxSInt256      () Int)\n(assert (>= maxSInt256        57896044618658097711785492504343953926634992332820282019728792003956564819967))\n(assert (<= maxSInt256        57896044618658097711785492504343953926634992332820282019728792003956564819967))\n"
                + "(declare-fun maxUInt128      () Int)\n(assert (>= maxUInt128                                              340282366920938463463374607431768211455))\n(assert (<= maxUInt128                                              340282366920938463463374607431768211455))\n"
                + "(declare-fun maxUFixed128x10 () Int)\n(assert (>= maxUFixed128x10                               3402823669209384634633746074317682114550000000000))\n(assert (<= maxUFixed128x10                               3402823669209384634633746074317682114550000000000))\n"
                + "(declare-fun maxUInt160      () Int)\n(assert (>= maxUInt160                                    1461501637330902918203684832716283019655932542975))\n(assert (<= maxUInt160                                    1461501637330902918203684832716283019655932542975))\n"
                + "(declare-fun maxUInt256      () Int)\n(assert (>= maxUInt256       115792089237316195423570985008687907853269984665640564039457584007913129639935))\n(assert (<= maxUInt256       115792089237316195423570985008687907853269984665640564039457584007913129639935))\n"
                ;

        SMT_PRELUDE = options.smtPrelude == null ? defaultPrelude : files.loadFromWorkingDirectory(options.smtPrelude);
    }

    public synchronized boolean isUnsat(String query, int timeout) {
        if (options.z3Executable) {
            return checkQueryWithExternalProcess(query, timeout);
        } else {
            return checkQueryWithLibrary(query, timeout);
        }
    }

    private boolean checkQueryWithLibrary(String query, int timeout) {
        boolean result = false;
        try (Z3Context context = new Z3Context()) {
            Z3Solver solver = new Z3Solver(context);
            Z3Params params = new Z3Params(context);
            params.add("timeout", timeout);
            solver.setParams(params);
            solver._assert(context.parseSmtlib2(SMT_PRELUDE + query));
            result = solver.check() == Z3Status.UNSAT;
        } catch (Z3Exception e) {
            kem.registerCriticalWarning(
                    "failed to translate smtlib expression:\n" + SMT_PRELUDE + query, e);
        } catch (UnsatisfiedLinkError e) {
            System.err.println(System.getProperty("java.library.path"));
            throw e;
        }
        return result;
    }

    private boolean checkQueryWithExternalProcess(String query, int timeout) {
        String result = "";
        try {
            for (int i = 0; i < Z3_RESTART_LIMIT; i++) {
                ProcessBuilder pb = files.getProcessBuilder().command(
                        OS.current().getNativeExecutable("z3"),
                        "-in",
                        "-smt2",
                        "-t:" + timeout);
                pb.redirectInput(ProcessBuilder.Redirect.PIPE);
                pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
                Process z3Process = pb.start();
                BufferedWriter input = new BufferedWriter(new OutputStreamWriter(
                    z3Process.getOutputStream()));
                BufferedReader output = new BufferedReader(new InputStreamReader(
                    z3Process.getInputStream()));
                input.write(SMT_PRELUDE + query + "(check-sat)\n");
                input.flush();
                result = output.readLine();
                z3Process.destroy();
                if (result != null) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!Z3_QUERY_RESULTS.contains(result)) {
            throw KEMException.criticalError("Z3 crashed on input query:\n" + query + "\nresult:\n" + result);
        }
        return "unsat".equals(result);
    }
}

