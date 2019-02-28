This definition contains a proposal of how `#parse` should work with the java-backend. 

`#parseString ( <external path to parser>, <token to parse>)`
which takes as input an executable path to an external parser and any token
and returns a string representation of the AST as a K term.
Ex:
`#parseString("k-light2k5.sh --output meta-kast outer-k.k KDefinition", "module TEST endmodule")`
will return:
```
#KApply(#token("kDefinition", "MetaKLabel"), #KList(
  #KApply(#token("emptyKRequireList", "MetaKLabel"), #EmptyKList(.KList)),#KList(#KApply(#token("kModuleList", "MetaKLabel"), #KList(#KApply(#token("emptyKModuleList", "MetaKLabel"), #EmptyKList(.KList)),#KList(
   #KApply(#token("kModule", "MetaKLabel"), #KList(#KToken(#token("TEST", "MetaValue"), #token("KModuleName", "MetaKSort")),#KList(#KApply(#token("noKAttributesDeclaration", "MetaKLabel"), #EmptyKList(.KList)),#KList(
   #KApply(#token("emptyKImportList", "MetaKLabel"), #EmptyKList(.KList)),#KList(
   #KApply(#token("emptyKSentenceList", "MetaKLabel"), #EmptyKList(.KList)),#EmptyKList(.KList)))))),#EmptyKList(.KList)))),#EmptyKList(.KList))))
```

The implementation can be found in the `java-backend:org.kframework.backend.java.builtins.BuiltinIOOperations`

`k-light2k5.sh` is a script that calls the K-Light parser. This is the same parser used in K4,
but taken out of the K repository along with all the K references.
Note that the K5 parser is the same, but with some performance improvements. Most notably it
has a scanner (flex), but this also means that it is not scannerless, a property which
is needed when parsing rules as bubbles.

Requirements:
https://github.com/radumereuta/k-light/tree/evenLighter
and k-light/bin to be in the PATH

Usage:
`kompile test.k --backend-java` (sometimes `-v --debug` can be useful)
`krun imp.k` (sometimes `--output kast` can be useful)

