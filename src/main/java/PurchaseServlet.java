import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import java.util.UUID;

@WebServlet(name = "PurchaseServlet")
public class PurchaseServlet extends HttpServlet {
//  private PurchaseDao liftRideDao;
  private Table table;

  public PurchaseServlet() {
//    this.liftRideDao = new PurchaseDao();
    AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
        .withRegion(Regions.US_EAST_1).withCredentials(new InstanceProfileCredentialsProvider(false))
        .build();
    DynamoDB dynamoDB = new DynamoDB(client);

    String tableName = "Purchases1";
    try {
      table = dynamoDB.createTable(tableName,
          Arrays.asList(new KeySchemaElement("purchaseID", KeyType.HASH)),
          Arrays.asList(new AttributeDefinition("purchaseID", ScalarAttributeType.S)),
          new ProvisionedThroughput(10L, 3000L));
      table.waitForActive();
    }
    catch (Exception e) {
      System.err.println("Unable to create table: ");
      System.err.println(e.getMessage());
    }
    table = dynamoDB.getTable(tableName);
  }

  protected void doPost(HttpServletRequest request,
      HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("application/json");

    // Gets the http request url path information
    String urlPath = request.getPathInfo();
    // Gets the http request body reader
    BufferedReader bodyReader = request.getReader();
    // Reads the http request body
    StringBuilder bodyBuilder = new StringBuilder();
    String line = null;
    while ((line = bodyReader.readLine()) != null) {
      line = line.strip();
      bodyBuilder.append(line);
    }
    String body = bodyBuilder.toString();

    // Validates the url path information
    if (urlPath == null || urlPath.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.getWriter().write("{\"message\": \"Missing parameters!\"}");
      return;
    }
    String[] urlParts = urlPath.split("/");
    if (!isUrlValid(urlParts)) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("{\"message\": \"Invalid url!\"}");
    // Validates the body
    }  else if (!isBodyValid(body)) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("{\"message\": \"Invalid body\"})");
    // The request is valid!!!
    } else {
//      liftRideDao.createPurchase(new Purchase(Integer.parseInt(urlParts[1]),
//                                              Integer.parseInt(urlParts[3]),
//                                              urlParts[5],
//                                              body));
      final String purchaseID = UUID.randomUUID().toString();
      int storeID = Integer.parseInt(urlParts[1]);
      int custID = Integer.parseInt(urlParts[3]);
      String date = urlParts[5];
      String items = body;

      final Map<String, Object> infoMap = new HashMap<String, Object>();
      infoMap.put("storeID", storeID);
      infoMap.put("custID", custID);
      infoMap.put("date", date);
      infoMap.put("items", items);

      try {
        PutItemOutcome outcome = table
            .putItem(new Item().withPrimaryKey("purchaseID", purchaseID).withMap("infos", infoMap));
      }
      catch (Exception e) {
        System.err.println("Unable to add purchase item");
        System.err.println(e.getMessage());
      }

      response.setStatus(HttpServletResponse.SC_CREATED);
      response.getWriter().write("{\"message\": \"It works!\"}");
    }
  }

  protected void doGet(HttpServletRequest request,
      HttpServletResponse response)
      throws ServletException, IOException {
  }

  private boolean isUrlValid(String[] urlPath) {
    // urlPath  = "{storeID}/customer/{custID}/date/{date}"
    // urlParts = [, {storeID}, customer, {custID}, date, {date}]
    if (urlPath.length != 6) return false;
    if (!urlPath[2].equals("customer") || !urlPath[4].equals("date")) return false;

    String storeID = urlPath[1];
    String custID = urlPath[3];
    String date = urlPath[5];
    if (!is32Int(storeID) || !is32Int(custID)) return false;
    if (!isValidDate(date)) return false;
    return true;
  }

  private boolean is32Int(String s) {
    try {
      Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return false;
    }
    return true;
  }

  private boolean isValidDate(String s) {
    DateTimeFormatter dateFormatter = DateTimeFormatter.BASIC_ISO_DATE;
    try {
      LocalDate.parse(s, dateFormatter);
    } catch (DateTimeParseException e) {
      return false;
    }
    return true;
  }

  private boolean isBodyValid(String body) throws IOException {
    // Purchase Json Schema
    InputStream purchaseSchemaInputStream = this.getClass().getResourceAsStream("/PurchaseSchema.json");
    JSONTokener purchaseSchemaData = new JSONTokener(purchaseSchemaInputStream);
    JSONObject purchaseJsonSchema = new JSONObject(purchaseSchemaData);

    // Parses the string json into json object in java
    JSONObject bodyJson = new JSONObject(body);

    // Validates purchase schema
    Schema purchaseSchemaValidator = SchemaLoader.load(purchaseJsonSchema);
    try {
      purchaseSchemaValidator.validate(bodyJson);
    } catch (ValidationException ve) {
      return false;
    }

    // Purchase Item Schema
    InputStream purchaseItemSchemaInputStream = this.getClass().getResourceAsStream("/PurchaseItemSchema.json");
    JSONTokener purchaseItemSchemaData = new JSONTokener(purchaseItemSchemaInputStream);
    JSONObject purchaseItemJsonSchema = new JSONObject(purchaseItemSchemaData);

    // Gets an json array of purchase items in java
    JSONArray items = bodyJson.getJSONArray("items");

    // Validates PurchaseItem schema
    Schema purchaseItemSchemaValidator = SchemaLoader.load(purchaseItemJsonSchema);
    for (Object i: items) {
      JSONObject item = (JSONObject) i;
      try {
        purchaseItemSchemaValidator.validate(item);
      } catch (ValidationException ve) {
        return false;
      }
    }
    return true;
  }
}