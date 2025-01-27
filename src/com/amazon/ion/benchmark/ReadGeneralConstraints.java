package com.amazon.ion.benchmark;

import com.amazon.ion.IonDatagram;
import com.amazon.ion.IonLoader;
import com.amazon.ion.IonReader;
import com.amazon.ion.IonStruct;
import com.amazon.ion.IonSystem;
import com.amazon.ion.IonType;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.system.IonReaderBuilder;
import com.amazon.ion.system.IonSystemBuilder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 * Parse Ion Schema file and get the general constraints in the file then pass the constraints to the Ion data generator.
 */
public class ReadGeneralConstraints {
    public static final IonSystem SYSTEM = IonSystemBuilder.standard().build();
    public static final IonLoader LOADER = SYSTEM.newLoader();

    /**
     * Get general constraints of Ion Schema and call the relevant generator method based on the type.
     * @param size is the size of the output file.
     * @param path is the path of the Ion Schema file.
     * @param format is the format of the generated file, select from set (ion_text | ion_binary).
     * @param outputFile is the path of the generated file.
     * @throws Exception if errors occur when reading and writing data.
     */
    public static void readIonSchemaAndGenerate(int size, String path, String format, String outputFile) throws Exception {
        try (IonReader reader = IonReaderBuilder.standard().build(new BufferedInputStream(new FileInputStream(path)))) {
            IonDatagram schema = LOADER.load(reader);
            for (int i = 0; i < schema.size(); i++) {
                IonValue schemaValue = schema.get(i);
                // Assume there's only one constraint between schema_header and schema_footer, if more constraints added, here is the point where developers should start.
                if (schemaValue.getType().equals(IonType.STRUCT) && schemaValue.getTypeAnnotations()[0].equals(IonSchemaUtilities.KEYWORD_TYPE)) {
                    IonStruct constraintStruct = (IonStruct) schemaValue;
                    //Construct the writer and pass the constraints to the following writing data to files process.
                    File file = new File(outputFile);
                    try (IonWriter writer = WriteRandomIonValues.formatWriter(format, file)) {
                        WriteRandomIonValues.writeRequestedSizeFile(size, writer, file, constraintStruct);
                    }
                    // Print the successfully generated data notification which includes the file path information.
                    WriteRandomIonValues.printInfo(outputFile);
                }
            }
        }
    }
}
