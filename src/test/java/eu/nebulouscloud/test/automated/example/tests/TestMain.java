package eu.nebulouscloud.test.automated.example.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nebulouscloud.util.StringToMapParser;

import java.util.Map;

public class TestMain {
    public static ObjectMapper objectMapper = new ObjectMapper();
    public static void main(String[] args) throws Exception {
        String string = "{when=2024-08-29T17:55:15.385688392Z, metaData={user=admin}, body={\"name\":\"20545-14\",\"master-node\":\"m20545-14-master\",\"nodes\":[{\"nodeName\":\"m20545-14-master\",\"nodeCandidateId\":\"8a748406919de61601919de8a0f5000c\",\"cloudId\":\"c9a625c7-f705-4128-948f-6b5765509029\"},{\"nodeName\":\"n20545-14-dummy-app-worker-1-1\",\"nodeCandidateId\":\"8a748406919de61601919de8a0f5000c\",\"cloudId\":\"c9a625c7-f705-4128-948f-6b5765509029\"},{\"nodeName\":\"n20545-14-dummy-app-worker-1-2\",\"nodeCandidateId\":\"8a748406919de61601919de8a0f5000c\",\"cloudId\":\"c9a625c7-f705-4128-948f-6b5765509029\"}],\"env-var\":{\"APPLICATION_ID\":\"2054552908automated-testing-mqtt-app-1724954095172\",\"BROKER_ADDRESS\":\"158.37.63.86\",\"ACTIVEMQ_HOST\":\"158.37.63.86\",\"BROKER_PORT\":\"32754\",\"ACTIVEMQ_PORT\":\"32754\",\"ONM_IP\":\"158.39.201.249\",\"ONM_URL\":\"http://158.37.63.36:8082\",\"PRIVATE_DOCKER_REGISTRY_SERVER\":\"docker.io\",\"PRIVATE_DOCKER_REGISTRY_USERNAME\":\"rsprat\",\"PRIVATE_DOCKER_REGISTRY_PASSWORD\":\"G9A1NbvTYA\",\"PRIVATE_DOCKER_REGISTRY_EMAIL\":\"rprat@gmail.com\"}}}";

        StringToMapParser parser = new StringToMapParser();
        // Parse the input string into a map
        Map<String, Object> result = parser.parseStringToMap(string);

        // Print the result
        Object bodyObject = result.get("body");
        Map<String, Object> bodyMap = (Map<String, Object>) bodyObject;
        Object nameObject = bodyMap.get("name");
        String name = (String) nameObject;
        System.out.println("Extracted name: " + name);


    }


}
