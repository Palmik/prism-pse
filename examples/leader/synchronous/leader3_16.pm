// synchronous leader election protocol  (itai & Rodeh)
// dxp/gxn 25/01/01
// N=3 and K=16

probabilistic

// CONSTANTS
const N=3; // number of processes
const K=16; // range of probabilistic choice

// counter module used to count the number of processes that have been read
// and to know when a process has decided
module counter
	
	// counter (c=i  means process j reading process (i-1)+j next)
	c : [1..N-1];
	
	// reading
	[read] c<N-1 -> (c'=c+1);
	// finished reading
	[read] c=N-1 -> (c'=c);
	//decide
	[done] u1 | u2 | u3 -> (c'=c);
	// pick again reset counter 
	[retry] !(u1 | u2 | u3) -> (c'=1);
	// loop (when finished to avoid deadlocks)
	[loop] s1=3 -> (c'=c);
	
endmodule

//  processes form a ring and suppose:
// process 1 reads process 2
// process 2 reads process 3
// process 3 reads process 1
module process1
	
	// local state
	s1 : [0..3];
	// s1=0 make random choice
	// s1=1 reading
	// s1=2 deciding
	// s1=3 finished
	
	// has a unique id so far (initially true)
	u1 : bool;
	
	// value to be sent to next process in the ring (initially sets this to its own value)
	v1 : [0..K-1];
	
	// random choice
	p1 : [0..K-1];
	
	// pick value
	[pick] s1=0 -> 1/K : (s1'=1) & (p1'=0) & (v1'=0) & (u1'=true)
	             + 1/K : (s1'=1) & (p1'=1) & (v1'=1) & (u1'=true)
	             + 1/K : (s1'=1) & (p1'=2) & (v1'=2) & (u1'=true)
	             + 1/K : (s1'=1) & (p1'=3) & (v1'=3) & (u1'=true)
	             + 1/K : (s1'=1) & (p1'=4) & (v1'=4) & (u1'=true)
	             + 1/K : (s1'=1) & (p1'=5) & (v1'=5) & (u1'=true)
	             + 1/K : (s1'=1) & (p1'=6) & (v1'=6) & (u1'=true)
	             + 1/K : (s1'=1) & (p1'=7) & (v1'=7) & (u1'=true)
	             + 1/K : (s1'=1) & (p1'=8) & (v1'=8) & (u1'=true)
	             + 1/K : (s1'=1) & (p1'=9) & (v1'=9) & (u1'=true)
	             + 1/K : (s1'=1) & (p1'=10) & (v1'=10) & (u1'=true)
	             + 1/K : (s1'=1) & (p1'=11) & (v1'=11) & (u1'=true)
	             + 1/K : (s1'=1) & (p1'=12) & (v1'=12) & (u1'=true)
	             + 1/K : (s1'=1) & (p1'=13) & (v1'=13) & (u1'=true)
	             + 1/K : (s1'=1) & (p1'=14) & (v1'=14) & (u1'=true)
	             + 1/K : (s1'=1) & (p1'=15) & (v1'=15) & (u1'=true);
	// read
	[read] s1=1 &  u1 & !p1=v2 & c<N-1 -> (u1'=true) & (v1'=v2);
	[read] s1=1 &  u1 &  p1=v2 & c<N-1 -> (u1'=false) & (v1'=v2) & (p1'=0);
	[read] s1=1 & !u1 &  c<N-1 -> (u1'=false) & (v1'=v2);
	// read and move to decide
	[read] s1=1 &  u1 & !p1=v2 & c=N-1 -> (s1'=2) & (u1'=true) & (v1'=0) & (p1'=0);
	[read] s1=1 &  u1 &  p1=v2 & c=N-1 -> (s1'=2) & (u1'=false) & (v1'=0) & (p1'=0);
	[read] s1=1 & !u1 &  c=N-1 -> (s1'=2) & (u1'=false) & (v1'=0);
	// deciding
	// done
	[done] s1=2 -> (s1'=3) & (u1'=false) & (v1'=0) & (p1'=0);
	//retry
	[retry] s1=2 -> (s1'=0) & (u1'=false) & (v1'=0) & (p1'=0);
	// loop (when finished to avoid deadlocks)
	[loop] s1=3 -> (s1'=3);
	
endmodule

// construct remaining processes through renaming
module process2=process1[s1=s2,p1=p2,v1=v2,u1=u2,v2=v3] endmodule
module process3=process1[s1=s3,p1=p3,v1=v3,u1=u3,v2=v1] endmodule

// REWARDS -expected number of rounds
rewards
	
	[pick] true : 1;
	
endrewards
