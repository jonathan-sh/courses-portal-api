package com.courses.portal.model;

import com.courses.portal.dao.StudentRepository;
import com.courses.portal.model.dto.Validation;
import com.courses.portal.useful.encryptions.EncryptionSHA;
import com.courses.portal.useful.constants.DetailsDescription;
import com.courses.portal.useful.mongo.MongoHelper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jonathan on 7/16/17.
 */
public class Student {
    @JsonIgnore
    @Expose(serialize = false)
    private static Logger logger = LoggerFactory.getLogger(Student.class);

    @Expose
    public Object _id;
    @Expose
    public String name;
    @Expose
    public String email;
    @Expose
    public String password;
    @Expose
    public List<String> courses = new ArrayList<>();
    @Expose
    public Boolean signature = false;
    @Expose
    public String urlImage;
    @Expose
    public String zipCode;
    @Expose
    public String street;
    @Expose
    public String city;
    @Expose
    public String number;
    @Expose
    public Boolean status;
    @JsonIgnore
    @Expose(serialize = false)
    public Validation validation = new Validation();

    @JsonIgnore
    public String BCryptEncoderPassword() {
        return new BCryptPasswordEncoder().encode(this.password);
    }
    @JsonIgnore
    public Student fieldValidationForCreation() {
        this.validation.status = this.name != null &&
                                 this.email != null &&
                                 this.password != null &&
                                 !this.name.isEmpty() &&
                                 !this.email.isEmpty() &&
                                 !this.password.isEmpty();

        if (!this.validation.status)
        {
            this.validation.fieldsError(requirementsForCreation());
        }

        return this;
    }
    @JsonIgnore
    private String requirementsForCreation() {
        return "< name, email, password >";
    }
    @JsonIgnore
    public Student treatmentForCreate() {
        if (validation.status)
        {
            this.email = this.email.toLowerCase();
            this.password = EncryptionSHA.generateHash(this.password);
            this.status = true;
            this._id = null;
        }
        return this;
    }
    @JsonIgnore
    public Student fieldValidationUpdate() {
        boolean premise = this._id != null;

        if (!premise)
        {
            this.validation.fieldsError(requirementsForUpdate());
        }

        this.validation.status = premise;

        return this;

    }
    @JsonIgnore
    private String requirementsForUpdate() {
        return "< _id >";
    }
    @JsonIgnore
    public Student treatmentForUpdate() {
        this.email = null;
        if (this.password != null)
        {
            this.password = EncryptionSHA.generateHash(this.password);
        }
        return this;
    }
    @JsonIgnore
    public Student validationOfExistence() {

        if (validation.status)
        {
            validation.status = studentRepository.findByEmail(this.email) == null;
            if (!validation.status)
            {
                validation.alreadyExists(this.email);
            }
        }

        return this;
    }
    @JsonIgnore
    public Student treatmentForResponse() {
        if (this._id != null)
        {
            this._id = MongoHelper.treatsId(this._id);
        }
        if (this.password != null)
        {
            this.password = DetailsDescription.PASSWORD.get();
        }
        return this;
    }


    @Expose(serialize = false)
    public static final String COLLECTION = "student";
    @Expose(serialize = false)
    private StudentRepository studentRepository = new StudentRepository(COLLECTION, this.getClass());

    @JsonIgnore
    public Student create() {
        boolean wasCreated = false;
        if (validation.status)
        {
            wasCreated = studentRepository.create(this);
            if (wasCreated)
            {
                this._id = studentRepository.findByEmail(this.email)._id;
                this.validation.httpStatus = HttpStatus.CREATED;
            }
        }

        return this;
    }

    @JsonIgnore
    public Student update() {
        boolean wasUpdated = false;
        this._id = MongoHelper.treatsId(this._id);
        if (validation.status)
        {
            wasUpdated = studentRepository.update(this._id, this);
            if (wasUpdated)
            {

                try
                {
                    Student result = (Student) studentRepository.readOne(this._id);
                    result.validation.httpStatus = HttpStatus.OK;
                    result.validation.status = wasUpdated;
                    return result;
                }
                catch (Exception e)
                {

                    logger.error("Error during cast to Student");
                    logger.error("Possible cause: " + e.getCause());
                }

            }
        }

        return this;
    }

    @JsonIgnore
    public boolean isValid() {
        fieldValidationForCreation();
        return this.validation.status && this.status;
    }

    @JsonIgnore
    public Object getSignatures() {
        List<Document> signatures = new ArrayList<>();
        studentRepository.findAll().forEach(item ->{
            Document doc = new Document();
            doc.put("_id",item._id);
            doc.put("name",item.name);
            doc.put("email",item.email);
            doc.put("signature",item.signature);
            doc.put("status",item.status);
            signatures.add(doc);

        });
        return signatures;
    }
}

