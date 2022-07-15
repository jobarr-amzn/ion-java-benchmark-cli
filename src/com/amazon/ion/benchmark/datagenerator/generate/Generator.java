package com.amazon.ion.benchmark.datagenerator.generate;

import com.amazon.ion.benchmark.datagenerator.schema.ReparsedType;
import com.amazon.ionelement.api.IonElement;

@FunctionalInterface
public interface Generator {
    IonElement generate(ReparsedType type) throws Throwable;
}
