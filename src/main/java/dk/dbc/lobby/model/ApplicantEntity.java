
package dk.dbc.lobby.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.lobby.JsonNodeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.sql.Timestamp;
import java.util.Date;

@NamedNativeQueries({
        @NamedNativeQuery(
            name = ApplicantEntity.GET_OUTDATED_APPLICANTS,
            query = "SELECT id FROM applicant where state=CAST (? AS applicant_state) and timeOfLastModification<now()-CAST (? AS INTERVAL)",
            resultClass = ApplicantEntity.class),
        })
@NamedQueries(
        @NamedQuery(
                name = ApplicantEntity.GET_BULK_APPLICANT_BODIES,
                query = "SELECT abe FROM ApplicantBodyEntity abe WHERE abe.id in :ids"
        )
)

@Entity
@Table(name = "applicant")
@JsonInclude(JsonInclude.Include.NON_NULL)
@SqlResultSetMapping(name="applicantResult", classes = {
        @ConstructorResult(targetClass = ApplicantEntity.class,
                columns = {
                        @ColumnResult(name="id"),
                        @ColumnResult(name="category"),
                        @ColumnResult(name="mimetype"),
                        @ColumnResult(name="state"),
                        @ColumnResult(name="timeOfCreation"),
                        @ColumnResult(name="timeOfLastModification"),
                        @ColumnResult(name="additionalInfo")})
})
public class ApplicantEntity {
    public static final String GET_APPLICANTS_QUERY =
            "SELECT applicant FROM ApplicantEntity applicant";
    public static final String GET_APPLICANTS_BY_ADDITIONAL_INFO_QUERY =
            "SELECT id, category, mimetype, state, timeOfCreation, timeOfLastModification, additionalInfo FROM applicant";

    public static final String GET_APPLICANTS_BY_ADDITIONAL_INFO_SQL_RESULT_SET_MAPPER =
            "applicantResult";

    public static final String GET_OUTDATED_APPLICANTS = "getOutdatedApplicants";

    public static  final String GET_BULK_APPLICANT_BODIES = "getBulkApplicants";

    // Please note that when used in a query, then the string value MUST be used
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

    @Transient
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

    public ApplicantEntity() {}

    public ApplicantEntity(String id, String category, String mimetype, String state, Timestamp timeOfCreation, Timestamp timeOfLastModification, org.postgresql.util.PGobject additionalInfo) {
        this.id = id;
        this.category = category;
        this.mimetype = mimetype;
        this.state = State.valueOf(state);
        this.timeOfCreation = timeOfCreation;
        this.timeOfLastModification = timeOfLastModification;

        JsonNodeConverter converter = new JsonNodeConverter();
        this.additionalInfo = converter.convertToEntityAttribute(additionalInfo);
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
