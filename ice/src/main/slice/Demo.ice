
#ifndef SR_DEMO_ICE
#define SR_DEMO_ICE

//#include <Ice/BuiltinSequences.ice> 

module Demo
{
	exception RequestCanceledException{};

	interface Calc
	{
		float add1(float a, float b);
		["amd"] float add2(float a, float b) throws RequestCanceledException;
		float subtract(float a, float b);
	};
};

#endif
