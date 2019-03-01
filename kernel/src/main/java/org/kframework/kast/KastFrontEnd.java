// Copyright (c) 2012-2019 K Team. All Rights Reserved.
package org.kframework.kast;

import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provider;
import org.kframework.attributes.Source;
import org.kframework.compile.AddSortInjections;
import org.kframework.compile.ExpandMacros;
import org.kframework.definition.Definition;
import org.kframework.definition.Rule;
import org.kframework.kompile.CompiledDefinition;
import org.kframework.kompile.DefinitionParsing;
import org.kframework.kore.K;
import org.kframework.main.FrontEnd;
import org.kframework.parser.concrete2kore.ParserUtils;
import org.kframework.parser.outer.Outer;
import org.kframework.unparser.KPrint;
import org.kframework.unparser.PrintOptions;
import org.kframework.unparser.ToKast;
import org.kframework.utils.errorsystem.KEMException;
import org.kframework.utils.errorsystem.KExceptionManager;
import org.kframework.utils.file.Environment;
import org.kframework.utils.file.FileUtil;
import org.kframework.utils.file.JarInfo;
import org.kframework.utils.file.KompiledDir;
import org.kframework.utils.file.TTYInfo;
import org.kframework.utils.inject.CommonModule;
import org.kframework.utils.inject.DefinitionScope;
import org.kframework.utils.inject.JCommanderModule;
import org.kframework.utils.inject.JCommanderModule.ExperimentalUsage;
import org.kframework.utils.inject.JCommanderModule.Usage;
import org.kframework.utils.Stopwatch;
import scala.Option;

import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Collections;

public class KastFrontEnd extends FrontEnd {

    public static List<Module> getModules() {
        List<Module> modules = new ArrayList<>();
        modules.add(new KastModule());
        modules.add(new JCommanderModule());
        modules.add(new CommonModule());
        return modules;
    }

    private final KastOptions options;
    private final Stopwatch sw;
    private final KExceptionManager kem;
    private final FileUtil files;
    private final Map<String, String> env;
    private final Provider<File> kompiledDir;
    private final Provider<CompiledDefinition> compiledDef;
    private final DefinitionScope scope;
    private final TTYInfo ttyInfo;

    @Inject
    KastFrontEnd(
            KastOptions options,
            @Usage String usage,
            @ExperimentalUsage String experimentalUsage,
            Stopwatch sw,
            KExceptionManager kem,
            JarInfo jarInfo,
            @Environment Map<String, String> env,
            FileUtil files,
            @KompiledDir Provider<File> kompiledDir,
            Provider<CompiledDefinition> compiledDef,
            DefinitionScope scope,
            TTYInfo ttyInfo) {
        super(kem, options.global, usage, experimentalUsage, jarInfo, files);
        this.options = options;
        this.sw = sw;
        this.kem = kem;
        this.files = files;
        this.env = env;
        this.kompiledDir = kompiledDir;
        this.compiledDef = compiledDef;
        this.scope = scope;
        this.ttyInfo = ttyInfo;
    }

    /**
     *
     * @return true if the application terminated normally; false otherwise
     */
    @Override
    public int run() {
        scope.enter(kompiledDir.get());
        try {
            Reader stringToParse = options.stringToParse();
            Source source = options.source();

            CompiledDefinition def = compiledDef.get();
            org.kframework.kore.Sort sort = options.sort;
            if (sort == null) {
                if (env.get("KRUN_SORT") != null) {
                    sort = Outer.parseSort(env.get("KRUN_SORT"));
                } else {
                    sort = def.programStartSymbol;
                }
            }
            org.kframework.definition.Module mod;
            org.kframework.definition.Module compiledMod;
            if (options.module == null) {
                mod = def.programParsingModuleFor(def.mainSyntaxModuleName(), kem).get();
                compiledMod = def.kompiledDefinition.getModule(def.mainSyntaxModuleName()).get();
            } else {
                Option<org.kframework.definition.Module> mod2 = def.programParsingModuleFor(options.module, kem);
                if (mod2.isEmpty()) {
                    throw KEMException.innerParserError("Module " + options.module + " not found. Specify a module with -m.");
                }
                mod = mod2.get();
                compiledMod = def.kompiledDefinition.getModule(options.module).get();
            }

            KPrint kprint = new KPrint(kem, files, ttyInfo, options.print, compiledDef.get().kompileOptions);
            if (options.parseWith.equals("program")) {
                K parsed = def.getParser(mod, sort, kem).apply(FileUtil.read(stringToParse), source);
                if (options.expandMacros) {
                    parsed = new ExpandMacros(compiledMod, files, def.kompileOptions, false).expand(parsed);
                }
                System.out.println(new String(kprint.prettyPrint(def, compiledMod, parsed), StandardCharsets.UTF_8));
                sw.printTotal("Total");

            } else if (options.parseWith.equals("configuration") || options.parseWith.equals("definition") || options.parseWith.equals("sentences")) {
                File cacheFile = def.kompileOptions.experimental.cacheFile != null
                               ? files.resolveWorkingDirectory(def.kompileOptions.experimental.cacheFile)
                               : files.resolveKompiled("cache.bin");
                ParserUtils parserUtils = new ParserUtils(files::resolveWorkingDirectory, kem, kem.options);
                DefinitionParsing definitionParsing = new DefinitionParsing(Collections.singletonList(new File(options.definitionLoading.directory)), false, kem, parserUtils, false, cacheFile, true, false);

                if (options.parseWith.equals("configuration")) {
                    Rule rule = definitionParsing.parseRule(def, FileUtil.read(stringToParse), source);
                    System.out.println(rule.toString());
                } else if (options.parseWith.equals("definition")) {
                    Definition parsed = definitionParsing.parseDefinitionAndResolveBubbles(new File("test.k"), "TEST-PARSING", "TEST-WASM", new HashSet<String>());
                    System.out.println(parsed.getModule("TEST-PARSING").get().toString());
                } else if (options.parseWith.equals("sentences")) {
                    System.out.println(definitionParsing.parseSentences(def, FileUtil.read(stringToParse), source));
                }

            } else {
                throw KEMException.innerParserError("Unrecognized parser: " + options.parseWith);
            }

            return 0;
        } finally {
            scope.exit();
        }
    }
}
