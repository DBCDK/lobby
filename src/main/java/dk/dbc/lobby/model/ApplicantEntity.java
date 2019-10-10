/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.lobby.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import dk.dbc.jsonb.JSONBContext;
import dk.dbc.jsonb.JSONBException;
import dk.dbc.jsonb.JsonNodeConverter;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.FetchType;
import javax.persistence.FieldResult;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.Date;

@Entity
@Table(name = "applicant")
@JsonInclude(JsonInclude.Include.NON_NULL)
@SqlResultSetMappings({
        @SqlResultSetMapping(
                name = ApplicantEntity.WITHOUT_BODY,
                entities = @EntityResult(
                        entityClass = ApplicantEntity.class,
                        fields = {
                                @FieldResult(name = "id", column = "id"),
                                @FieldResult(name = "category", column = "category"),
                                @FieldResult(name = "mimetype", column = "mimetype"),
                                @FieldResult(name = "state", column = "state"),
                                @FieldResult(name = "timeOfCreation", column = "timeOfCreation"),
                                @FieldResult(name = "timeOfLastModification", column = "timeOfLastModification"),
                                @FieldResult(name = "additionalInfo", column = "additionalInfo")}))
})
public class ApplicantEntity {
    public static final String GET_APPLICANTS_QUERY =
            "SELECT a.id, a.category, a.mimetype, a.state, a.timeOfCreation, a.timeOfLastModification, a.additionalInfo FROM applicant a";
    public static final String WITHOUT_BODY = "ApplicantEntityEntityWithoutBody";

    // TODO: 08/10/2019 move State enum to standalone api module
    public enum State {
        ACCEPTED, PENDING,
    }

    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    @Id
    private String id;

    private String category;

    private String mimetype;

    @Convert(converter = ApplicantStateConverter.class)
    private State state;

    @Column(insertable = false, updatable = false)
    private Timestamp timeOfCreation;

    private Timestamp timeOfLastModification;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] body;

    @JsonProperty
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode additionalInfo;

    @PrePersist
    @PreUpdate
    void onDatabaseCommit() {
        this.timeOfLastModification = new Timestamp(new Date().getTime());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Timestamp getTimeOfCreation() {
        return timeOfCreation;
    }

    public void setTimeOfCreation(Timestamp timeOfCreation) {
        this.timeOfCreation = timeOfCreation;
    }

    public Timestamp getTimeOfLastModification() {
        return timeOfLastModification;
    }

    public void setTimeOfLastModification(Timestamp timeOfLastModification) {
        this.timeOfLastModification = timeOfLastModification;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public JsonNode getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(JsonNode additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    @JsonIgnore
    public void setAdditionalInfo(String additionalInfo) throws JSONBException {
        setAdditionalInfo(JSONB_CONTEXT.getJsonTree(additionalInfo));
    }

    @Override
    public String toString() {
        return "ApplicantEntity{" +
                "id='" + id + '\'' +
                ", category='" + category + '\'' +
                ", mimetype='" + mimetype + '\'' +
                ", state=" + state +
                ", timeOfCreation=" + timeOfCreation +
                ", timeOfLastModification=" + timeOfLastModification +
                ", additionalInfo='" + additionalInfo + '\'' +
                '}';
    }
}
