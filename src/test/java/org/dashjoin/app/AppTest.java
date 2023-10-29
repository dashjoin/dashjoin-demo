package org.dashjoin.app;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.dashjoin.jsonata.Functions;
import com.dashjoin.jsonata.Jsonata;
import com.dashjoin.jsonata.Jsonata.JFunction;
import com.dashjoin.jsonata.Jsonata.JFunctionCallable;
import com.dashjoin.jsonata.json.Json;

/**
 * Perform some checks on the App's JSON config
 */
@SuppressWarnings("unchecked")
public class AppTest {

  /**
   * pseudo custom function used to run expressions
   */
  JFunction pseudoImpl = new JFunction(new JFunctionCallable() {
    @Override
    public Object call(Object input, @SuppressWarnings("rawtypes") List args) throws Throwable {
      return null;
    }
  }, null);

  /**
   * allowed values for widget field
   */
  List<String> allowedWidget = Arrays.asList("button", "links", "dj-toolbar", "dj-table-metadata",
      "chart", "card", "expansion", "edit", "all", "create", "container", "grid", "display",
      "${pk1}", "text", "activity-status", "upload", "icon", "spacer", "layout-edit-switch",
      "search", "search-result", "toolbar", "table", "queryeditor", "editRelated", "markdown",
      "page", "tree", "variable", "sidenav-switch", "map", "html", "notebook", "diagram",
      /* json schema form */ "textarea", "select", "date");

  /**
   * dashjoin JSONata functions
   */
  List<String> functions =
      Arrays.asList("all", "read", "create", "update", "traverse", "delete", "query", "queryGraph",
          "call", "incoming", "trigger", "djRoles", "djVersion", "djGetDatabases", "djGetDrivers",
          "djGetFunctions", "echo", "alterColumnTrigger", "alterTableTrigger", "openJson",
          "djSubscription", "crawl", "ls", "openText", "openJson", "exec");

  /**
   * syntax check of the files in the model folder
   */
  @Test
  public void testJson() throws Exception {
    for (File file : new File("model").listFiles()) {
      if (file.isDirectory()) {
        for (File item : file.listFiles()) {
          if (!item.getName().endsWith(".json"))
            continue;

          // parse JSON
          Object tree = Json.parseJson(new FileReader(item));

          // check widget values
          Object widgets = strings(jsonata("**.widget").evaluate(tree));
          for (Object widget : (List<String>) widgets)
            Assertions.assertTrue(allowedWidget.contains(widget), "Illegal widget used: " + widget);

          // parse and dry-run JSONata expressions
          for (String x : new String[] {"if", "context", "display", "expression", "onClick",
              "before-create", "after-create", "before-update", "after-update", "before-delete",
              "after-delete"}) {
            for (String expression : strings(jsonata("**." + x).evaluate(tree))) {

              // parse expression
              Jsonata expr = jsonata(expression);
              for (String function : functions)
                expr.registerFunction(function, pseudoImpl);
              expr.evaluate(null);
            }
          }
        }
      }
    }
  }

  /**
   * JSONata unit test
   */
  @Test
  public void testLogic() throws Exception {
    // read test suite
    Map<String, Object> suite = (Map<String, Object>) Json.parseJson(new FileReader("test.json"));
    Map<String, Object> test = (Map<String, Object>) suite.get("test");
    Map<String, Object> basedata = (Map<String, Object>) suite.get("basedata");
    Map<String, Object> cases = (Map<String, Object>) suite.get("cases");

    Object file = Json.parseJson(new FileReader((String) test.get("file")));

    String expression = (String) jsonata((String) test.get("expression")).evaluate(file);

    for (Entry<String, Object> _case : cases.entrySet()) {
      // test case might refer to url for input data
      Map<String, Object> value = (Map<String, Object>) _case.getValue();
      String url = (String) value.get("url");
      if (url != null) {
        String content = IOUtils.toString(new URL(url), Charset.defaultCharset());
        if (url.endsWith(".json"))
          value.put("data", Json.parseJson(content));
        else
          value.put("data", content);
      }

      // eval and check
      if (basedata != null) {
        Map<String, Object> data = new HashMap<>(basedata);
        data.putAll((Map<? extends String, ? extends Object>) value.get("data"));
        Assertions.assertEquals(evaluate(expression, data), value.get("expected"));
      } else {
        Assertions.assertEquals(evaluate(expression, value.get("data")), value.get("expected"));
      }
    }
  }

  /**
   * REST client that calls /rest/expression
   */
  Object evaluate(String expr, Object data) throws Exception {
    // send to http://admin:djdjdj@localhost:8080
    URL url = new URL("http://localhost:8080/rest/expression");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("POST");
    connection.setDoOutput(true);
    connection.setRequestProperty("content-type", "application/json");
    connection.setRequestProperty("Authorization",
        "Basic " + Base64.getEncoder().encodeToString("admin:djdjdj".getBytes()));
    Map<String, Object> expAndData = new HashMap<>();
    expAndData.put("data", data);
    expAndData.put("expression", expr);

    // TODO: remove replace once https://github.com/dashjoin/jsonata-java/issues/20 is fixed
    String payload = Functions.string(expAndData, false).replace("\"result\"", "\\\"result\\\"");
    IOUtils.write(payload, connection.getOutputStream(), Charset.defaultCharset());
    return Json.parseJson(new InputStreamReader(connection.getInputStream()));
  }

  /**
   * converts null, string, [string] to list of strings
   */
  List<String> strings(Object arg) {
    if (arg == null)
      return Arrays.asList();
    if (arg instanceof List)
      return (List<String>) arg;
    return Arrays.asList((String) arg);
  }
}
