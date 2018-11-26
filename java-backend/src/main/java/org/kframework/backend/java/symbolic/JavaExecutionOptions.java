// Copyright (c) 2014-2019 K Team. All Rights Reserved.
package org.kframework.backend.java.symbolic;

import com.beust.jcommander.Parameter;

import org.kframework.backend.java.util.StateLog;
import org.kframework.utils.inject.RequestScoped;
import org.kframework.utils.options.BaseEnumConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RequestScoped
public final class JavaExecutionOptions {

    @Parameter(names="--deterministic-functions", description="Throw assertion failure during "
        + "execution in the java backend if function definitions are not deterministic.")
    public boolean deterministicFunctions = false;

    @Parameter(names="--symbolic-execution", description="Use unification rather than "
        + "pattern matching to drive rewriting in the Java backend.")
    public boolean symbolicExecution = false;

    @Parameter(names="--audit-file", description="Enforce that the rule applied at the step specified by "
            + "--apply-step is a rule at the specified file and line, or fail with an error explaining why "
            + "the rule did not apply.")
    public String auditingFile;

    @Parameter(names="--audit-line", description="Enforce that the rule applied at the step specified by "
            + "--apply-step is a rule at the specified file and line, or fail with an error explaining why "
            + "the rule did not apply.")
    public Integer auditingLine;

    @Parameter(names="--audit-step", description="Enforce that the rule applied at the specified step is a rule "
            + "tagged with the javaBackendValue of --apply-tag, or fail with an error explaining why the rule did not apply.")
    public Integer auditingStep;

    @Parameter(names={"--state-log"}, description="Output symbolic execution debugging information")
    public boolean stateLog = false;

    @Parameter(names={"--state-log-path"}, description="Path where the debugging information should be stored")
    public String stateLogPath;

    @Parameter(names={"--state-log-id"}, description="Id of the current execution")
    public String stateLogId;

    @Parameter(names={"--state-log-events"}, converter=LogEventConverter.class, description="Comma-separated list of events to log: [OPEN|REACHINIT|REACHTARGET|REACHPROVED|NODE|RULE|SRULE|RULEATTEMPT|IMPLICATION|Z3QUERY|Z3RESULT|CLOSE]")
    public List<StateLog.LogEvent> stateLogEvents = Collections.emptyList();

    @Parameter(names="--cache-func", description="Cache evaluation results of pure functions. Enabled by default.", arity = 1)
    public boolean cacheFunctions = true;

    @Parameter(names="--cache-func-optimized",
            description="Clear function cache after initialization phase. Frees some memory. Use IN ADDITION to --cache-func")
    public boolean cacheFunctionsOptimized = false;

    @Parameter(names="--branching-allowed", arity=1, description="Number of branching events allowed before a forcible stop.")
    public int branchingAllowed = Integer.MAX_VALUE;

    @Parameter(names="--log", description="Log every step. KEVM only.")
    public boolean log = false;

    @Parameter(names="--log-stmts-only", description="Log only steps that execute a statement, without intermediary steps. " +
            "Except when intermediary steps are important for other reason, like branching. KEVM only.")
    public boolean logStmtsOnly = false;

    @Parameter(names="--log-basic",
            description="Log most basic information: summary of initial step, final steps and final implications." +
                    " All custom logging only works for KEVM-based specs.")
    public boolean logBasic = false;

    @Parameter(names="--log-cells", description="Specify what subset of configuration has to be printed when" +
            " an execution step is logged." +
            " Usage format: --log-pretty \"v2,v2,...\" , where v1,v2,... are either cell names," +
            " cell names in parentheses (like \"(k)\") or one of: \"(#pc)\", \"(#result)\". " +
            " The cells specified without parentheses are printed with toString()." +
            " The cells specified in parentheses are pretty-printed. Certain cells have custom formatting." +
            " The last 2 options mean:" +
            " (#pc) = pretty print the path condition (constraint)." +
            " (#result) = fully pretty-print the final result (e.g. the configuration for paths that were not proved)." +
            " This last option is very slow." +
            " Default value is:" +
            " --log-cells k,output,statusCode,localMem,pc,gas,wordStack,callData,accounts" +
            " Recommended alternative value:" +
            " --log-cells \"(k),output,statusCode,localMem,pc,gas,wordStack,callData,accounts,(#pc)\"")
    public List<String> logCells = Arrays.asList("k", "output", "statusCode", "localMem", "pc", "gas", "wordStack",
            "callData", "accounts");

    @Parameter(names = "--debug-steps", variableArity = true, description = "Specify exact steps for which --debug option should be enabled")
    public List<String> debugSteps = new ArrayList<>();

    @Parameter(names = "--debug-last-step", description = "Activate option --debug for last step. Useful to debug final implication.")
    public boolean debugLastStep = false;

    @Parameter(names="--debug-spec-rules", description="Enable --debug for steps where a specification rule is applied. " +
            "This may be useful because during spec rules new constraints are sometimes added to the path condition.")
    public boolean debugSpecRules = false;

    @Parameter(names="--log-rules", description="Log applied rules.")
    public boolean logRules = false;

    @Parameter(names="--debug-z3",
            description="Log formulae fed to z3 together with the rule that triggered them.")
    public boolean debugZ3 = false;

    @Parameter(names="--debug-z3-queries",
            description="Log actual z3 queries. Activates --debug-z3 automatically.")
    public boolean debugZ3Queries = false;

    @Parameter(names = "--halt-local-mem-non-map", description = "KEVM-specific. Halt when <localMem> cell at the end " +
            "of a step is not a map. useful debug option when memory model is a K builtin map. " +
            "Otherwise option should be false.")
    public boolean haltOnLocalMemNonMap = false;

    public boolean logRulesPublic = false;

    @Parameter(names="--log-target", description="Log target term.")
    public boolean logTarget = false;

    @Parameter(names="--cache-tostring",
            description="Cache toString() result for KItem, Equality and DisjunctiveFormula. " +
                    "Speeds up logging but eats more memory.", arity = 1)
    public boolean cacheToString = true;

    @Parameter(names = "--log-memory-after-gc",
            description = "In the summary box, in addition to printing regular used memory, " +
                    "also print used memory after System.gc(). Gives more precise information about memory usage.")
    public boolean logMemoryAfterGC = false;

    public static class LogEventConverter extends BaseEnumConverter<StateLog.LogEvent> {

        public LogEventConverter(String optionName) {
            super(optionName);
        }

        @Override
        public Class<StateLog.LogEvent> enumClass() {
            return StateLog.LogEvent.class;
        }
    }
}

