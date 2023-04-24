import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Paths;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.hasItems;

@QuarkusTest
public class AirlineTest {

  @ConfigProperty(name = "test.json.path") 
  String testJsonPath;

    @Test
    public void testRebook3Pax2Flights() {
        given()
          .body(getBodyFromJsonFile("rebook2Pax2Flights.json"))
          .contentType(ContentType.JSON)
          .when()
            .post("/airline")
          .then()
            .log().all()
            .statusCode(200)
            .body("'Rebooked Passengers'.size()", is(3))
            .body("'Rebooked Passengers'.Name", hasItems("Gold Passenger",
            "Silver Passenger","Bronze Passenger"))
            .body("'Rebooked Passengers'.'Flight Number'", hasItems("4321","4325"));
    }

    @Test
    public void testRebook3Pax2FlightsDiffMileagePriority() {
        given()
          .body(getBodyFromJsonFile("rebook3Pax2FlightsDiffMileagePriority.json"))
          .contentType(ContentType.JSON)
          .when()
            .post("/airline")
          .then()
            .log().all()
            .statusCode(200)
            .body("'Prioritized Waiting List'.size()",is(3))
            .body("'Prioritized Waiting List'[1].Name",is("Bronze Passenger 1"))
            .body("'Prioritized Waiting List'[2].Name",is("Bronze Passenger 2"))
            .body("'Rebooked Passengers'.size()", is(3))
            .body("'Rebooked Passengers'.Name", hasItems("Silver Passenger"
            ,"Bronze Passenger 1","Bronze Passenger 2"))
            .body("'Rebooked Passengers'.'Flight Number'", hasItems("4321","4325"));
    }

    protected String getBodyFromJsonFile(String fileName){
        String jsonStr = null;
        try{
              jsonStr = new String(Files.readAllBytes(Paths.get(this.testJsonPath + "/" + fileName)));

        }catch(Exception e){
            e.printStackTrace();
        }
        return jsonStr;
    }
}