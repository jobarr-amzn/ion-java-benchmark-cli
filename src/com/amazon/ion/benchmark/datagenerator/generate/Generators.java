package com.amazon.ion.benchmark.datagenerator.generate;

import com.amazon.ion.benchmark.datagenerator.schema.ReparsedType;

import java.security.SecureRandom;

//TODO: *This* is the standard library! Move core types etc. from the poorly-named DataGenerator to here.
public class Generators {

    private static final SecureRandom random = new SecureRandom();
    public void setSeed(long seed) {
        random.setSeed(seed);
    }

    public static Generator one_of(Generator... generators) {
        return type -> chooseRandomly(generators).generate(type);
    }

    //TODO: Range generator(s)

    //TODO: More like this?
    // DEFAULT_FLOAT_CONSTRAINTS should be a (smaller) range
    // DEFAULT_STRING could be regex constraint, or something [A-Z][a-z]{2,10}
//    public static Generator a_float() {
//        return a_float(DEFAULT_FLOAT_CONSTRAINTS);
//    }
//
//    public static Generator a_float(ReparsedType type) {
//
//    }

    //TODO: all_of??? each_of???

    // When building a generator for a type, grab whatever constraints we know how to handle together,
    // and compose them. Keep a record of consumed types. Register this generator for the type.
    // Then check the consumed constraints set against the key set for the ReparsedType's constraints.
    // If they're the same, great! We've successfully created a generator for this type.
    // Otherwise the consumed constraints set and key set for a ReparsedType are not the same,
    // we can't generate this type.
    // If we have already registered a generator for this type name, throw an error:
    //   "Can't generate '<type_name>' for competing constraints: [<registered constraints>] and [<residual constraints>]"
    // If we have not registered a generator for this type name, throw an error:
    //   "Can't generate '<type_name>' for contraints: [<residual constraints>]"

    @SafeVarargs
    private static <T> T chooseRandomly(T... objects) {
        int index = random.nextInt(objects.length);
        return objects[index];
    }
}
