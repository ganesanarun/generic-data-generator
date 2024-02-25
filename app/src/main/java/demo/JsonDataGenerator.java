package demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.instancio.Assignment;
import org.instancio.Instancio;
import org.instancio.InstancioApi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.instancio.Assign.given;
import static org.instancio.Select.all;
import static org.instancio.Select.field;

public class JsonDataGenerator {

    record Result<T>(T data, int index) {

    }

    public static void main(String[] args) {
        try {
            int numOfRecords = 1_000;
            String outputDir = "../generated_data";

            var schema = getSchemaForStockUpdateRequest(numOfRecords);
            var data = generateDataFor(schema, numOfRecords);
            saveThenAsJson(data, outputDir);

        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private static InstancioApi<StockUpdateRequest> getSchemaForStockUpdateRequest(int numOfRecords) {
        String[] systems = new String[]{"PARIS_CL"};
        String[] parisNodeCodes = new String[]{"123", "234", "123"};
        Item.UpdateType[] updateTypes = new Item.UpdateType[]{Item.UpdateType.ABSOLUTE};
        var itemIds = Instancio.ofList(String.class).size(numOfRecords / 4).create();

        var assignments = new Assignment[]{
                given(field(Item::getUpdateType))
                        .is(Item.UpdateType.ABSOLUTE)
                        .set(field(Item::getAdjustmentType), null),
                given(field(Quantity::getUnitOfMeasure))
                        .is(Quantity.UnitOfMeasure.UNITS)
                        .generate(field(Quantity::getValue), generators -> generators.ints().range(1, 100).as(BigDecimal::new)),
                given(field(StockUpdateRequest::getSystem))
                        .is("PARIS_CL")
                        .generate(field(Item::getNodeCode), generators -> generators.oneOf(parisNodeCodes))
        };
        return Instancio.of(StockUpdateRequest.class)
                .generate(field(StockUpdateRequest::getSystem), gen -> gen.oneOf(systems))
                .generate(field(Item::getId), gen -> gen.oneOf(itemIds))
                .generate(field(Item::getUpdateType), gen -> gen.oneOf(updateTypes))
                .supply(all(ZonedDateTime.class), () -> ZonedDateTime.now())
                .assign(assignments);
    }

    private static <T> Stream<Result<T>> generateDataFor(InstancioApi<T> model, int numRecords) throws IOException {
        return IntStream.range(1, numRecords + 1)
                .boxed()
                .parallel()
                .map(i -> new Result<T>(model.create(), i));
    }

    private static void makeDirectory(String outputDir) {
        new File(outputDir).mkdirs();
    }

    private static <T> void saveThenAsJson(Stream<Result<T>> data, String outputDir) {
        makeDirectory(outputDir);
        data.forEach(result -> saveDataToFile(result.data(), outputDir + "/data_" + result.index() + ".json"));
    }

    private static <T> void saveThemAsCSV(Stream<Result<T>> data, String outputDir) {
        makeDirectory(outputDir);
        String csvFilePath = outputDir + "/output.csv";
        try {
            FileWriter outFile = new FileWriter(csvFilePath);
            CSVWriter writer = new CSVWriter(outFile);
            StatefulBeanToCsv<T> build = new StatefulBeanToCsvBuilder<T>(writer)
                    .withApplyQuotesToAll(true)
                    .build();
            List<T> result = data.map(t -> t.data).toList();
            build.write(result);
        } catch (IOException | CsvRequiredFieldEmptyException | CsvDataTypeMismatchException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> void saveDataToFile(T jsonData, String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
            mapper.setDateFormat(new SimpleDateFormat());
            mapper.registerModule(new JavaTimeModule());
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), jsonData);
        } catch (IOException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }
}
