package eu.nebulouscloud.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import eu.nebulouscloud.exceptions.InvalidFormatException;

import java.util.HashMap;
import java.util.Map;

public class StringToMapParser  {
    private final Gson gson;

    public StringToMapParser() {
        this.gson = new Gson();
    }

    /**
     * Parses the input string into a Map<String, Object>
     *
     * @param input the input string to parse
     * @return a map representing the parsed input
     */
    public Map<String, Object> parseStringToMap(String input) throws InvalidFormatException {
        Map<String, Object> result = new HashMap<>();

        try {
            // Remove leading and trailing curly braces
            input = input.substring(1, input.length() - 1);

            // Split the input string by commas outside the JSON-like parts
            String[] parts = input.split(", (?=\\w+=)");

            for (String part : parts) {
                String[] keyValue = part.split("=", 2);
                if (keyValue.length != 2) {
                    throw new InvalidFormatException("Invalid key-value pair format: " + part);
                }

                String key = keyValue[0].trim();
                String value = keyValue[1].trim();

                // Try to parse the value as JSON
                Object parsedValue = tryParseJson(value);

                // Add to the result map
                result.put(key, parsedValue);
            }

        } catch (StringIndexOutOfBoundsException | JsonSyntaxException e) {
            throw new InvalidFormatException("Failed to parse input string", e);
        }

        return result;
    }

    /**
     * Tries to parse the input value as JSON and returns the appropriate object
     *
     * @param value the value to attempt to parse
     * @return a parsed object, either a JSON structure or a plain string
     */
    private Object tryParseJson(String value) {
        // Check if the value is JSON-like
        if (value.startsWith("{") && value.endsWith("}")) {
            try {
                // Attempt to parse as JSON
                JsonElement jsonElement = JsonParser.parseString(value);
                if (jsonElement.isJsonObject()) {
                    // If it's a JSON object, return it as a Map
                    return gson.fromJson(jsonElement, Map.class);
                } else if (jsonElement.isJsonArray()) {
                    // If it's a JSON array, return it as-is
                    return jsonElement;
                }
            } catch (JsonSyntaxException e) {
                // Not a valid JSON, return as string
            }
        }

        // If parsing failed or not a JSON, return the original string value
        return value;
    }
}