package factoid.web;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import factoid.converter.FactoidToBiopax;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.io.sbgn.L3ToSBGNPDConverter;
import org.biopax.paxtools.model.Model;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


@RestController
@RequestMapping(value = "/v2", method = {RequestMethod.POST})
public class Controller {

  @ApiOperation(value = "json-to-biopax", notes = "Converts a Factoid model to BioPAX.")
  @RequestMapping(path = "/json-to-biopax",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = "application/vnd.biopax.rdf+xml"
  )
  public String jsonToBiopax(
    @ApiParam("Factoid document content (JSON string)") @RequestBody String body
    //TODO: add url/path options as needed
//  ,  @ApiParam("test") @RequestParam(required = false) String test
  ) {
    // Add templates to converter by the reader
    FactoidToBiopax converter = new FactoidToBiopax();
    try {
      converter.addToModel(body);
    } catch (IllegalStateException | JsonSyntaxException | JsonIOException e) {
      throw new ConverterException(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      throw new ConverterException(HttpStatus.INTERNAL_SERVER_ERROR, e.toString());
    }

    // Convert the model to biopax string
    return converter.convertToBiopax();
  }

  @ApiOperation(value = "json-to-sbgn", notes = "Converts a Factoid model to SBGN-ML (via BioPAX).")
  @RequestMapping(path = "/json-to-sbgn",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = "application/xml"
  )
  public String jsonToSbgn(
    @ApiParam("Factoid document (JSON string)") @RequestBody String body) {
    try {
      InputStream is = new ByteArrayInputStream(jsonToBiopax(body).getBytes(StandardCharsets.UTF_8));
      Model model = new SimpleIOHandler().convertFromOWL(is);
      is.close();
      L3ToSBGNPDConverter converter = new L3ToSBGNPDConverter();
      converter.setDoLayout(false); //TODO: apply the default sbgn layout?
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      converter.writeSBGN(model, baos);
      return baos.toString(StandardCharsets.UTF_8.name());
    } catch (Exception e) {
      throw new ConverterException(HttpStatus.INTERNAL_SERVER_ERROR, e.toString());
    }
  }

  @ApiOperation(value = "biopax-to-sbgn", notes = "Converts a factoid BioPAX model to SBGN-ML (SBGN PD).")
  @RequestMapping(path = "/biopax-to-sbgn",
    consumes = "application/vnd.biopax.rdf+xml",
    produces = "application/xml"
  )
  public String biopaxToSbgn(
    @ApiParam("A factoid (small) BioPAX RDF/XML model") @RequestBody String body) {
    try {
      InputStream is = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
      Model model = new SimpleIOHandler().convertFromOWL(is);
      is.close();
      L3ToSBGNPDConverter converter = new L3ToSBGNPDConverter();
      converter.setDoLayout(false);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      converter.writeSBGN(model, baos);
      return baos.toString(StandardCharsets.UTF_8.name());
    } catch (Exception e) {
      throw new ConverterException(HttpStatus.INTERNAL_SERVER_ERROR, e.toString());
    }
  }

}
