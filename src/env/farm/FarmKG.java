package farm;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class FarmKG extends Artifact {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";

    private String repoLocation;

    private static String PREFIXES = "PREFIX was: <https://was-course.interactions.ics.unisg.ch/farm-ontology#>\n" +
                                     "PREFIX hmas: <https://purl.org/hmas/>\n" +
                                     "PREFIX td: <https://www.w3.org/2019/wot/td#>\n";

    public void init(String repoLocation) {
        this.repoLocation = repoLocation;
    }

    @OPERATION
    public void queryFarm(OpFeedbackParam<String> farm) {
        // Build a query that selects a farm whose IRI starts with the dynamic prefix, e.g. "http://localhost:7200/andreas-baumann#farm-"
        String queryStr = PREFIXES +
            "SELECT ?farm WHERE { " +
            "  ?farm a was:Farm . " +
            "  FILTER(STRSTARTS(STR(?farm), \"http://localhost:7200/andreas-baumann#farm-\")) " +
            "} LIMIT 1";
        JsonArray farmBindings = executeQuery(queryStr);
        if (farmBindings.size() > 0) {
            JsonObject firstBinding = farmBindings.get(0).getAsJsonObject();
            JsonObject farmBinding = firstBinding.getAsJsonObject("farm");
            String farmValue = farmBinding.getAsJsonPrimitive("value").getAsString();
            farm.set(farmValue);
        } else {
            farm.set(null);
        }
    }
    @OPERATION 
    public void queryThing(String farm, String offeredAffordance, OpFeedbackParam<String> thingDescription) {
        String tdValue = null;
        String tdVariableName = "td";
        // Use hmas:contains since your farm data uses that property to link tractors.
        String queryStr = PREFIXES +
            "SELECT ?" + tdVariableName + " WHERE {\n" + 
            "  <" + farm + "> <https://purl.org/hmas/contains> ?thing.\n" +
            "  ?thing td:hasActionAffordance ?aff.\n" +
            "  ?thing hmas:hasProfile ?" + tdVariableName + ".\n" +
            "  ?aff a <" + offeredAffordance +">.\n" +
            "} LIMIT 1";
        JsonArray tdBindings = executeQuery(queryStr);
        if (tdBindings.size() > 0) {
            JsonObject firstBinding = tdBindings.get(0).getAsJsonObject();
            JsonObject tdBinding = firstBinding.getAsJsonObject(tdVariableName);
            tdValue = tdBinding.getAsJsonPrimitive("value").getAsString();
            thingDescription.set(tdValue);
        } else {
            thingDescription.set("Not found");
        }
    }

    // ======= Modified Methods for Task 2.3 =======

    // q4: Query land sections in a given farm.
    @OPERATION
    public void queryFarmSections(String farm, OpFeedbackParam<Object[]> sections) {
        List<String> sectionList = new ArrayList<>();
        String queryStr = PREFIXES + "SELECT ?section WHERE { <" + farm + "> was:hasLandSection ?section }";
        JsonArray bindings = executeQuery(queryStr);
        for (int i = 0; i < bindings.size(); i++) {
            JsonObject binding = bindings.get(i).getAsJsonObject();
            JsonObject sectionObj = binding.getAsJsonObject("section");
            if (sectionObj != null) {
                sectionList.add(sectionObj.getAsJsonPrimitive("value").getAsString());
            }
        }
        sections.set(sectionList.toArray(new Object[0]));
    }

    // q5: Query the coordinates [X1, Y1, X2, Y2] for a given land section.
    @OPERATION
    public void querySectionCoordinates(String section, OpFeedbackParam<Object[]> coordinates) {
        String queryStr = PREFIXES +
            "SELECT ?x1 ?y1 ?x2 ?y2 WHERE { " +
            "  <" + section + "> was:hasCoordinates ?coord . " +
            "  ?coord was:hasX1 ?x1 ; " +
            "         was:hasY1 ?y1 ; " +
            "         was:hasX2 ?x2 ; " +
            "         was:hasY2 ?y2 . " +
            "}";
        JsonArray bindings = executeQuery(queryStr);
        if (bindings.size() > 0) {
            JsonObject binding = bindings.get(0).getAsJsonObject();
            float x1 = binding.getAsJsonObject("x1").getAsJsonPrimitive("value").getAsFloat();
            float y1 = binding.getAsJsonObject("y1").getAsJsonPrimitive("value").getAsFloat();
            float x2 = binding.getAsJsonObject("x2").getAsJsonPrimitive("value").getAsFloat();
            float y2 = binding.getAsJsonObject("y2").getAsJsonPrimitive("value").getAsFloat();
            coordinates.set(new Object[] { x1, y1, x2, y2 });
        } else {
            coordinates.set(new Object[] {});
        }
    }

    // q6: Query the crop growing in a given land section.
    @OPERATION 
    public void queryCropOfSection(String section, OpFeedbackParam<String> crop) {
        String queryStr = PREFIXES + "SELECT ?crop WHERE { <" + section + "> was:hasCrop ?crop }";
        JsonArray bindings = executeQuery(queryStr);
        if (bindings.size() > 0) {
            JsonObject binding = bindings.get(0).getAsJsonObject();
            JsonObject cropObj = binding.getAsJsonObject("crop");
            if (cropObj != null) {
                crop.set(cropObj.getAsJsonPrimitive("value").getAsString());
            } else {
                crop.set("not found");
            }
        } else {
            crop.set("not found");
        }
    }

    // q7: Query the required moisture level of a given crop.
    @OPERATION
    public void queryRequiredMoisture(String crop, OpFeedbackParam<Integer> level) {
        String queryStr = PREFIXES + "SELECT ?moisture WHERE { <" + crop + "> was:hasMoistureRequirement ?moisture }";
        JsonArray bindings = executeQuery(queryStr);
        if (bindings.size() > 0) {
            JsonObject binding = bindings.get(0).getAsJsonObject();
            JsonObject moistureObj = binding.getAsJsonObject("moisture");
            if (moistureObj != null) {
                // Assuming the moisture level is an integer value (e.g., 120)
                int moistureValue = moistureObj.getAsJsonPrimitive("value").getAsInt();
                level.set(moistureValue);
            } else {
                level.set(0);
            }
        } else {
            level.set(0);
        }
    }

    // =================================================

    private JsonArray executeQuery(String queryStr) {
        String queryUrl = this.repoLocation + "?query=" + URLEncoder.encode(queryStr, StandardCharsets.UTF_8);
        try {
            URI uri = new URI(queryUrl);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                // Authentication code can be re-enabled as needed.
                .header("Accept", "application/sparql-results+json")
                .GET()
                .build();
             
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("HTTP error code : " + response.statusCode());
            }
            String responseBody = response.body();
            JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
            JsonArray bindingsArray = jsonObject.getAsJsonObject("results").getAsJsonArray("bindings");
            return bindingsArray;
        } catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return new JsonArray();
    }
}
