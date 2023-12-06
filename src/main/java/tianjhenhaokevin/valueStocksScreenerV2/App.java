package tianjhenhaokevin.valueStocksScreenerV2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import org.apache.commons.lang3.StringUtils;
import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class App implements Runnable {
    private static final String TEMP_FILE_PATH = "output/temp.json";
    private static final String RESULT_FILE_PATH = "output/result.txt";
    private JTextField jTextField;
    private JButton jButton;
    private PrintStream printStream;
    private boolean waitingForInput = false;

    public App() {}

    public App(JTextField jTextField, JButton jButton, PrintStream printStream) {
        this.jTextField = jTextField;
        this.jButton = jButton;
        this.printStream = printStream;
    }

    private String readInput() {
        this.jButton.setEnabled(true);
        waitingForInput = true;

        while (waitingForInput) {
            try {
                Thread.currentThread().sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        String ret = jTextField.getText();
        jTextField.setText("");
        return ret;
    }

    public void inputReady() {
        this.waitingForInput = false;
        this.jButton.setEnabled(false);
    }

    @Override
    public void run() {

        //Load temp file
        List<Quote> quoteList = new ArrayList<>();
        boolean loadedQuoteListFromTemp = false;

        try {
            File tempFile = new File(TEMP_FILE_PATH);

            if(tempFile.exists()) {
                printStream.println("Continue run from previous scan? (enter [no] to cancel) ");
                String input = readInput();
                printStream.println(input);

                if(!input.equalsIgnoreCase("no")) {

                    ObjectMapper objectMapper = new ObjectMapper();
                    String tempFileContent = Files.readString(Paths.get(TEMP_FILE_PATH));
                    quoteList = objectMapper.readValue(tempFileContent, new TypeReference<>() {});

                    loadedQuoteListFromTemp = true;
                }
            }
        } catch (Exception e) {
            printStream.println("Error loading from previous scan, exiting app");
            return;
        }

        if(!loadedQuoteListFromTemp) {
            try {
                quoteList = loadQuoteListFromCsvFile();
            } catch (Exception e) {
                return;
            }
        }

        List<Quote> finalQuoteList = quoteList;

        //Define shutdown hook
        Thread shutdownHook = new Thread(() -> {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String fileContent = objectMapper.writeValueAsString(finalQuoteList);
                Files.write(Paths.get(TEMP_FILE_PATH), fileContent.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {

            }
        });

        //Save quote list on premature shutdown
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        DataAPI dataAPI = new DataAPI();

        try {
            printStream.println("Initializing data connection...");
            dataAPI.init();
        } catch (Exception e) {
            printStream.println("Failed to initialize data connection, exiting app");
            return;
        }

        List<String> unprocessedTickerList = new ArrayList<>();
        int processedCount = 0;
        boolean processedCountChanged = false;

        for (int i = 0; i < finalQuoteList.size(); i++) {
            Quote quote = finalQuoteList.get(i);

            try {
                if (quote.getIntrinsicValue() == null && !quote.isIgnore()) {
                    processedCountChanged = true;
                    processedCount++;

                    Map<String, Object> tickerData = dataAPI.queryData(quote.getTicker());
                    List<Double> epsDataList = (List<Double>)tickerData.get("eps");
                    double[] intrinsicAndGrowth = calculateIntrinsicAndGrowthValue(epsDataList);

                    IntrinsicValue intrinsicValue;

                    if(intrinsicAndGrowth == null) {
                        intrinsicValue = new IntrinsicValue(
                                (String) tickerData.get("currency"),
                                (String) tickerData.get("last_eps_year")
                        );
                    } else {
                        intrinsicValue = new IntrinsicValue(
                                (String) tickerData.get("currency"),
                                intrinsicAndGrowth[0],
                                intrinsicAndGrowth[1],
                                (String) tickerData.get("last_eps_year")
                        );
                    }

                    quote.setIntrinsicValue(intrinsicValue);
                    printStream.println("Successfully processed #" + quote.getRow() + " (" + quote.getTicker() + ")");
                }

            } catch (Exception e) {
                printStream.println("Error processing #" + quote.getRow() + " (" + quote.getTicker() + ")");
                unprocessedTickerList.add(quote.getTicker());

                try {
                    dataAPI.close();
                    dataAPI.init();
                } catch (Exception e2) {}
            }

            try {
                if(processedCountChanged) {

                    if(processedCount % 10 == 0) {
                        dataAPI.close();
                        System.gc();

                        Thread.currentThread().sleep(10000);
                        dataAPI.init();

                    } else if(processedCount % 5 == 0) {
                        System.gc();
                        Thread.currentThread().sleep(5000);

                    } else {
                        Thread.currentThread().sleep(3000);
                    }

                    processedCountChanged = false;
                }

            } catch (Exception e) {}
        }

        dataAPI.close();

        //Run and remove shutdown hook
        shutdownHook.run();
        Runtime.getRuntime().removeShutdownHook(shutdownHook);

        adjustIntrinsicValueCurrency(quoteList);
        printToResultFile(quoteList);

        printStream.println("Result file is generated, exiting app");
    }

    public void adjustIntrinsicValueCurrency(List<Quote> quoteList) {
        Set<String> currencySet = quoteList.stream()
                .filter(quote -> quote.getIntrinsicValue() != null && quote.getIntrinsicValue().getValue() != null)
                .map(quote -> quote.getIntrinsicValue().getCurrency())
                .collect(Collectors.toSet());

        Map<String, Double> exchangeRateMap = new HashMap<>();

        for (String currency : currencySet) {
            printStream.println("Enter rate for 1 " + currency + " to USD");
            String input = readInput();
            printStream.println(input);
            double rate = Double.parseDouble(input);

            exchangeRateMap.put(currency, rate);
        }

        for (Quote quote : quoteList.stream().filter(quote -> quote.getIntrinsicValue() != null &&
                quote.getIntrinsicValue().getValue() != null).collect(Collectors.toList())) {

            quote.getIntrinsicValue().setValue(quote.getIntrinsicValue().getValue() * exchangeRateMap.get(quote.getIntrinsicValue().getCurrency()));
            quote.getIntrinsicValue().setCurrency("USD");
        }
    }

    public List<Quote> loadQuoteListFromCsvFile() throws Exception {
        List<Quote> quoteList = new ArrayList<>();
        String csvFilePath = "input/finviz-scan.csv";
        printStream.println("Loading list from [input/finviz-scan.csv]...");

        try (CSVReader reader = new CSVReader(new FileReader(csvFilePath))) {
            String[] nextLine;

            while ((nextLine = reader.readNext()) != null) {
                List<String> lineItems = Arrays.asList(nextLine);

                if(lineItems.size() < 9 || lineItems.stream().anyMatch(lineItem -> StringUtils.isBlank(lineItem))) {
                    printStream.println("Line " + lineItems + " not loaded due to incorrect format");
                    continue;
                }

                Quote quote = new Quote();
                quote.setRow(lineItems.get(0).trim());
                quote.setTicker(lineItems.get(1).trim());
                quote.setName(lineItems.get(2).trim());
                quote.setSector(lineItems.get(3).trim());
                quote.setIndustry(lineItems.get(4).trim());
                quote.setCountry(lineItems.get(5).trim());

                try {
                    quote.setPrice(Double.parseDouble(lineItems.get(8)
                            .trim()
                            .replace(",", "")));
                } catch (Exception e) {
                    printStream.println("Line " + lineItems + " not loaded due to incorrect format");
                    continue;
                }

                quoteList.add(quote);
            }

            printStream.println("Finished loading quotes from [" + csvFilePath + "]");

        } catch (Exception e){
            printStream.println("Error reading from [" + csvFilePath + "]");
            throw e;
        }

        return quoteList;
    }

    public static double[] calculateIntrinsicAndGrowthValue(List<Double> epsDataList) {

        if (epsDataList.size() < 6) {
            return null;
        }

        if (epsDataList.subList(epsDataList.size() - 6, epsDataList.size()).stream().anyMatch(epsData -> epsData <= 0)) {
            return null;
        }

        for (int i = epsDataList.size() - 6; i >= 0; i--) {
            double iEps = epsDataList.get(i);

            if (iEps <= 0) {
                break;
            }

            int upPeriod = 0;
            double totalPeriod = (epsDataList.size() - 1) - i;
            double peakEps = epsDataList.get(i);

            for (int i2 = i + 1; i2 < epsDataList.size(); i2++) {
                double i2Eps = epsDataList.get(i2);

                if ((i2Eps / peakEps) <= 0.8) {
                    upPeriod = 0;
                    break;
                }

                if (i2Eps > peakEps) {
                    peakEps = i2Eps;
                    upPeriod++;
                }
            }

            if ((upPeriod / totalPeriod) < 0.65) {
                continue;
            }

            //Find the start eps to calculate growth rate
            double pastEps = epsDataList.subList(0, (i+1)).stream().max(Comparator.naturalOrder()).get();
            double presentEps = epsDataList.get(epsDataList.size()-1);

            if (presentEps <= pastEps) {
                return null;
            }

            double[] ret = new double[2];
            double growthRate = Math.pow((presentEps / pastEps), (1.0 / totalPeriod));

            ret[0] = calculateIntrinsicValue(growthRate, presentEps);
            ret[1] = growthRate;
            return ret;
        }

        return null;
    }

    static double calculateIntrinsicValue(double growthRate, double presentEps) {
        double adjustedGrowthRate = (growthRate - 1) / 2;

        if(adjustedGrowthRate > 0.15) {
            adjustedGrowthRate = 0.15;
        }

        adjustedGrowthRate = (adjustedGrowthRate + 1) / 1.1;

        double growthPeriodValue = 0;
        double yrByYrEps = presentEps;

        for (int i2 = 0; i2 < 10; i2++) {
            yrByYrEps = yrByYrEps * adjustedGrowthRate;
            growthPeriodValue += yrByYrEps;
        }

        double stagnantGrowthRate = 1.02 / 1.1;
        double stagnantPeriodValue = 0;

        for (int i2 = 0; i2 < 10; i2++) {
            yrByYrEps = yrByYrEps * stagnantGrowthRate;
            stagnantPeriodValue += yrByYrEps;
        }

        double intrinsicValue = (growthPeriodValue + stagnantPeriodValue) * 0.7;
        return intrinsicValue;
    }

    static void printToResultFile(List<Quote> quoteList) {
        int sectorNamePad = quoteList.stream().map(quote -> quote.getSector().length())
                .max(Comparator.naturalOrder()).get() + 5;
        int industryNamePad = quoteList.stream().map(quote -> quote.getIndustry().length())
                .max(Comparator.naturalOrder()).get() + 5;

        StringBuilder outputBuilder = getStringBuilder(sectorNamePad, industryNamePad);

        Comparator<Quote> comparator = Comparator.comparing(quote -> quote.getSector());
        comparator = comparator.thenComparing(quote -> {
            double priceIntrinsicRatio = quote.getPrice() / quote.getIntrinsicValue().getValue();

            if(priceIntrinsicRatio <= 1) {
                return 1;
            } else if(priceIntrinsicRatio <= 1.1) {
                return 2;
            } else {
                return 3;
            }
        });

        comparator = comparator.thenComparing(quote -> quote.getIntrinsicValue().getGrowthRate(), Comparator.reverseOrder());

        List<Quote> sortedList = quoteList.stream()
                .filter(quote -> quote.getIntrinsicValue() != null && quote.getIntrinsicValue().getValue() != null)
                .sorted(comparator).collect(Collectors.toList());

        outputBuilder.append("\n");
        int numbering = 1;

        for (Quote quote : sortedList) {
            double growthRate = (quote.getIntrinsicValue().getGrowthRate() - 1) * 100;
            String growthRateStr = String.format("%.2f", growthRate) + "%";

            double priceToIntrinsic = (quote.getPrice() / quote.getIntrinsicValue().getValue()) * 100;
            String priceToIntrinsicStr = String.format("%.2f", priceToIntrinsic) + "%";

            String priceStr = "$" + String.format("%.2f", quote.getPrice());
            String intrinsicStr = "$" + String.format("%.2f", quote.getIntrinsicValue().getValue());

            outputBuilder.append(String.format("%-10s", numbering++));
            outputBuilder.append(String.format("%-" + sectorNamePad + "s", quote.getSector()));
            outputBuilder.append(String.format("%-" + industryNamePad + "s", quote.getIndustry()));
            outputBuilder.append(String.format("%-15s", quote.getTicker()));
            outputBuilder.append(String.format("%-15s", growthRateStr));
            outputBuilder.append(String.format("%-20s", priceToIntrinsicStr));
            outputBuilder.append(String.format("%-15s", priceStr));
            outputBuilder.append(String.format("%-15s", intrinsicStr));

            outputBuilder.append("\n");
        }

        try {
            Files.write(Paths.get(RESULT_FILE_PATH), outputBuilder.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static StringBuilder getStringBuilder(int sectorNamelPad, int industryNamePad) {
        StringBuilder outputBuilder = new StringBuilder();

        outputBuilder.append(String.format("%-10s", "No"));
        outputBuilder.append(String.format("%-" + sectorNamelPad + "s", "Sector"));
        outputBuilder.append(String.format("%-" + industryNamePad + "s", "Industry"));
        outputBuilder.append(String.format("%-15s", "Ticker"));
        outputBuilder.append(String.format("%-15s", "Growth %"));
        outputBuilder.append(String.format("%-20s", "Price/Intrinsic"));
        outputBuilder.append(String.format("%-15s", "Price"));
        outputBuilder.append(String.format("%-15s", "Intrinsic"));

        String lineSeparator = "=".repeat(90 + sectorNamelPad + industryNamePad);

        outputBuilder.append("\n");
        outputBuilder.append(lineSeparator);
        return outputBuilder;
    }
}