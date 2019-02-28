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

module TEST
  imports COMMONS
  imports K-IO
  imports LIST
  imports ID-SYNTAX
  
  syntax String ::= token2String ( K )    [function, functional, hook(STRING.token2string)]
  syntax Input
  syntax Stdout
  syntax Stderr
  syntax MetaK ::= parseError(Input, Stdout, Stderr)

  configuration <k> $PGM:Any </k>

  // take the input given as argument and run outer parsing to get the syntax AST
  rule S:Any => eP(#parseString("k-light2k5.sh --output meta-kast outer-k.k KDefinition", S), .K, .List)
  // use the list of productions to parse a string
  //rule <k> prods(_, L) => #parseWithProds(L, "Pgm", "int a; a = 1;") </k>

  // meta AST declarations
  syntax MetaKLabel
  syntax MetaValue
  syntax MetaKSort
  syntax MetaK ::= MetaKLabel | MetaValue | MetaKSort
                 | "#KApply" "(" K "," K ")" [klabel(#KApply)]
                 | "#KToken" "(" K "," K ")" [klabel(#KToken)]
                 | "#KList"  "(" K "," K ")" [klabel(#KList)]
                 | "#EmptyKList" "(" K ")"   [klabel(#EmptyKList)]

  // collect productions from anywhere in the AST and put them in a List
  syntax KItem ::= eP(K, K, List) // extractProduction
  syntax KItem ::= prods(K, List) // productions collector
  rule eP(`#KApply`(Tk, Kl),     Srt, L) => eP(Kl, Srt, L) [owise] // generic descent
  rule eP(`#KList`(T1, T2),      Srt, L) => eP(T1, Srt, L) ~> T2 // generically visiting every child
  rule eP(`#EmptyKList`(.KList), Srt, L) => prods(Srt, L)
  rule eP(`#KToken`(_, _),       Srt, L) => prods(Srt, L)
  rule prods(Srt, L) ~> T:KItem          => eP(T, Srt, L)

  rule eP(`#KApply`(#token("kSyntaxProduction","MetaKLabel"), `#KList`(Srt, Kl)), S, L) => eP(Kl, Srt, L)
  rule eP(`#KApply`(#token("kProductionWAttr", "MetaKLabel"), Kl), Srt, L) =>
       prods(Srt, L ListItem(`#KApply`(#token("kSyntaxProduction", "MetaKLabel"), `#KList`(Srt, `#KApply`(#token("kProductionWAttr",  "MetaKLabel"), Kl)))))

  // generate grammar
  syntax K ::= pp(K, String) // pretty print
  rule <k> prods(_, L) => pp(.K, "module TEMPPRETTYPRINT\n") ~> L ListItem("endmodule\n") </k>
  rule pp(.K, S) ~> ListItem(T) L => pp(T, S) ~> L
  rule pp(`#KList`(T1, T2), S) ~> L:List => pp(T1, S) ~> ListItem(T2) L // generically visiting every child
  rule pp(`#EmptyKList`(.KList), S) => pp(.K, S)
  rule pp(T:String, S) => pp(.K, S +String T) // closing matching element (for ] or endmodule)
  rule pp(`#KApply`(
            #token("kSyntaxProduction", "MetaKLabel"),
           `#KList`(`#KToken`(Srt, #token("KSort","MetaKSort")), `#KApply`(#token("kProductionWAttr",  "MetaKLabel"), Kl))), S) 
    => pp(Kl, S +String "  " +String "syntax " +String token2String(Srt) +String " ::= ")
  rule pp(`#KApply`(#token("regexTerminal","MetaKLabel"),`#KList`(`#KToken`(Tk, #token("KString","MetaKSort")),`#EmptyKList`(.KList))), S)
    => pp(.K, S +String "r" +String token2String(Tk) +String " ")
  rule pp(`#KApply`(#token("kAttributesDeclaration","MetaKLabel"), Kl), S) ~> L
    => pp(Kl, S +String "[") ~> ListItem("dummy]\n") L
  rule pp(`#KApply`(#token("kAttributesList","MetaKLabel"), Kl), S)
    => pp(Kl, S)
  rule pp(`#KApply`(#token("tagSimple","MetaKLabel"),`#KList`(`#KToken`(Tk,#token("KEY","MetaKSort")),`#EmptyKList`(.KList))), S)
    => pp(.K, S +String token2String(Tk) +String ", ")
  rule pp(`#KApply`(#token("tagContent","MetaKLabel"),
           `#KList`(
            `#KToken`(Key, #token("KEY","MetaKSort")),
            `#KList`(`#KToken`(Vl, #token("TAGList","MetaKSort")),`#EmptyKList`(.KList)))), S)
    => pp(.K, S +String token2String(Key) +String "(" +String token2String(Vl) +String ")" +String ", ")
  rule pp(`#KApply`(#token("nonTerminal","MetaKLabel"),`#KList`(`#KToken`(Tk,#token("KSort","MetaKSort")),`#EmptyKList`(.KList))), S)
    => pp(.K, S +String token2String(Tk) +String " ")
  rule pp(`#KApply`(#token("terminal","MetaKLabel"),`#KList`(`#KToken`(Tk, #token("KString","MetaKSort")),`#EmptyKList`(.KList))), S)
    => pp(.K, S +String token2String(Tk) +String " ")
  rule pp(`#KApply`(#token("kProduction","MetaKLabel"), Kl), S)
    => pp(Kl, S)
  rule pp(`#KApply`(#token("noKAttributesDeclaration","MetaKLabel"),`#EmptyKList`(.KList)), S)
    => pp(.K, S +String "\n")

  // pretty printing finished, time to write to file
  rule pp(.K, S) ~> .List
    => #open("tempPrettyPrint.k", "w") ~> S
  rule Fd:Int ~> S:String
    => #write(Fd, S) ~> #close(Fd) ~> #parseString("k-light2k5.sh --output meta-kast tempPrettyPrint.k Pgm", "int a; a = 1;")

endmodule

