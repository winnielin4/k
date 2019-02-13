// Copyright (c) 2013-2019 K Team. All Rights Reserved.
// Author: Radu Mereuta

module COMMONS
  syntax Any

endmodule

module TEST-SYNTAX
  imports COMMONS
  // parse anything sent to the input as a single token of sort Any
  syntax Any ::= r"([\\n\\r]|.)*" [token]
endmodule

module KLABELS
  // KLabels and sorts of the parsed language need to be defined in order for the backend to function
  syntax KDefinition   ::= KRequireList KModuleList [klabel(kDefinition)]
  syntax KRequireList
  syntax OptionalAttributes
  syntax KImportList
  syntax KSentenceList
  syntax KModuleName
  syntax KModule       ::= "kmodule" KModuleName OptionalAttributes
                                    KImportList
                                    KSentenceList
                           "endkmodule"
                               [klabel(kModule)]
  syntax KModuleList   ::= "emptyDummy"  [klabel(emptyKModuleList)]
                         | KModuleList KModule [klabel(kModuleList), unit(emptyKModuleList)]

  syntax KSentenceList ::= "emptyDummy"  [klabel(emptyKSentenceList)]
                         | KSentenceList KSentence [klabel(kSentenceList), unit(kemptyKSentenceList)]

  syntax KSort
  syntax KSentence ::= "ksyntax" KSort OptionalAttributes [klabel(kSyntaxSort)]
                     | "ksyntax" KSort "::=" PrioritySeqBlock [klabel(kSyntaxProduction)]

  syntax PrioritySeqBlock ::= PrioritySeqBlock ">" AssocAttribute ProdBlock [klabel(prioritySeqBlock)]
                            | ProdBlock
  syntax AssocAttribute
  syntax KProduction
  syntax KProductionWAttr ::= KProduction OptionalAttributes [klabel(kProductionWAttr)]
  syntax ProdBlock ::= ProdBlock "|" KProductionWAttr [klabel(prodBlock)]
                     | KProductionWAttr
  syntax KSentence ::= "kconfiguration" Contents [klabel(kConfiguration)]
                     | "krule"    Contents [klabel(kRule)]
                     | "kcontext" Contents [klabel(kContext)]
  syntax Contents
  syntax KItem ::= parseError(Input, Stdout, Stderr) [klabel(parseError)]
  syntax Input
  syntax Stdout
  syntax Stderr
  //// IMP specific
  syntax Id
  syntax Start ::= "int" Id ";" Id [klabel(start)]
  syntax Start ::= Id "=" Id ";" [klabel(asgn)]

endmodule

module TEST
  imports COMMONS
  imports K-IO
  import KLABELS

  configuration
	<k> $PGM:Any </k>
	<store> .K </store>
	<sort> .K </sort>

  // take the input given as argument and run outer parsing to get the syntax AST
  rule S:Any => #parseString("k-light2k5.sh outer-k.k KDefinition", S)
  // use the list of productions to parse a string
  rule <k> .K => #parseWithProds(P, "Pgm", "int a; a = 1;") </k> <store> P => .K </store>

  // go through the AST and collect all the productions
  rule kDefinition(_, M) => M
  rule kModuleList(ML, M) => ML ~> M
  rule emptyKModuleList(.KList) => .K
  rule kModule(_, _, _, S) => S
  rule kSentenceList(SL, S) => SL ~> S
  rule emptyKSentenceList(.KList) => .K
  rule <k> kSyntaxProduction(S, Pi) => Pi ...</k>
       <sort> _ => S </sort>
  rule prioritySeqBlock(P1, _, P2) => P1 ~> P2
  rule prodBlock(P1, P2) => P1 ~> P2
  rule <k> kProductionWAttr(P, At) => .K ...</k>
       <store>... .K => kSyntaxProduction(S, kProductionWAttr(P, At)) </store>
       <sort> S </sort>
  rule kConfiguration(_) => .K
  rule kRule(_) => .K
  rule kContext(_) => .K

endmodule

