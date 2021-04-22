import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

@WebServlet(name = "PurchaseServlet")
public class PurchaseServlet extends HttpServlet {
  private PurchaseDao liftRideDao;

  public PurchaseServlet() {
    this.liftRideDao = new PurchaseDao();
  }

  protected void doPost(HttpServletRequest request,
      HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("application/json");
    String urlPath = request.getPathInfo();
    BufferedReader bodyReader = request.getReader();
    StringBuilder bodyBuilder = new StringBuilder();
    String line = null;
    while ((line = bodyReader.readLine()) != null) {
      line = line.strip();
      bodyBuilder.append(line);
    }
    String body = bodyBuilder.toString();

    if (urlPath == null || urlPath.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.getWriter().write("{\"message\": \"Missing parameters!\"}");
      return;
    }

    String[] urlParts = urlPath.split("/");
    if (!isUrlValid(urlParts)) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("{\"message\": \"Invalid url!\"}");
    }  else if (!isBodyValid(body)) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("{\"message\": \"Invalid body\"})");
    } else {
      response.setStatus(HttpServletResponse.SC_CREATED);
      liftRideDao.createPurchase(new Purchase(Integer.parseInt(urlParts[1]),
                                              Integer.parseInt(urlParts[3]),
                                              urlParts[5],
                                              body));
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