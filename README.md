Tekninen kuvaus ja ohjeet Joonas Stengårdin tekemän Alman ennakkotehtävän käyttämiseen.

# Toteutus
Toteutin ohjelman Java Springillä tehtävänannon mukaisesti, H2 muistinvaraisella tietokannalla. Työkaluna käytin Eclipseä. Käytin englantia muuttujien ja luokkien nimeämiseen sekä käyttäjälle annettaviin ilmoituksiin, koska tehtävänannossa ei ollut puhetta kielestä ja mietin että alan standardina se on ehkä todennäköisemmin teillä käytössä työkielenä ja Kauppalehden API:n tiedot oli myös englanniksi. Kirjoitin myös kattavan suomenkielisen kommentoinnin ohjelmaan.

# Ohjeet käynnistämiseen
Ohjelma käynnistetään AlmaEnnakkoApplication.java nimisestä luokasta, jossa on ohjelman päämetodi. Esimerkiksi avaamalla luokka IDE ohjelmassa kuten Eclipsessä ja valitsemalla toiminto "run" luokan sisällä. Luokat näkyvät seuraavan kansiorakenteen alla: 
AlmaEnnakko->src/main/java->com.joonass.ennakkoteht

Ohjelma toimii 8080 portissa localhostissa. Tarkemmat URL-osoitteet rajapinnan käyttämiseen alempana ohjelman rakenteessa.

# Ohjelman rakenne
Ohjelmassa on pääluokan lisäksi Customer, CustomerController, CustomerRepository ja SpringDocConfig luokat.

Customer-luokka on asiakkaan tietomalli tietokannassa, sinne on määritelty tietokantataulun sarakkeet eli asiakkaan tiedot, ja getterit ja setterit niiden käyttämiseen.
Sarakkeet ovat: id, name, email, website, businessId, streetAddress ja phoneNumber.

Id:ssä on Spring Data JPA:n @Id annotaatio, eli se on asiakkaiden käyttämä tietokannan pääavain eli primary key. Spring generoi ID:n automaattisesti kun asiakkaan muut tiedot lisätään tietokantaan. Koska ID on uniikki, käytin sitä CustomerControllerissa halutun asiakkaan löytämiseen ja poistamiseen.

CustomerRepository on JpaRepository rajapinta joka mahdollistaa tietokantatoimintojen suorittamisen asiakkaiden tiedoille, eli sovellukssa käytetyt findById, findAll jne. metodit. Custom hakuja tähän ohjelmaan ei tarvittu ja Repositoryssä ei ole paljoa koodia.

CustomerController luokassa on ohjelman GET, POST ja DELETE HTTP metodit. Niistä ensimmäisenä on addCustomer metodi, jota käytetään POST requestilla asiakkaiden lisäämiseen tietokantaan. Lisättävä asiakas täytyy lisätä POST requestin Body-osaan JSON muodossa, esimerkiksi näin:
```json
{
  "name": "Matti",
  "email": "matti.mallikas@gmail.com",
  "businessId": "1944757-4"
}
```
Metodi käyttää /customers mappingia, eli POST requesti tehdään seuraavaan osoitteeseen:
http://localhost:8080/customers/

Nimi eli "name" on tehtävänannon mukaisesti pakollinen arvo asiakkaan tietoja syöttäessä. Se tarkastetaan seuraavalla koodinpätkällä addCustomer metodissa:

```java
//Tarkastetaan ensin onko asiakkaan nimeä syötetty
    	if(customer.getName() == null) {
    		//Halutaan että nimen syöttäminen on pakollista, joten annetaan kustomoitu error viesti ilmoittaen että nimi puuttuu POST requestista
    		return new ResponseEntity<>("Including a name is mandatory.", HttpStatus.BAD_REQUEST);
    	}
```

Sähköposti eli "email" ja verkkosivut eli "website" ovat valinnaisia, ne jäävät nulliksi jos niitä ei syötetä. Jos sähköposti annettiin, siitä tarkistetaan oliko se oikeassa muodossa tekemällä siihen regex vertaus:

```java
    	if(customer.getEmail() != null) {
    		//Jos sähköposti on syötetty, tarkistetaan regexillä onko se oikeassa muodossa. Tämä on yleinen regex sähköpostin tarkistamiseen, 
    		//eli tekstiä ja yleisiä merkkejä + @-merkki + tekstiä ja yleisiä merkkejä + piste + 2-4 merkkiä aakkosia
    		//Jos sähköposti on validissa muodossa, ohitetaan tämä
    		if (!customer.getEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}$")) {
    			//Sähköposti ei ollut oikeassa muodossa
                return new ResponseEntity<>("Invalid email address format.", HttpStatus.BAD_REQUEST);
            }
    	}
```
Y-tunnus eli businessId on myös valinnainen. Jos se annetaan, tekee ohjelma GET requestin Kauppalehden API:in, mistä haetaan asiakkaan yrityksen osoite ja puhelinnumero, ja tallennetaan ne mukaan asiakkaan tietoihin. Kauppalehden API antaa tiedot JSON objekteina, niin ohjelma ottaa niistä vain ne halutut arvot. Puhelinnumero muunnetaan +358 muotoon tehtävänannon mukaisesti.

