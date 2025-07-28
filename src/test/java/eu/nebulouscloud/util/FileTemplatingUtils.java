package eu.nebulouscloud.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;


public class FileTemplatingUtils {
    static Logger LOGGER = LoggerFactory.getLogger(FileTemplatingUtils.class);
    static ObjectMapper om = new ObjectMapper();

    /**
     * Load a JSON file and perform the substitutions.
     * @param path Path to JSON file
     * @param substitutions Map of placeholders and values
     * @return Parsed JSON object as a Map
     * @throws Exception
     */
    public static Map<String, Object> loadJSONFileAndSubstitute(String path, Map<String, String> substitutions) throws Exception {
        String jsonContent = loadFileAndSubstitute(path, substitutions);
        return om.readValue(jsonContent, HashMap.class);
    }

    /**
     * Load a JSON array file and perform the substitutions.
     *
     * @param path          Path to the JSON array file.
     * @param substitutions Map of placeholders and corresponding replacement values.
     * @return Parsed JSON array as a List of Maps.
     * @throws Exception If file reading or JSON parsing fails.
     */
    public static List<Map<String, Object>> loadJSONArrayFileAndSubstitute(String path, Map<String, String> substitutions) throws Exception {
        String jsonContent = loadFileAndSubstitute(path, substitutions);
        return om.readValue(jsonContent, new TypeReference<List<Map<String, Object>>>() {
        });
    }

    

    /**
     * Load a JSON file and perform substitutions, returning the JSON as a string.
     * @param path Path to JSON file
     * @param substitutions Map of placeholders and values
     * @return JSON string with placeholders replaced
     * @throws Exception If file reading fails
     */
    public static String loadJSONFileAndSubstituteAsString(String path, Map<String, String> substitutions) throws Exception {
        return loadFileAndSubstitute(path, substitutions);
    }



    /**
     * Load a text file and perform the substitutions.
     * @param path Path to the file
     * @param substitutions Map of placeholders and values
     * @return The modified file content as a String
     * @throws Exception
     */
    public static String loadFileAndSubstitute(String path, Map<String, String> substitutions) throws Exception {
        LOGGER.info("Loading file: " + path);
        InputStream inputStream = FileTemplatingUtils.class.getClassLoader().getResourceAsStream(path);

        if (inputStream == null) {
            throw new FileNotFoundException("File not found: " + path);
        }

        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (substitutions != null) line = applySubstitutions(line, substitutions);
                contentBuilder.append(line).append("\n");
            }
        }
        return contentBuilder.toString();
    }

    /**
     * Replaces placeholders in the given line.
     * @param line The input line
     * @param substitutions Map of placeholder-value pairs
     * @return Modified line
     */
    private static String applySubstitutions(String line, Map<String, String> substitutions) {
        for (Map.Entry<String, String> entry : substitutions.entrySet()) {
            line = line.replace(entry.getKey(), entry.getValue());
        }
        return line;
    }

    /**
     * Reads a private/public key from a .pem file and ensures it is JSON-safe.
     * @param keyFilePath Path to the .pem file
     * @return The key content as a JSON-escaped string
     * @throws Exception If file reading fails
     */
    public static String loadKeyFromFile(String keyFilePath) throws Exception {
        try {
            LOGGER.info("Loading key from: " + keyFilePath);

            if (!Files.exists(Paths.get(keyFilePath))) {
                throw new FileNotFoundException("Key file not found: " + keyFilePath);
            }

            // Read file and escape newlines
            String key = new String(Files.readAllBytes(Paths.get(keyFilePath))).trim();

            return key.replace("\n", "\\n"); //  Escape newlines properly
        } catch (Exception ex) {
            throw new Exception("Error reading key file: " + keyFilePath, ex);
        }
    }

}
