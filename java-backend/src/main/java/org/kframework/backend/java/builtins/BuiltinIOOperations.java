// Copyright (c) 2013-2019 K Team. All Rights Reserved.
package org.kframework.backend.java.builtins;

import org.kframework.attributes.Source;
import org.kframework.backend.java.kil.*;
import org.kframework.kore.KORE;
import org.kframework.kore.KToken;
import org.kframework.kore.TransformK;
import org.kframework.krun.RunProcess;
import org.kframework.krun.RunProcess.ProcessOutput;
import org.kframework.krun.api.io.FileSystem;
import org.kframework.parser.kore.KoreParser;
import org.kframework.utils.errorsystem.KException;
import org.kframework.utils.errorsystem.ParseFailedException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.util.*;

import static org.kframework.Collections.stream;


/**
 * Table of {@code public static} methods for builtin IO operations.
 */
public class BuiltinIOOperations {

    public static Term getTime(TermContext termContext) {
        return IntToken.of(System.currentTimeMillis());
    }

    public static Term open(StringToken term1, StringToken term2, TermContext termContext) {
        FileSystem fs = termContext.fileSystem();
        try {
            return IntToken.of(fs.open(term1.stringValue(), term2.stringValue()));
        } catch (IOException e) {
            return processIOException(e.getMessage(), termContext);
        }
    }

    public static Term tell(IntToken term, TermContext termContext) {
        FileSystem fs = termContext.fileSystem();
        try {
            return IntToken.of(fs.get(term.longValue()).tell());
        } catch (IOException e) {
            return processIOException(e.getMessage(), termContext);
        }
    }

    public static Term getc(IntToken term, TermContext termContext) {
        FileSystem fs = termContext.fileSystem();
        try {
            return IntToken.of(fs.get(term.longValue()).getc() & 0xff);
        } catch (IOException e) {
            return processIOException(e.getMessage(), termContext);
        }
    }

    public static Term read(IntToken term1, IntToken term2, TermContext termContext) {
        FileSystem fs = termContext.fileSystem();
        try {
            return StringToken.of(fs.get(term1.longValue()).read(term2.intValue()));
        } catch (IOException e) {
            return processIOException(e.getMessage(), termContext);
        }
    }

    public static Term close(IntToken term, TermContext termContext) {
        FileSystem fs = termContext.fileSystem();
        try {
            fs.close(term.longValue());
            return BuiltinList.kSequenceBuilder(termContext.global()).build();
        } catch (IOException e) {
            return processIOException(e.getMessage(), termContext);
        }
    }

    public static Term seek(IntToken term1, IntToken term2, TermContext termContext) {
        FileSystem fs = termContext.fileSystem();
        try {
            fs.get(term1.longValue()).seek(term2.longValue());
            return BuiltinList.kSequenceBuilder(termContext.global()).build();
        } catch (IOException e) {
            return processIOException(e.getMessage(), termContext);
        }
    }

    public static Term putc(IntToken term1, IntToken term2, TermContext termContext) {
        FileSystem fs = termContext.fileSystem();
        try {
            fs.get(term1.longValue()).putc(term2.unsignedByteValue());
            return BuiltinList.kSequenceBuilder(termContext.global()).build();
        } catch (IOException e) {
            return processIOException(e.getMessage(), termContext);
        }
    }
    public static Term write(IntToken term1, StringToken term2, TermContext termContext) {
        FileSystem fs = termContext.fileSystem();
        try {
            fs.get(term1.longValue()).write(term2.byteArrayValue());
            return BuiltinList.kSequenceBuilder(termContext.global()).build();
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            return processIOException(e.getMessage(), termContext);
        }
    }

    /**
     * Execute path and gives input as an argument.
     * No whitespaces allowed in path.
     * Example `cat file` or `echo string` or whatever external parser.
     */
    public static Term parse(StringToken path, StringToken input, TermContext termContext) {
        List<String> tokens = new ArrayList<>(Arrays.asList(path.stringValue().split(" ")));
        String tempF = "tempRuntimeParser.txt";
        termContext.global().files.saveToTemp(tempF, input.stringValue());
        tokens.add(termContext.global().files.resolveTemp(tempF).getAbsolutePath());
        Map<String, String> environment = new HashMap<>();
        RunProcess.ProcessOutput output = RunProcess.execute(environment, new ProcessBuilder().directory(new File(".")), tokens.toArray(new String[tokens.size()]));

        if (output.exitCode != 0) {
            throw new ParseFailedException(new KException(KException.ExceptionType.ERROR, KException.KExceptionGroup.CRITICAL, "Parser returned a non-zero exit code: "
                    + output.exitCode + "\nStdout:\n" + new String(output.stdout) + "\nStderr:\n" + new String(output.stderr)));
        }

        byte[] kast = output.stdout != null ? output.stdout : new byte[0];
        return termContext.getKOREtoBackendKILConverter().convert(KoreParser.parse(new String(kast), termContext.getSource()));
    }

