// Copyright (c) 2015-2019 K Team. All Rights Reserved.
package org.kframework.kprove;

import com.google.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.kframework.builtin.BooleanUtils;
import org.kframework.builtin.Sorts;
import org.kframework.compile.*;
import org.kframework.definition.*;
import org.kframework.definition.Module;
import org.kframework.kompile.CompiledDefinition;
import org.kframework.kompile.Kompile;
import org.kframework.kore.K;
import org.kframework.kore.KApply;
import org.kframework.kore.KRewrite;
import org.kframework.kore.KToken;
import org.kframework.kore.KVariable;
import org.kframework.kore.Sort;
import org.kframework.kore.VisitK;
import org.kframework.kore.TransformK;
import org.kframework.rewriter.Rewriter;
import org.kframework.unparser.KPrint;
import org.kframework.utils.Stopwatch;
import org.kframework.utils.errorsystem.KEMException;
import org.kframework.utils.errorsystem.KExceptionManager;
import org.kframework.utils.file.FileUtil;
import scala.Option;
import scala.Tuple2;
import scala.collection.JavaConverters;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Function;

import static org.kframework.Collections.*;
import static org.kframework.kore.KORE.KToken;


/**
 * Class that implements the "--prove" option.
 */
public class KProve {

    private final KExceptionManager kem;
    private final Stopwatch sw;
    private final FileUtil files;
    private final KPrint kprint;

    @Inject
    public KProve(KExceptionManager kem, Stopwatch sw, FileUtil files, KPrint kprint) {
        this.kem    = kem;
        this.sw     = sw;
        this.files  = files;
        this.kprint = kprint;
    }

    public int run(KProveOptions options, CompiledDefinition compiledDefinition, Backend backend, Function<Definition, Rewriter> rewriterGenerator) {
        Tuple2<Definition, Module> compiled = getProofDefinition(options.specFile(files), options.defModule, options.specModule, compiledDefinition, backend, files, kem, sw);
        Rewriter rewriter = rewriterGenerator.apply(compiled._1());
        Module specModule = compiled._2();

        if (options.concretizeSorts.size() > 0 && options.concreteInstances > 0) {
            specModule = this.concretizeSpecs(specModule, options.concretizeSorts, options.concreteInstances);
        }

        K results = rewriter.prove(specModule);
        int exit;
        if (results instanceof KApply) {
            KApply kapp = (KApply) results;
            exit = kapp.klabel().name().equals("#True") ? 0 : 1;
        } else {
            exit = 1;
        }
        kprint.prettyPrint(compiled._1(), compiled._1().getModule("LANGUAGE-PARSING").get(), s -> kprint.outputFile(s), results);
        return exit;
    }

    private static Module getModule(String defModule, Map<String, Module> modules, Definition oldDef) {
        if (modules.containsKey(defModule))
            return modules.get(defModule);
        Option<Module> mod = oldDef.getModule(defModule);
        if (mod.isDefined()) {
            return mod.get();
        }
        throw KEMException.criticalError("Module " + defModule + " does not exist.");
    }

