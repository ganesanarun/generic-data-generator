package demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.instancio.Instancio;
import org.instancio.InstancioApi;

import java.io.File;
import java.io.IOException;
import java.util.stream.IntStream;

import static org.instancio.Select.field;

public class JsonDataGenerator {

    record Result<T>(T data, int index) {

    }

    public static void main(String[] args) {
        try {
            int numRecords = 10;
            String[] countries = new String[]{"USA", "Canada", "UK", "Australia"};
            String[] types = new String[]{"home", "work", "mobile"};
            final var schema = Instancio.of(demo.Schema.class).generate(field(demo.Schema::getCountry), gen -> gen.oneOf(countries)).generate(field(demo.Schema::getType), gen -> gen.oneOf(types)).ignore(field(demo.Schema::getId));
            generateAndSaveRandomData(schema, numRecords);
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private static <T> void generateAndSaveRandomData(InstancioApi<T> model, int numRecords) throws IOException {
        String outputDir = "generated_data";
        new File(outputDir).mkdirs();
        IntStream.range(1, numRecords)
                .boxed()
                .parallel().map(i -> new Result<T>(generateRandomData(model), i)).forEach(result -> saveDataToFile(result.data(), outputDir + "/data_" + result.index() + ".json"));
    }

    private static <T> T generateRandomData(InstancioApi<T> model) {
        return model.create();
    }

    private static <T> void saveDataToFile(T jsonData, String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), jsonData);
        } catch (IOException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }
}
