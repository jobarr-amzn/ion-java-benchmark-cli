package com.amazon.ion.benchmark.datagenerator.generate;

import com.amazon.ion.benchmark.datagenerator.DataConstructor;
import com.amazon.ion.benchmark.datagenerator.schema.ReparsedType;
import com.amazon.ionelement.api.Ion;
import com.amazon.ionelement.api.IonElement;

import java.util.HashMap;
import java.util.Map;

//TODO: Better name!
//TODO: Extract standard library from this
public class DataGenerator {

    //TODO: Way to append / Method for registering new generator :(
    // Probably we should have a GeneratorSet view of a more permissive type (Map? Our own?)
    GeneratorSet generators;

    //TODO: Constructor

    //TODO: Recursive types, how to handle. For now, assume that they don't exist :D
    static final GeneratorSet CORE_TYPES = buildCoreTypesMap();

    //TODO: Immutable. Our own wrapper interface that removes remove and put? Use Immutables? Guava?
    //TODO: Detect clobbering/overrides (both here prior to immutability, and for user-defined types)
    //TODO: Test with reflexive validation for core and Ion types (would catch bad IonValue generation)
    //TODO: Common pattern in e.g. DataConstructor for validating that we've consumed all constraints
    // * Create set, consume constraints and add to set, validate against type's contraint map's keyset
    // * Want to avoid cloning/altering the constraint map
    //TODO: Change interfaces to use ReparsedType instead of Map.
    private static GeneratorSet buildCoreTypesMap() {
        Map<String, Generator> coreTypesMap = new HashMap<>();
        // Scalar types are straightforward
        coreTypesMap.put("float", DataGenerator::generateFloat);
        coreTypesMap.put("symbol",    type -> Ion.ionSymbol(DataConstructor.constructString(type.getConstraintMap())));
        coreTypesMap.put("int",       type -> Ion.ionInt(DataConstructor.constructInt(type.getConstraintMap())));
        coreTypesMap.put("string",    type -> Ion.ionString(DataConstructor.constructString(type.getConstraintMap())));
        coreTypesMap.put("decimal",   type -> Ion.ionDecimal(DataConstructor.constructDecimal(type.getConstraintMap())));
        coreTypesMap.put("timestamp", type -> Ion.ionTimestamp(DataConstructor.constructTimestamp(type.getConstraintMap())));
        coreTypesMap.put("blob",      type -> Ion.ionBlob(DataConstructor.constructLobs(type.getConstraintMap())));
        coreTypesMap.put("clob",      type -> Ion.ionClob(DataConstructor.constructLobs(type.getConstraintMap())));

        // Sequence types less straightforward
        //TODO: Compose other generators...
//        coreTypesMap.put("struct", ...);

        //TODO: These have implementations that are slightly more complicated, need to convert to composited
//        coreTypesMap.put("list", ...);
//        coreTypesMap.put("sexp", ...);

        //TODO: Compose other primitives with one_of
//        coreTypesMap.put("document", ...);
//        coreTypesMap.put("lob", ...);
//        coreTypesMap.put("number", ...);
//        coreTypesMap.put("text", ...);
//        coreTypesMap.put("any", ...);
        coreTypesMap.put("nothing", DataGenerator::nothing);

        return GeneratorMapSet.of(coreTypesMap);
    }

    //TODO: Implement, these are all essentially core types + nullable
    //TODO: Implement type-constrained null generators, use Generators.one_of with core type generator
    static final GeneratorSet ION_TYPES = buildIonTypesMap();

    private static GeneratorSet buildIonTypesMap() {
        Map<String, Generator> ionTypesMap = new HashMap<>();

//        ionTypesMap.put("$float", ...);
//        ionTypesMap.put("$symbol", ...);
//        ionTypesMap.put("$int", ...);
//        ionTypesMap.put("$string", ...);
//        ionTypesMap.put("$decimal", ...);
//        ionTypesMap.put("$timestamp", ...);
//        ionTypesMap.put("$blob", ...);
//        ionTypesMap.put("$clob", ...);

        ionTypesMap.put("$struct", type -> DataConstructor.constructIonStruct(type.getConstraintMap()));

        // TODO: These have implementations that are slightly more complicated
//        ionTypesMap.put("$list", ...);
//        ionTypesMap.put("$sexp", ...);

        return GeneratorMapSet.of(ionTypesMap);
    }
    
    private static IonElement nothing(ReparsedType $) {
        throw new IllegalArgumentException("Can't generate 'nothing'");
    }

    private static IonElement generateFloat(ReparsedType type) {
        return Ion.ionFloat(DataConstructor.constructFloat(type.getConstraintMap()));
    }

    //TODO: Add compositing primitives in `Generators` as necessary
}
