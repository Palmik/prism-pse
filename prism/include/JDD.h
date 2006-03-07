/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class jdd_JDD */

#ifndef _Included_jdd_JDD
#define _Included_jdd_JDD
#ifdef __cplusplus
extern "C" {
#endif
#undef jdd_JDD_PLUS
#define jdd_JDD_PLUS 1L
#undef jdd_JDD_MINUS
#define jdd_JDD_MINUS 2L
#undef jdd_JDD_TIMES
#define jdd_JDD_TIMES 3L
#undef jdd_JDD_DIVIDE
#define jdd_JDD_DIVIDE 4L
#undef jdd_JDD_MIN
#define jdd_JDD_MIN 5L
#undef jdd_JDD_MAX
#define jdd_JDD_MAX 6L
#undef jdd_JDD_EQUALS
#define jdd_JDD_EQUALS 7L
#undef jdd_JDD_NOTEQUALS
#define jdd_JDD_NOTEQUALS 8L
#undef jdd_JDD_GREATERTHAN
#define jdd_JDD_GREATERTHAN 9L
#undef jdd_JDD_GREATERTHANEQUALS
#define jdd_JDD_GREATERTHANEQUALS 10L
#undef jdd_JDD_LESSTHAN
#define jdd_JDD_LESSTHAN 11L
#undef jdd_JDD_LESSTHANEQUALS
#define jdd_JDD_LESSTHANEQUALS 12L
#undef jdd_JDD_FLOOR
#define jdd_JDD_FLOOR 13L
#undef jdd_JDD_CEIL
#define jdd_JDD_CEIL 14L
#undef jdd_JDD_POW
#define jdd_JDD_POW 15L
#undef jdd_JDD_MOD
#define jdd_JDD_MOD 16L
#undef jdd_JDD_ZERO_ONE
#define jdd_JDD_ZERO_ONE 1L
#undef jdd_JDD_LOW
#define jdd_JDD_LOW 2L
#undef jdd_JDD_NORMAL
#define jdd_JDD_NORMAL 3L
#undef jdd_JDD_HIGH
#define jdd_JDD_HIGH 4L
#undef jdd_JDD_LIST
#define jdd_JDD_LIST 5L
#undef jdd_JDD_CMU
#define jdd_JDD_CMU 1L
#undef jdd_JDD_BOULDER
#define jdd_JDD_BOULDER 2L
/*
 * Class:     jdd_JDD
 * Method:    GetCUDDManager
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_GetCUDDManager
  (JNIEnv *, jclass);

/*
 * Class:     jdd_JDD
 * Method:    DD_SetOutputStream
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1SetOutputStream
  (JNIEnv *, jclass, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_GetOutputStream
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1GetOutputStream
  (JNIEnv *, jclass);

/*
 * Class:     jdd_JDD
 * Method:    DD_InitialiseCUDD
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1InitialiseCUDD__
  (JNIEnv *, jclass);

/*
 * Class:     jdd_JDD
 * Method:    DD_InitialiseCUDD
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1InitialiseCUDD__JD
  (JNIEnv *, jclass, jlong, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_SetCUDDMaxMem
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1SetCUDDMaxMem
  (JNIEnv *, jclass, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_SetCUDDEpsilon
 * Signature: (D)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1SetCUDDEpsilon
  (JNIEnv *, jclass, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_CloseDownCUDD
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1CloseDownCUDD
  (JNIEnv *, jclass, jboolean);

/*
 * Class:     jdd_JDD
 * Method:    DD_Ref
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1Ref
  (JNIEnv *, jclass, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_Deref
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1Deref
  (JNIEnv *, jclass, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_PrintCacheInfo
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintCacheInfo
  (JNIEnv *, jclass);

/*
 * Class:     jdd_JDD
 * Method:    DD_Create
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Create
  (JNIEnv *, jclass);

/*
 * Class:     jdd_JDD
 * Method:    DD_Constant
 * Signature: (D)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Constant
  (JNIEnv *, jclass, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_PlusInfinity
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1PlusInfinity
  (JNIEnv *, jclass);

/*
 * Class:     jdd_JDD
 * Method:    DD_MinusInfinity
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1MinusInfinity
  (JNIEnv *, jclass);

/*
 * Class:     jdd_JDD
 * Method:    DD_Var
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Var
  (JNIEnv *, jclass, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_Not
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Not
  (JNIEnv *, jclass, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_Or
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Or
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_And
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1And
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_Xor
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Xor
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_Implies
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Implies
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_Apply
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Apply
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_MonadicApply
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1MonadicApply
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_Restrict
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Restrict
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_ITE
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1ITE
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_PermuteVariables
 * Signature: (IIII)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1PermuteVariables
  (JNIEnv *, jclass, jint, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_SwapVariables
 * Signature: (IIII)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1SwapVariables
  (JNIEnv *, jclass, jint, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_VariablesGreaterThan
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1VariablesGreaterThan
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_VariablesGreaterThanEquals
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1VariablesGreaterThanEquals
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_VariablesLessThan
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1VariablesLessThan
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_VariablesLessThanEquals
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1VariablesLessThanEquals
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_VariablesEquals
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1VariablesEquals
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_ThereExists
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1ThereExists
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_ForAll
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1ForAll
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_SumAbstract
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1SumAbstract
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_ProductAbstract
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1ProductAbstract
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_MinAbstract
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1MinAbstract
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_MaxAbstract
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1MaxAbstract
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_GreaterThan
 * Signature: (ID)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1GreaterThan
  (JNIEnv *, jclass, jint, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_GreaterThanEquals
 * Signature: (ID)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1GreaterThanEquals
  (JNIEnv *, jclass, jint, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_LessThan
 * Signature: (ID)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1LessThan
  (JNIEnv *, jclass, jint, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_LessThanEquals
 * Signature: (ID)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1LessThanEquals
  (JNIEnv *, jclass, jint, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_Equals
 * Signature: (ID)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Equals
  (JNIEnv *, jclass, jint, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_Interval
 * Signature: (IDD)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Interval
  (JNIEnv *, jclass, jint, jdouble, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_RoundOff
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1RoundOff
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_EqualSupNorm
 * Signature: (IID)Z
 */
JNIEXPORT jboolean JNICALL Java_jdd_JDD_DD_1EqualSupNorm
  (JNIEnv *, jclass, jint, jint, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_FindMin
 * Signature: (I)D
 */
JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1FindMin
  (JNIEnv *, jclass, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_FindMax
 * Signature: (I)D
 */
JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1FindMax
  (JNIEnv *, jclass, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_RestrictToFirst
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1RestrictToFirst
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_GetNumNodes
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1GetNumNodes
  (JNIEnv *, jclass, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_GetNumTerminals
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1GetNumTerminals
  (JNIEnv *, jclass, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_GetNumMinterms
 * Signature: (II)D
 */
JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1GetNumMinterms
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_GetNumPaths
 * Signature: (I)D
 */
JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1GetNumPaths
  (JNIEnv *, jclass, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_PrintInfo
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintInfo
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_PrintInfoBrief
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintInfoBrief
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_PrintSupport
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintSupport
  (JNIEnv *, jclass, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_GetSupport
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1GetSupport
  (JNIEnv *, jclass, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_PrintTerminals
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintTerminals
  (JNIEnv *, jclass, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_PrintTerminalsAndNumbers
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintTerminalsAndNumbers
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_SetVectorElement
 * Signature: (IIIJD)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1SetVectorElement
  (JNIEnv *, jclass, jint, jint, jint, jlong, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_SetMatrixElement
 * Signature: (IIIIIJJD)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1SetMatrixElement
  (JNIEnv *, jclass, jint, jint, jint, jint, jint, jlong, jlong, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_Set3DMatrixElement
 * Signature: (IIIIIIIJJJD)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Set3DMatrixElement
  (JNIEnv *, jclass, jint, jint, jint, jint, jint, jint, jint, jlong, jlong, jlong, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_GetVectorElement
 * Signature: (IIIJ)D
 */
JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1GetVectorElement
  (JNIEnv *, jclass, jint, jint, jint, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_Identity
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Identity
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_Transpose
 * Signature: (IIII)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Transpose
  (JNIEnv *, jclass, jint, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_MatrixMultiply
 * Signature: (IIIII)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1MatrixMultiply
  (JNIEnv *, jclass, jint, jint, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_PrintVector
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintVector
  (JNIEnv *, jclass, jint, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_PrintMatrix
 * Signature: (IIIIII)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintMatrix
  (JNIEnv *, jclass, jint, jint, jint, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_PrintVectorFiltered
 * Signature: (IIIII)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintVectorFiltered
  (JNIEnv *, jclass, jint, jint, jint, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_ExportDDToDotFile
 * Signature: (ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1ExportDDToDotFile
  (JNIEnv *, jclass, jint, jstring);

/*
 * Class:     jdd_JDD
 * Method:    DD_ExportDDToDotFileLabelled
 * Signature: (ILjava/lang/String;Ljava/util/Vector;)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1ExportDDToDotFileLabelled
  (JNIEnv *, jclass, jint, jstring, jobject);

/*
 * Class:     jdd_JDD
 * Method:    DD_ExportMatrixToPPFile
 * Signature: (IIIIILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1ExportMatrixToPPFile
  (JNIEnv *, jclass, jint, jint, jint, jint, jint, jstring);

/*
 * Class:     jdd_JDD
 * Method:    DD_ExportMatrixToMatlabFile
 * Signature: (IIIIILjava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1ExportMatrixToMatlabFile
  (JNIEnv *, jclass, jint, jint, jint, jint, jint, jstring, jstring);

/*
 * Class:     jdd_JDD
 * Method:    DD_ExportMatrixToSpyFile
 * Signature: (IIIIIILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1ExportMatrixToSpyFile
  (JNIEnv *, jclass, jint, jint, jint, jint, jint, jint, jstring);

/*
 * Class:     jdd_JDD
 * Method:    DD_Printf
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1Printf
  (JNIEnv *, jclass, jstring);

#ifdef __cplusplus
}
#endif
#endif
