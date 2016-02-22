grammar Behaviour;

name
  : ID # IDName
  | '..' # ParentName
  | '*' # AnyName
  ;

locationUnit
  : location EOF
  ;

location
  : ID # FixedLocation
  | <assoc=right> ('/' name)+ # AbsoluteLocation
  | <assoc=right> '..' ('/' name)* # RelativeLocation
  ;

// TODO: generalise to use more expressive message types
message
  : '(' message ')' # ParensMessage
  | ID # IDMessage
  ;

roleUnit
  : role EOF
  ;

// TODO: generalise to allow predicate roles
role
  : '(' role ')' # ParensRole
  | 'true' # TrueRole
  | 'false' # FalseRole
  | ID # IDRole
  | '!' ID # NotIDRole
  | <assoc=right> role ('&&' role)+ # ConjunctionRole
  | <assoc=right> role ('||' role)+ # DisjunctionRole
  | '~' role # DefnNotRole
  ;

pathUnit
  : path EOF
  ;

path
  : '(' path ')' # ParensPath
  | 'true' # TrueEvent
  | 'false' # FalseEvent
  | location '?' message # ReceiveEvent
  | location '!' message # SendEvent
  | 'if' behaviour # BehaviourPath
  | path '*' # ZeroOrMorePath
  | path '+' # OneOrMorePath
  | <assoc=right> path ('+' path)+ # ChoicePath
  | <assoc=right> path (';' path)+ # SequencePath
  ;

behaviourUnit
  : behaviour EOF
  ;

behaviour
  : '(' behaviour ')' # ParensBehaviour
  | role # RoleQuery
  | 'TT' # TTBehaviour
  | 'FF' # FFBehaviour
  | <assoc=right> behaviour ('&&' behaviour)+ # AndBehaviour
  | <assoc=right> behaviour ('||' behaviour)+ # OrBehaviour
  | '<' path '>' behaviour # ExistsBehaviour
  | '[' path ']' behaviour # AllBehaviour
  | '~' behaviour # DefnNotBehaviour
  ;

// LEXER

// NOTE: the '_' ID generally has a special meaning
ID     : ('a'..'z'|'A'..'Z'|'_')('a'..'z'|'A'..'Z'|'0'..'9'|'_'|'$')*;
WS     : [ \t\n\r]+ -> skip ;
