This definition contains a proposal of how `#parse` should work with the java-backend. 

The two most important hooked symbols are:

`#parseString ( <external path to parser>, <token to parse>)`
which takes as input an executable path to an external parser and any token
and returns a string representation of the AST as a K term.
Ex:

`#parseString("k-light2k5.sh outer-k.k KDefinition", "module TEST endmodule")`

will return:

```
kDefinition(
  emptyKRequireList(.KList),
  kModuleList(emptyKModuleList(.KList),
  kModule(#token("TEST","KModuleName"),
    noKAttributesDeclaration(.KList),
    emptyKImportList(.KList),
    emptyKSentenceList(.KList)))))
```
This implies that the parser called already knows how to recognise the language in the input.

The second variant is:

`#parseWithProds ( <list of productions>, <start symbol>, <token to parse> )`

Takes as input a list of productions from the above AST, a start symbol and a token to parse.
This will generate a parser at runtime starting from the productions sent as input.

The implementation can be found in the

`java-backend:org.kframework.backend.java.builtins.BuiltinIOOperations`

`k-light2k5.sh` is a script that calls the K-Light parser. This is the same parser used in K4,
but taken out of the K repository along with all the K references.
Note that the K5 parser is the same, but with some performance improvements. Most notably it
has a scanner (flex), but this also means that it is not scannerless, a property which
is needed when parsing rules as bubbles.

More about the definition:

Curently, the java-backend doesn't support meta-level operations, this is why
the transformation rules are written at object level. One issue that comes from this is that
KLabels and sorts of the parsed language need to be defined in order for the backend to function.
See module `KLABELS`.

Looking at the workflow of the front-end (importing grammars, rewriting in certain context) it
makes sense to have `rewrite in MODULENAME`, a feature found in Maude which would be very useful here.

Requirements:
https://github.com/radumereuta/k-light/tree/evenLighter
and k-light/bin to be in the PATH

Usage:

`kompile test.k --backend-java`

`krun imp.k --output kast`

