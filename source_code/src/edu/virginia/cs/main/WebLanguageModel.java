/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author wua4nw
 */
public class WebLanguageModel {

    private static final String API_KEY = "";
    private static final String REQUEST_URL_PREFIX = "https://westus.api.cognitive.microsoft.com/text/weblm/v1.0/calculateJointProbability";

    public static String getJointProbability(String query) {
        HttpClient httpclient = HttpClients.createDefault();
        String value = "";
        try {
            URIBuilder builder = new URIBuilder(REQUEST_URL_PREFIX);

            builder.setParameter("model", "query");
            builder.setParameter("order", "2");

            URI uri = builder.build();
            HttpPost request = new HttpPost(uri);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Ocp-Apim-Subscription-Key", API_KEY);

            // Request body
            String query_json = "{\n"
                    + "	\"queries\":\n"
                    + "	[\n"
                    + "	\"" + query + "\"\n"
                    + "	]\n"
                    + "}";
            StringEntity reqEntity = new StringEntity(query_json);
            request.setEntity(reqEntity);

            HttpResponse response = httpclient.execute(request);
            HttpEntity entity = response.getEntity();

            //close connection. We got what we need.
            if (entity != null) {
                String sb = EntityUtils.toString(entity);

                //find the start index of results number, skip 12 for "WebTotal":"    
                int find = sb.indexOf("\"probability\":");
                int startindex = find + 14;

                //find the last index of results number
                int lastindex = sb.length() - 3; //find the last index of number

                //get the String of number we need 
                value = sb.substring(startindex, lastindex);
            }
        } catch (URISyntaxException | IOException | ParseException e) {
            System.out.println(e.getMessage());
        }
        return value;
    }

    public static void processQueries(String filepath, String savepath) {
        HashMap<String, Double> dictionary = new HashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filepath));
            BufferedWriter writer = new BufferedWriter(new FileWriter(savepath));

            //Process line, put probability into hashMap, and write results into file
            String line;
            while ((line = reader.readLine()) != null) {
                String[] queries = line.split(",");
                //Input the key and count into local dictionary
                for (int i = 0; i < queries.length; i++) {
                    if (!dictionary.containsKey(queries[i])) {
                        String prob = getJointProbability(queries[i]);
                        dictionary.put(queries[i], Double.parseDouble(prob));
                    }
                    if (i == 0) {
                        writer.write(queries[i] + "," + dictionary.get(queries[i]));
                    } else {
                        writer.write("," + queries[i] + "," + dictionary.get(queries[i]));
                    }
                }
                writer.write("\n");
            }
            reader.close();
            writer.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(WebLanguageModel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WebLanguageModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
