// TODO: delete this!
grammar Query;

propositionUnit
  : proposition EOF
  ;

proposition
  : '(' proposition ')' # ParensProp
  | 'true' # TrueProp
  | 'false' # FalseProp
  | ID # IDProp
  | '!' ID # NotIDProp
  | <assoc=right> proposition ('&&' proposition)+ # ConjunctionProp
  | <assoc=right> proposition ('||' proposition)+ # DisjunctionProp
  | '~' proposition # DefnNotProp
  ;

pathUnit
  : path EOF
  ;

path
  : '(' path ')' # ParensPath
  | proposition # PropositionPath
  | query '?' # QueryPath
  | path '*' # RepeatPath
  | <assoc=right> path ('+' path)+ # ChoicePath
  | <assoc=right> path (';' path)+ # SequencePath
  ;

queryUnit
  : query EOF
  ;

query
  : '(' query ')' # ParensQuery
  | proposition # PropositionQuery
  | 'TT' # TTQuery
  | 'FF' # FFQuery
  | <assoc=right> query ('&&' query)+ # AndQuery
  | <assoc=right> query ('||' query)+ # OrQuery
  | '<' path '>' query # ExistsQuery
  | '[' path ']' query # AllQuery
  | '~' query # DefnNotQuery
  ;

// LEXER

ID     : ('a'..'z'|'A'..'Z'|'_')('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
WS     : [ \t\n\r]+ -> skip ;
