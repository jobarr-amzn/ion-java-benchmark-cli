package com.amazon.ion.benchmark;

import com.amazon.ion.IonDatagram;
import com.amazon.ion.IonDecimal;
import com.amazon.ion.IonFloat;
import com.amazon.ion.IonList;
import com.amazon.ion.IonReader;
import com.amazon.ion.IonString;
import com.amazon.ion.IonStruct;
import com.amazon.ion.IonType;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.benchmark.datagenerator.ReadGeneralConstraints;
import com.amazon.ion.system.IonReaderBuilder;
import com.amazon.ion.system.IonTextWriterBuilder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParseAndCompareBenchmarkResults {
    public static final String RELATIVE_DIFFERENCE_SCORE = "relative_difference_score";
    public static final List<String> BENCHMARK_SCORE_KEYWORDS = Arrays.asList("speed", "Heap usage", "Serialized size", "·gc.alloc.rate");
    private static final String PRIMARY_METRIC = "primaryMetric";
    private static final String PARAMETERS = "params";
    private static final String INPUT = "input";
    private static final String OPTIONS = "options";
    private static final String SECONDARY_METRIC = "secondaryMetrics";
    private static final String SCORE = "score";
    private static final String SPEED = "speed";
    private static final String FORMAT = "format";
    private static final String TYPE = "type";
    private static final String API = "api";
    private static final String RAW_DATA = "rawData";
    private static final String FORMAT_KEYWORD = "f";
    private static final String TYPE_KEYWORD = "t";

    /**
     * Get the paths of benchmark results from two commits then invoke the methods to calculate relative change for each aspect from the result.
     * @param optionsMap is the hash map which generated by the command line parser which match the option name and its value appropriately.
     * @throws Exception if errors occur when reading Ion data.
     */
    public static void compareResult(Map<String, Object> optionsMap) throws Exception {
        String benchmarkResultPrevious = optionsMap.get("--benchmark-result-previous").toString();
        String benchmarkResultNew = optionsMap.get("--benchmark-result-new").toString();
        String outputFilePath = optionsMap.get("<output_file>").toString();
        Map<String, BigDecimal> scoreMap = new HashMap<>();
        for (String benchmarkScoreKeyword : BENCHMARK_SCORE_KEYWORDS) {
            BigDecimal previousScore = getScore(benchmarkResultPrevious, benchmarkScoreKeyword);
            BigDecimal newScore = getScore(benchmarkResultNew, benchmarkScoreKeyword);
            BigDecimal result = calculateDifference(previousScore, newScore);
            scoreMap.put(benchmarkScoreKeyword, result);
        }
        Map<String, BigDecimal> thresholdMap = getThresholdMap(benchmarkResultPrevious, benchmarkResultNew);
        writeResult(benchmarkResultNew, outputFilePath, scoreMap, thresholdMap);
    }

    /**
     * Calculate the threshold scores and construct a map to match the threshold with the aspect it represents.
     * @param benchmarkResultPrevious is the benchmark result of ion-java from the existing commit.
     * @param benchmarkResultNew is the benchmark result of ion-java from the new commit.
     * @return a map which match the thresholds score with the aspect name it represents.
     * @throws Exception if errors occurs when calling method parseScore.
     */
    public static Map<String, BigDecimal> getThresholdMap(String benchmarkResultPrevious, String benchmarkResultNew) throws Exception {
        Map<String, BigDecimal> thresholdMap = new HashMap<>();
        for (String keyWord : BENCHMARK_SCORE_KEYWORDS) {
            IonList rawDataPrevious = (IonList) parseScore(benchmarkResultPrevious, keyWord).get(RAW_DATA);
            IonList rawDataNew = (IonList) parseScore(benchmarkResultNew, keyWord).get(RAW_DATA);
            BigDecimal thresholdPrevious = getThresholdScore((IonList) rawDataPrevious.get(0));
            BigDecimal thresholdNew = getThresholdScore((IonList) rawDataNew.get(0));
            if (thresholdPrevious.compareTo(thresholdNew) < 0) {
                thresholdMap.put(keyWord, thresholdPrevious);
            } else {
                thresholdMap.put(keyWord, thresholdNew);
            }
        }
        return thresholdMap;
    }

    /**
     * Get threshold score by applying (minScore - maxScore)/maxScore to a list of raw data in benchmark result.
     * @param rawDataList is an Ion List contains performance scores from multiple iterations of benchmark process.
     * @return calculated threshold score.
     */
    private static BigDecimal getThresholdScore(IonList rawDataList) {
        List<BigDecimal> rawData = new ArrayList<>();
        for (int i = 0; i < rawDataList.size(); i++ ) {
            IonDecimal score = (IonDecimal) rawDataList.get(i);
            rawData.add(score.bigDecimalValue());
        }
        BigDecimal threshold = calculateDifference(Collections.max(rawData), Collections.min(rawData));
        return threshold;
    }

    /**
     * Get score of specific aspect from set (speed | heap usage | serialized size | gc.allocated.rate) after parsing the benchmark result.
     * @param benchmarkResultFilePath is the path of benchmark result file.
     * @param keyWord from set (speed | Heap usage | Serialized size | ·gc.alloc.rate) specifies which score will be extracted from the benchmark result.
     * @return the score of specific aspect in BigDecimal format.
     * @throws Exception if error occurs when reading Ion Data.
     */
    public static BigDecimal getScore(String benchmarkResultFilePath, String keyWord) throws Exception {
        IonStruct scoreStruct = parseScore(benchmarkResultFilePath, keyWord);
        IonValue score = scoreStruct.get(SCORE);
        if (score.getType().equals(IonType.FLOAT)) {
            IonFloat scoreFloat = (IonFloat) score;
            return scoreFloat.bigDecimalValue();
        } else {
            IonDecimal scoreDecimal = (IonDecimal) score;
            return scoreDecimal.bigDecimalValue();
        }
    }

    /**
     * Parse the benchmark result and extract the IonStruct which contains scores information.
     * @param benchmarkResultFilePath is the file path of benchmark result.
     * @param keyWord represents which aspect of scores are required to be extracted.
     * @return an IonStruct which contains the scores information.
     * @throws Exception if errors occur when create IonReader.
     */
    private static IonStruct parseScore(String benchmarkResultFilePath, String keyWord) throws Exception {
        IonStruct scoreStruct;
        IonStruct benchmarkResultStruct = readHelper(benchmarkResultFilePath);
        if (keyWord.equals(SPEED)) {
            scoreStruct = (IonStruct)benchmarkResultStruct.get(PRIMARY_METRIC);
        } else {
            IonStruct secondaryMetricStruct = (IonStruct) benchmarkResultStruct.get(SECONDARY_METRIC);
            scoreStruct = (IonStruct)secondaryMetricStruct.get(keyWord);
        }
        return scoreStruct;
    }

    /**
     * Extract the parameter of specific aspect from set (input | options) out of benchmark result.
     * @param benchmarkResultFilePath is the path of benchmark result.
     * @param keyWord specifies which parameter will be returned, from set (input | options).
     * @return an Ion Value represents the parameter conforms with the keyword.
     * @throws Exception if error occurs when reading Ion data.
     */
    private static IonValue getParameter(String benchmarkResultFilePath, String keyWord) throws Exception{
        IonStruct benchmarkResultStruct = readHelper(benchmarkResultFilePath);
        IonStruct parameterStruct = (IonStruct)benchmarkResultStruct.get(PARAMETERS);
        return parameterStruct.get(keyWord);
    }

    /**
     * This is a helper method which create a IonReader for benchmark result and extract the IonStruct which contain parameters or scores information.
     * @param benchmarkResultFilePath is the path of benchmark result.
     * @return an Ion Struct which contains information of parameters using during the benchmark process or scores.
     * @throws Exception if error occurs when reading Ion Data.
     */
    private static IonStruct readHelper(String benchmarkResultFilePath) throws Exception {
        try (IonReader reader = IonReaderBuilder.standard().build(new BufferedInputStream(new FileInputStream(benchmarkResultFilePath)))) {
            reader.next();
            if (reader.getType().equals(IonType.LIST)) {
                reader.stepIn();
                reader.next();
                if (reader.getType().equals(IonType.STRUCT)) {
                    IonDatagram benchmarkResultDatagram = ReadGeneralConstraints.LOADER.load(reader);
                    IonStruct benchmarkResult = (IonStruct) benchmarkResultDatagram.get(0);
                    return benchmarkResult;
                }
            }
            throw new IllegalStateException("The content of benchmark result is not supported.");
        }
    }

    /**
     * Calculate the relative difference between scores from benchmark results of different in-java commits.
     * @param previousScore is score from the benchmark result of the existing ion-java commit.
     * @param newScore is score from the benchmark result of the new ion-java commit.
     * @return relative changes of two scores from different benchmark results in BigDecimal format.
     */
    private static BigDecimal calculateDifference(BigDecimal previousScore, BigDecimal newScore) {
        BigDecimal scoreDifference = newScore.subtract(previousScore);
        return scoreDifference.divide(previousScore, RoundingMode.HALF_UP);
    }

    /**
     * Write calculated relative changes of scores in an Ion Struct into the generated file and detect if performance regression happened.
     * @param benchmarkResult is the path of benchmark result.
     * @param outputFilePath is destination path of generated result.
     * @param scoreMap is a hashmap which match relative change of the score with the aspect it represents.
     * @param thresholdMap is a hashmap which match threshold of the score with the aspect it represents.
     * @throws Exception if error occurs when reading Ion data.
     */
    private static void writeResult(String benchmarkResult, String outputFilePath, Map<String, BigDecimal> scoreMap, Map<String, BigDecimal> thresholdMap) throws Exception {
        File file = new File(outputFilePath);
        IonString inputFileName = (IonString) getParameter(benchmarkResult, INPUT);
        String parameters = getParameter(benchmarkResult, OPTIONS).toString();
        try (
                IonWriter writer = IonTextWriterBuilder.standard().build(new BufferedOutputStream(new FileOutputStream(file)));
                IonReader reader = IonReaderBuilder.standard().build(parameters.substring(1,parameters.length() - 1))
        ) {
            reader.next();
            writer.stepIn(IonType.STRUCT);
            writer.setFieldName(INPUT);
            writer.writeString(inputFileName.stringValue().substring(inputFileName.stringValue().lastIndexOf("/") + 1));
            writer.addTypeAnnotation(reader.getTypeAnnotations()[0]);
            writer.setFieldName(PARAMETERS);
            writer.stepIn(IonType.STRUCT);
            reader.stepIn();
            while (reader.next() != null) {
                if (reader.getFieldName().equals(FORMAT_KEYWORD)) {
                    writer.setFieldName(FORMAT);
                    writer.writeString(reader.stringValue());
                } else if (reader.getFieldName().equals(TYPE_KEYWORD)) {
                    writer.setFieldName(TYPE);
                    writer.writeString(reader.stringValue());
                } else {
                    writer.setFieldName(API);
                    writer.writeString(reader.stringValue());
                }
            }
            reader.stepOut();
            writer.stepOut();
            writer.setFieldName(RELATIVE_DIFFERENCE_SCORE);
            writer.stepIn(IonType.STRUCT);
            for ( String scoreName : scoreMap.keySet()) {
                writer.setFieldName(scoreName);
                writer.writeDecimal(scoreMap.get(scoreName));
            }
            writer.stepOut();
            writer.stepOut();
        }
        String regressionDetectResult = detectRegression(thresholdMap, scoreMap, outputFilePath);
        // This print out value will be passed to one environment variable in the GitHub Actions workflow.
        if (regressionDetectResult != null) {
            System.out.println(regressionDetectResult);
        }
    }

    /**
     * Compare the relative changes of benchmark results with the thresholds, if the relative change smaller than threshold score which represent the decrease threshold of one
     * specific aspect, then the performance regression detected and return 'true'.
     * @param thresholdMap is a hashmap which match threshold of the score with the aspect it represents.
     * @param scoreMap is a hashmap which match relative change of the score with the aspect it represents.
     * @param outputFilePath is the destination of generated report after comparison process.
     * @return a String which contains the information about whether performance regression happened.
     * @throws Exception if occur happen when reading Ion Data.
     */
    public static String detectRegression(Map<String, BigDecimal> thresholdMap, Map<String, BigDecimal> scoreMap, String outputFilePath) throws Exception {
        try (IonReader comparisonResultReader = IonReaderBuilder.standard().build(new BufferedInputStream(new FileInputStream(outputFilePath)))) {
            IonDatagram comparisonResult = ReadGeneralConstraints.LOADER.load(comparisonResultReader);
            IonStruct comparisonResultStruct = (IonStruct) comparisonResult.get(0);
            comparisonResultStruct.remove(RELATIVE_DIFFERENCE_SCORE);
            IonString inputFile = (IonString) comparisonResultStruct.get(INPUT);
            String fileName = inputFile.stringValue();
            Map<String, BigDecimal> regressions = new HashMap<>();
            for (String keyWord : scoreMap.keySet()) {
                if (scoreMap.get(keyWord).compareTo(thresholdMap.get(keyWord)) < 0) {
                    regressions.put(keyWord, scoreMap.get(keyWord));
                }
            }
            if (regressions.size() != 0) {
                return "The performance regression detected when benchmark the ion-java from the new commit with the test data: "
                        + fileName.substring(fileName.lastIndexOf(File.separator) + 1) + " and parameters: " + comparisonResultStruct.get(PARAMETERS) + System.lineSeparator()
                        + "The following aspects have regressions: " + regressions + System.lineSeparator();
            } else {
                // Only regression detected messages are expected, and if no regression detected after executing the current ion-java-benchmark invoke an empty string will be returned.
                return null;
            }
        }
    }
}