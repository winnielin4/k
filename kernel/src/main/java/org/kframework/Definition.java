// Copyright (c) 2015-2019 K Team. All Rights Reserved.
package org.kframework;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.inject.util.Providers;
import org.kframework.attributes.Source;
import org.kframework.kompile.DefinitionParsing;
import org.kframework.kompile.Kompile;
import org.kframework.main.GlobalOptions;
import org.kframework.parser.concrete2kore.ParserUtils;
import org.kframework.utils.errorsystem.KExceptionManager;
import org.kframework.utils.file.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@API
public class Definition {

    /**
     * Parses the text to create a {@link Definition} object.
     * The main module of the definition will be last module defined in the text file.
     */
    public static org.kframework.definition.Definition from(String definitionText) {
        Pattern pattern = Pattern.compile("(?:^|\\s)module ([A-Z][A-Z\\-]*)");
        Matcher m = pattern.matcher(definitionText);
        if(!m.find()) {
            throw new RuntimeException("Could not find any module in the definition");
        }
        String nameOfLastModule = m.group(m.groupCount());
        return from(definitionText, nameOfLastModule, Source.apply("generated"));
    }

    /**
     * Parses the text to create a {@link Definition} object.
     */
    public static org.kframework.definition.Definition from(String definitionText, String mainModuleName) {
        return from(definitionText, mainModuleName, Source.apply("generated"));
    }

    /**
     * Parses the text to create a {@link Definition} object.
     */
    public static org.kframework.definition.Definition from(String definitionText, String mainModuleName, Source source) {
        return from(definitionText, mainModuleName, source, Lists.newArrayList(Kompile.BUILTIN_DIRECTORY));
    }

    /**
     * Parses the text to create a {@link Definition} object.
     */
    public static org.kframework.definition.Definition from(String definitionText, String mainModuleName, Source source, List<File> lookupDirectories) {
        File tempDir = Files.createTempDir();
        File theFileUtilTempDir = new File(tempDir.getAbsolutePath() + File.pathSeparator + "tempDir");
        File definitionDir = new File(tempDir.getAbsolutePath() + File.pathSeparator + "definitionDir");
        File workingDir = new File(tempDir.getAbsolutePath() + File.pathSeparator + "workingDir");
        File kompiledDir = new File(tempDir.getAbsolutePath() + File.pathSeparator + "kompiledDir");
        if(!theFileUtilTempDir.mkdir() || !definitionDir.mkdir() || !workingDir.mkdir() || !kompiledDir.mkdir()) {
            throw new AssertionError("Could not create one of the temporary directories");
        }
        GlobalOptions globalOptions = new GlobalOptions();
        KExceptionManager kem = new KExceptionManager(globalOptions);

        FileUtil fileUtil = new FileUtil(theFileUtilTempDir,
                Providers.of(definitionDir),
                workingDir,
                Providers.of(kompiledDir),
                globalOptions,
                System.getenv());
        ParserUtils parserUtils = new ParserUtils(fileUtil::resolveWorkingDirectory, kem, globalOptions);

        org.kframework.definition.Definition definition = parserUtils.loadDefinition(
                mainModuleName,
                mainModuleName,
                definitionText,
                source,
                workingDir,
                lookupDirectories,
                false,
                false
        );

        return definition;
    }

    /**
     * Load and parse the bubbles of a definition
     * @param filePath
     * @return
     */
    public static org.kframework.definition.Definition from(File filePath) {
        String definitionText = FileUtil.load(filePath);
        Pattern pattern = Pattern.compile("(?:^|\\s)module ([A-Z][A-Z\\-]*)");
        Matcher m = pattern.matcher(definitionText);
        String mainModuleName = null;
        while (m.find())
            mainModuleName = m.group(m.groupCount());
        if(mainModuleName == null) {
            throw new RuntimeException("Could not find any module in the definition");
        }

        File tempDir = Files.createTempDir();
        File theFileUtilTempDir = new File(tempDir.getAbsolutePath() + File.pathSeparator + "tempDir");
        File definitionDir = new File(tempDir.getAbsolutePath() + File.pathSeparator + "definitionDir");
        File workingDir = new File(tempDir.getAbsolutePath() + File.pathSeparator + "workingDir");
        File kompiledDir = new File(tempDir.getAbsolutePath() + File.pathSeparator + "kompiledDir");
        ArrayList<File> lookupDirectories = Lists.newArrayList(Kompile.BUILTIN_DIRECTORY);
        if(!theFileUtilTempDir.mkdir() || !definitionDir.mkdir() || !workingDir.mkdir() || !kompiledDir.mkdir()) {
            throw new AssertionError("Could not create one of the temporary directories");
        }
        GlobalOptions globalOptions = new GlobalOptions();
        KExceptionManager kem = new KExceptionManager(globalOptions);

        FileUtil fileUtil = new FileUtil(theFileUtilTempDir,
                Providers.of(definitionDir),
                workingDir,
                Providers.of(kompiledDir),
                globalOptions,
                System.getenv());
        ParserUtils parserUtils = new ParserUtils(fileUtil::resolveWorkingDirectory, kem, globalOptions);

        DefinitionParsing definitionParsing = new DefinitionParsing(
                lookupDirectories,
                true,
                kem,
                parserUtils,
                true,
                fileUtil.resolveKompiled("cache.bin"),
                true,
                false);

        org.kframework.definition.Definition def = definitionParsing.parseDefinitionAndResolveBubbles(filePath, mainModuleName, mainModuleName);
        return def;
    }
}
