package eu.nebulouscloud.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;


public class FileTemplatingUtils {
    static Logger LOGGER = LoggerFactory.getLogger(FileTemplatingUtils.class);
    static ObjectMapper om = new ObjectMapper();

    /**
     * Load a JSON file stored in the resources folder of the project and perform the substitutions provided.
     * @param path
     * @param substitutions
     * @return
     * @throws Exception
     */
    public static Map<String,Object> loadJSONFileAndSubstitute(String path,Map<String,String> substitutions) throws Exception
    {
        return om.readValue(loadFileAndSubstitute(path, substitutions),HashMap.class);
    }

    /**
     * Load a text file stored in the resources folder of the project and perform the substitutions provided.
     * @param path
     * @param substitutions
     * @return
     * @throws Exception
     */
    public static String loadFileAndSubstitute(String path,Map<String,String> substitutions) throws Exception
    {
        LOGGER.info(path);
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(FileTemplatingUtils.class.getClassLoader()
                .getResourceAsStream(path)))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Apply substitutions
                if(substitutions!=null)line = applySubstitutions(line, substitutions);
                contentBuilder.append(line).append("\n");
            }
        }
        catch(Exception ex)
        {
            throw new Exception(ex);
        }
        return contentBuilder.toString();

    }

    /**
     * Find any placeholders in the given line and substitute them with the appropriate value
     * @param line
     * @param substitutions
     * @return
     */
    private static String applySubstitutions(String line, Map<String, String> substitutions) {

        for (Map.Entry<String, String> entry : substitutions.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            line = line.replace(key, value);
        }
        return line;
    }




}
