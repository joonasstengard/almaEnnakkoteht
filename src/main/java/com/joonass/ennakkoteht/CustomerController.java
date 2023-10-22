package com.joonass.ennakkoteht;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;




@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/customers")
@Tag(name = "Customer API")
public class CustomerController {
    @Autowired
    private CustomerRepository customerRepository;

    //POST requesti asiakkaan lisäämiselle tietokantaan JSON muodossa
    //ResponseEntityn tyyppinä on <?> jotta voidaan antaa kustomoituja virheilmoituksia
    @PostMapping(value="/", consumes={"application/json"})
    @Operation(summary = "Adds a new customer", description = "Add a new customer to the database as a POST request with a JSON. "
    		+ "Including a name is mandatory. Including email, website or business ID is optional. "
    		+ "Street address and phone number are fetched automatically from the API of Kauppalehti if a Business ID is given. ")
    @ApiResponse(responseCode = "201", description = "Customer added successfully.")
    @ApiResponse(responseCode = "400", description = "Bad request: Including a name is mandatory. / Bad request: Invalid email address format.")
    @ApiResponse(responseCode = "500", description = "Error processing request / Error accessing Kauppalehti API")

    public ResponseEntity<?> addCustomer(@RequestBody Customer customer) {
        
    	//Tarkastetaan ensin onko asiakkaan nimeä syötetty
    	if(customer.getName() == null) {
    		//Halutaan että nimen syöttäminen on pakollista, joten annetaan kustomoitu error viesti ilmoittaen että nimi puuttuu POST requestista
    		return new ResponseEntity<>("Including a name is mandatory.", HttpStatus.BAD_REQUEST);
    	}
    	
    	//Tarkastetaan onko sähköpostia syötetty, jos ei ole niin ohitetaan vaan tämä koska sähköposti ei ole pakollinen
    	if(customer.getEmail() != null) {
    		//Jos sähköposti on syötetty, tarkistetaan regexillä onko se oikeassa muodossa. Tämä on yleinen regex sähköpostin tarkistamiseen, 
    		//eli tekstiä ja yleisiä merkkejä + @-merkki + tekstiä ja yleisiä merkkejä + piste + 2-4 merkkiä aakkosia
    		//Jos sähköposti on validissa muodossa, ohitetaan tämä
    		if (!customer.getEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}$")) {
    			//Sähköposti ei ollut oikeassa muodossa
                return new ResponseEntity<>("Invalid email address format.", HttpStatus.BAD_REQUEST);
            }
    	}
    	
    	//Tarkastetaan onko asiakkaan yrityksen katuosoitetta tai puhelinnumeroa syötetty, niitä ei kuulu syöttää manuaalisesti
    	if(customer.getPhoneNumber() != null || customer.getStreetAddress() != null) {
    		//Jos kumpi tahansa oli syötetty, laitetaan niiden arvoksi null
    		customer.setPhoneNumber(null);
    		customer.setStreetAddress(null);
    	}
        

    	//Otetaan y-tunnus eli businessId talteen. Tämä on null jos sitä ei ole syötetty POST requestiin
        String businessId = customer.getBusinessId();

        //Tarkastetaan onko y-tunnusta syötettynä. Jos on, tehdään alla oleva GET requesti Kauppalehden API:in. Jos ei ole, tallennetaan asiakkaan tiedot sellaisenaan.
        if (businessId != null) {
            //Tehdään GET requesti Kauppalehden API:n, jossa on annettu y-tunnus URL:n loppuosassa
            RestTemplate restTemplate = new RestTemplate();
            String apiUrl = "https://www.kauppalehti.fi/company-api/basic-info/" + businessId;
            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);

