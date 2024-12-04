package dk.dbc.lobby.model;

import java.util.List;

public class ApplicantStateList {

    private ApplicantEntity.State state;

    private List<String> ids;

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }

    public ApplicantStateList withIds(List<String> ids) {
        this.ids = ids;
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

    @Override
    public String toString() {
        return "ApplicantStateList{" +
                "state=" + state +
                ", ids=" + ids +
                '}';
    }
}
