package com.amazon.ion.benchmark.datagenerator.generate;

import com.amazon.ion.benchmark.datagenerator.schema.ReparsedType;
import com.sun.tools.javac.jvm.Gen;

import java.util.Map;

public class GeneratorMapSet implements GeneratorSet {
    private Map<String, Generator> map;

    private GeneratorMapSet(Map<String, Generator> map) {
        this.map = map;
    }

    @Override
    public Generator get(String name) {
        return map.getOrDefault(name, NotDefinedException.GENERATOR);
    }

    static GeneratorMapSet of(Map<String, Generator> map) {
        return new GeneratorMapSet(map);
    }

    static final Generator NOT_DEFINED = type -> { throw new NotDefinedException(type); };

    static class NotDefinedException extends RuntimeException {
        NotDefinedException(ReparsedType type) {
            super(String.format("Type with name '%s' is not defined", type.getName()));
        }
        static final Generator GENERATOR = type -> { throw new NotDefinedException(type); };
    }
}
