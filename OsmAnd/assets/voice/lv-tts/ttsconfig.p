:- op('==', xfy, 500).
version(101).
language(lv).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['griezties pa kreisi ']).
turn('left_sh', ['strauji pagriezties pa kreisi ']).
turn('left_sl', ['pagriezties pa kreisi ']).
turn('right', ['griezties pa labi ']).
turn('right_sh', ['strauji pagriezties pa labi ']).
turn('right_sl', ['pagriezties pa labi ']).

prepare_turn(Turn, Dist) == ['P�c ', D, ' pagriezties ', M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['P�c ', D, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['Gatavojaties apgriezties p�c ', D] :- distance(Dist) == D.
make_ut(Dist) == ['P�c ', D, ' apgrie�aties '] :- distance(Dist) == D.
make_ut == ['Apgrie�aties '].
make_ut_wp == ['Apgrie�aties pie pirm�s iesp�jas '].

prepare_roundabout(Dist) == ['Sagatvojaties lokveida kust�bai ', D] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == ['P�c ', D, ' iebrauciet lokveida krustojum�, un tad brauciet ', E, 'pagriezien�'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['izbrauciet ', E, 'izbrauktuv�'] :- nth(Exit, E).

go_ahead == ['Dodaties taisni uz priek�u '].
go_ahead(Dist) == ['Brauciet pa ce�u ', D]:- distance(Dist) == D.

and_arrive_destination == ['un ierodaties galapunkt� '].

then == ['tad '].
reached_destination == ['j�s esiet nok�uvis galapunkt� '].
bear_right == ['turieties pa labi '].
bear_left == ['turieties pa kreisi '].

route_new_calc(Dist) == ['Brauciens ir ', D] :- distance(Dist) == D.
route_recalc(Dist) == ['Mar�ruts ir p�r��in�ts, att�lums ', D] :- distance(Dist) == D.

location_lost == ['pazudis g p s sign�ls '].


%% 
nth(1, 'pirmais ').
nth(2, 'otrais ').
nth(3, 'tre�ais ').
nth(4, 'ceturtais ').
nth(5, 'piektais ').
nth(6, 'sestais ').
nth(7, 'sept�tais ').
nth(8, 'astotais ').
nth(9, 'dev�tais ').
nth(10, 'desmit ').
nth(11, 'vienpadsmitais ').
nth(12, 'divpadsmitais ').
nth(13, 'tr�spadsmitais ').
nth(14, '�etrpadsmitais ').
nth(15, 'piecpadsmitais ').
nth(16, 'se�padsmitais ').
nth(17, 'septi�padsmitais ').


%%% distance measure
distance(Dist) == [ X, ' meteriem'] :- Dist < 100, D is round(Dist/10)*10, num_atom(D, X).
distance(Dist) == [ X, ' meteriem'] :- Dist < 1000, D is round(2*Dist/100)*50, num_atom(D, X).
distance(Dist) == ['aptuveni 1 kilometers '] :- Dist < 1500.
distance(Dist) == ['aptuveni ', X, ' kilometeri '] :- Dist < 10000, D is round(Dist/1000), num_atom(D, X).
distance(Dist) == [ X, ' kilometri '] :- D is round(Dist/1000), num_atom(D, X).


%% resolve command main method
%% if you are familar with Prolog you can input specific to the whole mechanism,
%% by adding exception cases.
flatten(X, Y) :- flatten(X, [], Y), !.
flatten([], Acc, Acc).
flatten([X|Y], Acc, Res):- flatten(Y, Acc, R), flatten(X, R, Res).
flatten(X, Acc, [X|Acc]).

resolve(X, Y) :- resolve_impl(X,Z), flatten(Z, Y).
resolve_impl([],[]).
resolve_impl([X|Rest], List) :- resolve_impl(Rest, Tail), ((X == L) -> append(L, Tail, List); List = Tail).