    /**
     * Execute path and parse the file represented by input. See above.
     */
    public static Term parseFile(StringToken path, Token input, TermContext termContext) {
        List<String> tokens = new ArrayList<>(Arrays.asList(path.stringValue().split(" ")));
        tokens.add(input.s());
        Map<String, String> environment = new HashMap<>();
        RunProcess.ProcessOutput output = RunProcess.execute(environment, new ProcessBuilder().directory(new File(".")), tokens.toArray(new String[tokens.size()]));

        if (output.exitCode != 0) {
            return termContext.getKOREtoBackendKILConverter().convert(KoreParser.parse(
                    "parseError(" +
                            "#token(" + StringUtil.enquoteCString(input.s()) + ", \"Input\"), " +
                            "#token(" + StringUtil.enquoteCString(new String(output.stdout)) + ", \"Stdout\"), " +
                            "#token(" + StringUtil.enquoteCString(new String(output.stderr)) + ", \"Stderr\"))"
                    , termContext.getSource()));
        }

        byte[] kast = output.stdout != null ? output.stdout : new byte[0];
        return termContext.getKOREtoBackendKILConverter().convert(KoreParser.parse(new String(kast), termContext.getSource()));
    }

    public static Term loadDefinition(StringToken path, Token input, TermContext termContext) {
        List<String> tokens = new ArrayList<>(Arrays.asList(path.stringValue().split(" ")));
        String tempF = "tempRuntimeParser.txt";
        termContext.global().files.saveToTemp(tempF, input.s());
        String filePath = termContext.global().files.resolveTemp(tempF).getAbsolutePath();
        tokens.add(filePath);
        Map<String, String> environment = new HashMap<>();
        RunProcess.ProcessOutput output = RunProcess.execute(environment, new ProcessBuilder().directory(new File(".")), tokens.toArray(new String[tokens.size()]));

        if (output.exitCode != 0) {
            return termContext.getKOREtoBackendKILConverter().convert(KoreParser.parse(
                    "parseError(" +
                            "#token(" + StringUtil.enquoteCString(input.s()) + ", \"Input\"), " +
                            "#token(" + StringUtil.enquoteCString(new String(output.stdout)) + ", \"Stdout\"), " +
                            "#token(" + StringUtil.enquoteCString(new String(output.stderr)) + ", \"Stderr\"))"
                    , termContext.getSource()));
        }

        byte[] kast = output.stdout != null ? output.stdout : new byte[0];
        K kore = KoreParser.parse(new String(kast), termContext.getSource());

        // parse bubbles
        Definition def = org.kframework.Definition.from(new File(filePath));
        // collect the bubbles from the java pipeline
        // TODO: find a way to keep the configuration since it desuggars into syntax decl
        Map<String, String> ss = stream(def.modules())
                .flatMap(m -> stream(m.localSentences()))
                .filter(s -> (s instanceof Rule || s instanceof Configuration) && s.att().contains("originalBubble") && s.att().contains("kastBubble"))
                .collect(Collectors.toMap(s1 -> s1.att().get("originalBubble"), s2 -> s2.att().get("kastBubble")));

        // visit the result of outer parsing and replace bubbles with parses from the java pipeline

        K body = new TransformK() {
            @Override
            public K apply(KToken token) {
                if (!token.sort().name().equals("Bubble"))
                    return token;
                for (String s : ss.keySet())
                    if (token.s().startsWith(s))
                        return KoreParser.parse(ss.get(s), termContext.getSource());
                return token;
            }
        }.apply(kore);

        Term rez = termContext.getKOREtoBackendKILConverter().convert(body);
        return rez;
    }

    public static Term parseInModule(StringToken input, StringToken startSymbol, StringToken moduleName, TermContext termContext) {
        throw new RuntimeException("Not implemented!");
    }

    public static Term system(StringToken term, TermContext termContext) {
        Map<String, String> environment = new HashMap<>();
        String[] args = term.stringValue().split("\001", -1);
        //for (String c : args) { System.out.println(c); }
        ProcessOutput output = RunProcess.execute(environment, termContext.global().files.getProcessBuilder(), args);

        KLabelConstant klabel = KLabelConstant.of(KORE.KLabel("#systemResult(_,_,_)"), termContext.definition());
        /*
        String klabelString = "#systemResult(_,_,_)";
        KLabelConstant klabel = KLabelConstant.of(klabelString, context);
        assert def.kLabels().contains(klabel) : "No KLabel in definition for " + klabelString;
        */
        String stdout = output.stdout != null ? new String(output.stdout) : "";
        String stderr = output.stderr != null ? new String(output.stderr) : "";
        return KItem.of(klabel, KList.concatenate(IntToken.of(output.exitCode),
            StringToken.of(stdout.trim()), StringToken.of(stderr.trim())), termContext.global());
    }

    private static KItem processIOException(String errno, Term klist, TermContext termContext) {
        String klabelString = "#" + errno + "_K-IO";
        KLabelConstant klabel = KLabelConstant.of(KORE.KLabel(klabelString), termContext.definition());
        assert termContext.definition().kLabels().contains(klabel) : "No KLabel in definition for errno '" + errno + "'";
        return KItem.of(klabel, klist, termContext.global());
    }

    private static KItem processIOException(String errno, TermContext termContext) {
        return processIOException(errno, KList.EMPTY, termContext);
    }
}
