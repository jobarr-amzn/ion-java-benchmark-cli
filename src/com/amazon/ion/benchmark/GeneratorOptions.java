package com.amazon.ion.benchmark;

import com.amazon.ionschema.Schema;

import java.util.List;
import java.util.Map;

/**
 * Execute Ion Data Generator after receiving the hashmap of command line options.
 */
public class GeneratorOptions {

    /**
     * Check the validation of input ion schema and execute the Ion Data generating process.
     * @param optionsMap is the hash map which generated by the command line parser which match the option name and its value appropriately.
     * @throws Exception if errors occurs when calling the methods of generating Ion data.
     */
    public static void executeGenerator(Map<String, Object> optionsMap) throws Exception {
        int size = Integer.parseInt(optionsMap.get("--data-size").toString());
        String format = ((List<String>) optionsMap.get("--format")).get(0);
        String path = optionsMap.get("<output_file>").toString();
        String inputFilePath = optionsMap.get("--input-ion-schema").toString();
        // Check whether the input schema file is valid and get the loaded schema.
        Schema schema = IonSchemaUtilities.loadSchemaDefinition(inputFilePath);
        ReadGeneralConstraints.constructAndWriteIonData(size, schema, format, path);
    }
}
