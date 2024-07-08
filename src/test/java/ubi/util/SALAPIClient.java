package ubi.util;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import kong.unirest.core.json.JSONArray;


public class SALAPIClient {

    public SALAPIClient(String baseURL,String user,String password)
    {
        Unirest.config().defaultBaseUrl(baseURL);
        HttpResponse<String> response = Unirest.post("/pagateway/connect")
                .multiPartContent()
                .field("username", user)
                .field("password", password)
                .asString();
        if(!response.isSuccess())
        {
            throw new RuntimeException("Couldn't login to SAL\n"+response.toString());
        }

        Unirest.config().addDefaultHeader("sessionid", response.getBody());
    }

    public JSONArray getAllClouds()
    {
        return Unirest.get("/cloud").asJson().getBody().getArray();
    }

    public boolean isCloudRegistrationOngoing()
    {
        String ret = Unirest.get("/cloud/async").asString().getBody();
        if(ret.equals("false")) {return false;}
        if(ret.equals("true")) {return true;}
        throw new RuntimeException("Unknown status: "+ret);
    }
}