    public static Map<Definition, Definition> cache = Collections.synchronizedMap(new LinkedHashMap<Definition, Definition>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry entry) {
            return size() > 10;
        }
    });

    public static Tuple2<Definition, Module> getProofDefinition(File proofFile, String defModuleName, String specModuleName, CompiledDefinition compiledDefinition, Backend backend, FileUtil files, KExceptionManager kem, Stopwatch sw) {
        Kompile kompile = new Kompile(compiledDefinition.kompileOptions, files, kem, sw, true);
        if (defModuleName == null) {
            defModuleName = compiledDefinition.kompiledDefinition.mainModule().name();
        }
        if (specModuleName == null) {
            specModuleName = FilenameUtils.getBaseName(proofFile.getName()).toUpperCase();
        }
        java.util.Set<Module> modules = kompile.parseModules(compiledDefinition, defModuleName, files.resolveWorkingDirectory(proofFile).getAbsoluteFile());
        Map<String, Module> modulesMap = new HashMap<>();
        modules.forEach(m -> modulesMap.put(m.name(), m));
        Module defModule = getModule(defModuleName, modulesMap, compiledDefinition.getParsedDefinition());
        Module specModule = getModule(specModuleName, modulesMap, compiledDefinition.getParsedDefinition());
        specModule = backend.specificationSteps(compiledDefinition.kompiledDefinition).apply(specModule);
        specModule = spliceModule(specModule, compiledDefinition.kompiledDefinition);
        Definition combinedDef = Definition.apply(defModule, compiledDefinition.getParsedDefinition().entryModules(), compiledDefinition.getParsedDefinition().att());
        Definition compiled = compileDefinition(backend, combinedDef);
        return Tuple2.apply(compiled, specModule);
    }

    private static Definition compileDefinition(Backend backend, Definition combinedDef) {
        Definition compiled = cache.get(combinedDef);
        if (compiled == null) {
            compiled = backend.steps().apply(combinedDef);
            cache.put(combinedDef, compiled);
        }
        return compiled;
    }

    private static Module spliceModule(Module specModule, Definition kompiledDefinition) {
        return ModuleTransformer.from(mod -> kompiledDefinition.getModule(mod.name()).isDefined() ? kompiledDefinition.getModule(mod.name()).get() : mod, "splice imports of specification module").apply(specModule);
    }

    private static Module concretizeSpecs(Module specModule, List<String> concretizeSorts, int concreteInstances) {
        Set<Sentence> newSentences = new HashSet<Sentence>();
        for (Sentence sent: JavaConverters.seqAsJavaList(specModule.localSentences().toSeq())) {
            if (! (sent instanceof Rule)) {
                newSentences.add(sent);
            } else {
                for (Rule rule: concretizeRule((Rule) sent, concretizeSorts, concreteInstances)) {
                    newSentences.add(rule);
                }
            }
        }

        return new Module(specModule.name(), specModule.imports(), JavaConverters.asScalaSet(newSentences), specModule.att());
    }

    private static final Random randomNumber = new Random();

    private static Set<Rule> concretizeRule(Rule rule, List<String> concretizeSorts, int concretizeInstances) {
        Set<Rule> newRules = new HashSet<Rule>();
        for (int i = 0; i < concretizeInstances; i++) {
            HashMap<KVariable, K> ruleSubstitution = new HashMap<KVariable, K>();
            for (String sortName: concretizeSorts) {
                if (! (sortName.equals("Int") || sortName.equals("Bool"))) {
                    throw KEMException.criticalError("Unsupported sort for concretization. Supported sorts are [Int|Bool], found: " + sortName);
                }
                for (KVariable var: getLHSVariablesOfSort(rule.body(), sortName)) {
                    if      (sortName.equals("Int"))  ruleSubstitution.put(var, KToken(Integer.toString(randomNumber.nextInt(10)), Sorts.Int()));
                    else if (sortName.equals("Bool")) ruleSubstitution.put(var, randomNumber.nextInt(2) == 0 ? BooleanUtils.FALSE : BooleanUtils.TRUE);
                }
            }
            newRules.add(applySubstitution(rule, ruleSubstitution));
        }
        return newRules;
    }

    private static Rule applySubstitution(Rule rule, Map<KVariable, K> subst) {
        TransformK substAll = new TransformK() {
            @Override
            public K apply(KVariable kvar) {
                if (subst.containsKey(kvar)) {
                    return subst.get(kvar);
                } else {
                    return kvar;
                }
            }
        };
        return new Rule(substAll.apply(rule.body()), substAll.apply(rule.requires()), substAll.apply(rule.ensures()), rule.att()); 
    }

    private static Set<KVariable> getLHSVariablesOfSort(K term, String sortName) {
        Set<KVariable> vars = new HashSet<KVariable>();
        new VisitK() {
            @Override
            public void apply(KRewrite krw) {
                super.apply(krw.left());
            }

            @Override
            public void apply(KVariable v) {
                if (v.att().get(Sort.class).name().equals(sortName)) {
                    vars.add(v);
                }
            }
        }.apply(term);
        return vars;
    }
}