            //Tarkastetaan onnistuuko GET requesti
            if (response.getStatusCode().is2xxSuccessful()) {
                try {
                	//Kauppalehden API:ssa näitä tietoja ei anneta Stringeinä vaan JSON objekteina, joten käytetään
                	//Jacksonia siihen että otetaan streetAddress ja phone objekteista pelkästään "value" niminen arvo talteen.
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonResponse = objectMapper.readTree(response.getBody());

                    JsonNode streetAddressNode = jsonResponse.path("streetAddress");
                    JsonNode phoneNode = jsonResponse.path("phone");

                    JsonNode streetNode = streetAddressNode.path("street").path("value");
                    JsonNode phoneValueNode = phoneNode.path("value");

                    String streetAddress = streetNode.asText();
                    String phone = phoneValueNode.asText();

                    //Laitetaan arvoksi null, jos ne ovat Kauppalehdeltä saaduissa tiedoissa tyhjiä, esimerkiksi jos heidän antamien tietojen rakenne muuttuu olennaisesti. 
                    //POST requestin kautta syöttämättä jätetyt valinnaiset tiedot ovat tässä applikaatiossa myös aina null, joten tämä on mielestäni jatkokehityksen kannalta selkeintä
                    if (streetAddress.isEmpty()) {
                        streetAddress = null;
                    }
                    if (phone.isEmpty()) {
                        phone = null;
                    }
                    
                    //Asetetaan Kauppalehden API:sta saadut katuosoite ja puhelinnumero customer objektille ennen tallennusta
                    customer.setStreetAddress(streetAddress);
                    customer.setPhoneNumber(phone);
                } catch (Exception e) {
                	//Käsitellään virhe jos GET requestin käsitteleminen ei onnistu
                    e.printStackTrace();
                    return new ResponseEntity<>("Error processing request. ", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else {
                return new ResponseEntity<>("Error accessing Kauppalehti API. ", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        //Tallennetaan lopuksi asiakkaan tiedot, tänne päädytään lopulta oli asiakkaan tiedoissa mukana y-tunnusta tai ei
        Customer savedCustomer = customerRepository.save(customer);
        //Tarkastetaan vielä ilmoitusta varten että haettiinko Kauppalehden API:sta tietoja. Tämän voisi tehdä ylemmässä metodissa, mutta mielestäni paremman luettavuuden vuoksi se on tässä
        String infoAboutDataFetchedFromKLApi = "";
        if(savedCustomer.getBusinessId()!=null) {
        	infoAboutDataFetchedFromKLApi = "The following additional information about the customer was fetched from an external API - Street address: "+savedCustomer.getStreetAddress()+
        			" and phone number: "+savedCustomer.getPhoneNumber()+".";
        }
        //Ilmoitus nimen kera siitä että tallennus onnistui.
        return new ResponseEntity<>("Successfully saved customer with a name of "+savedCustomer.getName()+" to database. Assigned it an ID of "+savedCustomer.getId()+". "+infoAboutDataFetchedFromKLApi, HttpStatus.CREATED);
    }

    
    //Listaa kaikki tietokannan asiakkaat GET requestilla customers/ osoitteeseen
    @GetMapping("/")
    @Operation(summary = "Lists all customers", description = "Retrieves a list of all customers in the database. ")
    @ApiResponse(responseCode = "200", description = "List all customers.")
    @ApiResponse(responseCode = "404", description = "No customers have been added to the database yet. ")
    public ResponseEntity<?> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();
        //Tarkistetaan että onko tietokannassa vielä yhtään asiakkaita, jos se on tyhjä niin ilmoitetaan asiasta
        if(customers.isEmpty()) {
        	return new ResponseEntity<>("No customers have been added to the database yet.", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(customers, HttpStatus.OK);
    }

    //Hae yksittäisen asiakkaan tiedot ID:n mukaan, ID on aina uniikki. GET requesti osoitteeseen customers/id
    @GetMapping("/{customerId}")
    @Operation(summary = "Get customer by ID")
    @ApiResponse(responseCode = "200", description = "Successfully found a customer with the corresponding ID")
    @ApiResponse(responseCode = "404", description = "No customer found with the given ID.")
    @Parameter(name = "The unique ID of the customer", required = true)
    public ResponseEntity<?> getCustomerById(@PathVariable Long customerId) {
    	//Etsitään ID:llä tietokannasta asiakas
        Optional<Customer> customer = customerRepository.findById(customerId);
        if (customer.isPresent()) {
            return new ResponseEntity<>(customer.get(), HttpStatus.OK);
        } else {
        	//Annetulla ID:llä ei löytynyt asiakasta
            return new ResponseEntity<>("No customer found with the given ID. ", HttpStatus.NOT_FOUND);
        }
    }

    //Poista yksittäisen asiakkaan tiedot ID:n mukaan. DELETE requesti osoitteeseen customers/id
    @DeleteMapping("/{customerId}")
    @Operation(summary = "Delete customer by ID")
    @ApiResponse(responseCode = "200", description = "Successfully deleted user from database.")
    @ApiResponse(responseCode = "404", description = "No customer found with the given ID.")
    @Parameter(name = "The unique ID of the customer", required = true)
    public ResponseEntity<?> deleteCustomer(@PathVariable Long customerId) {
    	//Etsitään ID:llä tietokannasta asiakas poistamista varten
    	Optional<Customer> customer = customerRepository.findById(customerId);
        if (customer.isPresent()) {
        	//Poistetaan asiakas, ja annetaan takaisin ilmoitus jossa näkyy poistetun asiakkaan ID
        	customerRepository.deleteById(customerId);
            return new ResponseEntity<>("Successfully deleted user with the id of:"+customerId+" from database. ", HttpStatus.OK);
        } else {
        	//Annetulla ID:llä ei löytynyt asiakasta
            return new ResponseEntity<>("No customer found with the given ID. ", HttpStatus.NOT_FOUND);
        }

    }

    
}
