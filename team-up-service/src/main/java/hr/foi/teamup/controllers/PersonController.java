/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hr.foi.teamup.controllers;
import hr.foi.teamup.model.Credentials;
import hr.foi.teamup.model.Location;
import hr.foi.teamup.model.Person;
import hr.foi.teamup.repositories.PersonRepository;
import java.util.List;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Tomislav Turek
 * 
 * 
 */
@RestController
@RequestMapping(value = "/person")
public class PersonController {
    
    PersonRepository personRepository;

    @Autowired
    public PersonController(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }
    
    /**
     * gets all users from database
     * @return all users in json format with HTTP 200
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<List<Person>> retrieveAll() {
        Logger.getLogger("PersonController.java").log(Level.INFO,
                "GET on /person -- retrieving full list of users");
        return new ResponseEntity(this.personRepository.findAll(), HttpStatus.OK);
    }
    
    /**
     * looks up a user and verifies password
     * @param credentials username and password
     * @return person info and HTTP 200 on success or HTTP 404 on fail
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ResponseEntity<Person> login(@RequestBody Credentials credentials) {
        Logger.getLogger("PersonController.java").log(Level.INFO,
                "POST on /person/login -- " + credentials.toString());
        String username = credentials.getUsername();
        String password = credentials.getPassword();
        
        Person found = this.personRepository.findByCredentialsUsername(username);
        if(found != null && found.getCredentials().getPassword().equals(password)) {
            Logger.getLogger("PersonController.java").log(Level.INFO,
                    "Successfully verified, returning " + found.toString());
            return new ResponseEntity(found, HttpStatus.OK);
        } else {
            Logger.getLogger("PersonController.java").log(Level.WARN,
                    "Verification failed for " + credentials.toString());
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
     
    }
    
    /**
     * inserts new user to database
     * @param person user to insert
     * @return person info and HTTP 200 on success or HTTP BAD REQUEST on fail
     */
    @RequestMapping(value = "/signup", method = RequestMethod.POST)
    public ResponseEntity<Person> signup(@RequestBody Person person) {
        Logger.getLogger("PersonController.java").log(Level.INFO,
                "POST on /person/signup -- " + person.toString());
        
        Person signed = this.personRepository.save(person);
        if(signed != null) {
            Logger.getLogger("PersonController.java").log(Level.INFO,
                    "Registration success for " + signed.toString());
            return new ResponseEntity(signed, HttpStatus.OK);
        } else {
            Logger.getLogger("PersonController.java").log(Level.WARN,
                    "Registration failed for " + person.toString());
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * gets user with specified id
     * @param id id of user
     * @return person info with HTTP 200 on success or HTTP 404 on fail
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<Person> retrieveById(@RequestParam long id) {
        Logger.getLogger("PersonController.java").log(Level.INFO,
                "GET on /person/" + id + " -- ");
        Person found = this.personRepository.findByIdPerson(id);
        if(found != null) {
            Logger.getLogger("PersonController.java").log(Level.INFO,
                    "User found for id " + id + ", returning " + found.toString());
            return new ResponseEntity(found, HttpStatus.OK);
        } else {
            Logger.getLogger("PersonController.java").log(Level.WARN,
                    "No user found for id " + id);
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }
    
    @RequestMapping(value="/test")
    public ResponseEntity testInsert(){
        Credentials cred=new Credentials();
        
        cred.setPassword("111");
        cred.setUsername("lalala");
        Location a=new Location();
        a.setLat(2.12121);
        a.setLng(2.334);
        
        Person person=new Person();
        person.setIdPerson(11);
        person.setName("probni");
        person.setSurname("probic");
        person.setCredentials(cred);
        person.setLocation(a);
        
        Logger.getLogger("PersonController.java").log(Level.INFO,
                    "Going to save user");
        this.personRepository.save(person);
        Logger.getLogger("PersonController.java").log(Level.INFO,
                    "Going to return users");
        return new ResponseEntity(this.personRepository.findAll(), HttpStatus.OK);
    }
    
     /**
     * updates user with specified id
     * @param id id of user
     * @return person info with HTTP 200 on success or HTTP 404 on fail
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public ResponseEntity modify(@RequestParam long id, @RequestBody Person person) {
        Logger.getLogger("PersonController.java").log(Level.INFO,
                "PUT on /person/" + id + " -- " + person.toString());
        
        Person signed = this.personRepository.findByIdPerson(id);
        if(signed != null) {
            this.personRepository.save(person);
            Logger.getLogger("PersonController.java").log(Level.INFO,
                    "Update successful for " + person.toString());
            return new ResponseEntity(HttpStatus.OK);
        } else {
            Logger.getLogger("PersonController.java").log(Level.WARN,
                    "No user found for id " + id);
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }
}
