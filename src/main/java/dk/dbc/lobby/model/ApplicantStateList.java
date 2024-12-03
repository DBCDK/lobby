package dk.dbc.lobby.model;

import java.util.List;

public class ApplicantStateList {

    private ApplicantEntity.State state;

    private List<String> id;

    public List<String> getId() {
        return id;
    }

    public void setId(List<String> id) {
        this.id = id;
    }

    public ApplicantStateList withId(List<String> id) {
        this.id = id;
        return this;
    }

    public ApplicantEntity.State getState() {
        return state;
    }

    public void setState(ApplicantEntity.State state) {
        this.state = state;
    }

    public ApplicantStateList withState(ApplicantEntity.State state) {
        this.state = state;
        return this;
    }
}