Controllerin getAllCustomers metodi listaa kaikki tietokannan asiakkaat. Sitä voi käyttää tekemällä GET requestin osoitteeseen:
http://localhost:8080/customers/ (eli sama mihin POST requesti asiakkaan lisäämiseksi)
Lisäsin vielä ylimääräisen ilmoituksen tilanteeseen, jossa käyttäjä yrittää käyttää tätä metodia tyhjään tietokantaan: 

```java
//Tarkistetaan että onko tietokannassa vielä yhtään asiakkaita, jos se on tyhjä niin ilmoitetaan asiasta
        if(customers.isEmpty()) {
        	return new ResponseEntity<>("No customers have been added to the database yet.", HttpStatus.NOT_FOUND);
        }
```

Yksittäisen asiakkaan tietoja voi hakea getCustomerById metodilla jota voi käyttää tekemällä GET requestin osoitteeseen http://localhost:8080/customers/{customerId}, jossa {customerId} tilalle voi laittaa halutun käyttäjän ID:n. Esimerkiksi:
http://localhost:8080/customers/1 missä haetaan asiakas jonka id on 1.

Yksittäisen käyttäjän haussa käytetään ID:tä siksi, että se on ainoa uniikki tietokantataulun sarake asiakkailla. Asiakkaiden ID:n näkee tässä ohjelmassa molempia ylempiä metodeja käyttämällä. Eli lisäsin ilmoituksen ID:stä asiakkaan tallentamisen yhteyteen, ja lisäksi ne on listattuna tuossa getAllCustomers haussa.

Asiakkaan tiedot voi poistaa deleteCustomer metodilla, joka toimii samassa osoitteessa kuin ylempi, mutta DELETE requestilla. Rakenteeltaan deleteCustomer metodi on melkein sama kuin getCustomerById, sinne on vaan lisätty asiakkaan poistamiseen seuraava koodi:

```java
if (customer.isPresent()) {
        	//Poistetaan asiakas, ja annetaan takaisin ilmoitus jossa näkyy poistetun asiakkaan ID
        	customerRepository.deleteById(customerId);
            return new ResponseEntity<>("Successfully deleted user with the id of:"+customerId+" from database. ", HttpStatus.OK);
        }
```

Eli DELETE requesti tehdään esimerkiksi seuraavaan osoitteeseen: http://localhost:8080/customers/3 missä numero 3 tarkoittaa poistettavan asiakkaan ID arvoa.

Molemmissa ylemmissä metodeissa on lisäksi virheilmoitukset jos haettua asiakasta ei löydy tietokannasta.

Viimeisenä luokkana ohjelmassa on vielä SpringDocConfig, jossa on konfiguraatiot OpenAPI kuvauksen generoimiseen, siitä lisää alempana OpenAPI kuvaus-kappaleessa.

# Yleisimmistä virhetilanteista

Ohjelmassa on useita erilaisia viestejä mitä se voi antaa eri virheidenkäsittelytilanteissa: jos annettu sähköpostiosoite on väärässä muodossa, jos nimeä ei ole annettu, jos tietokanta on tyhjä, jos GET requesti Kauppalehden API:in epäonnistui tai jos sen käsittelyssä oli virhe, ja jos haettavaa tai poistettavaa asiakasta ei löydy. 

Pohdin asiaa, että onko se virhetilanne jos käyttäjä syöttää asiakkaan yrityksen osoitteen tai puhelinnumeron POST requestissa suoraan. Niitähän ei siinä mielessä kuuluisi syöttää itse, että ohjelma hakee ne Kauppalehden API:sta jos y-tunnus on syötetty. Kallistuin hieman siihen että se ei ole virhetilanne, mutta tehtävänanto jätti tämän avoimeksi niin "korjasin sen" että pääsin koodaamaan siihen tarkastuksen. Eli jos käyttäjä yrittää syöttää jomman kumman arvon manuaalisesti, ohjelma vaihtaa molempien niiden arvoksi null. Jos lisäksi y-tunnus on annettu, ne haetaan sitä kautta, jos ei ole y-tunnusta, ne jää nulliksi.

# OpenAPI kuvaus
OpenAPI kuvauksen tekemiseen käytin springdoc-openapi kirjastoa. Ohjelman ollessa käynnissä, kuvauksen näkee seuraavasta URL osoitteesta Swagger UI:lla: 
http://localhost:8080/swagger-ui.html
