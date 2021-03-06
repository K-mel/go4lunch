package com.camel.go4lunch.models.googlePlaceResult;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PlaceSearchGeometry {

    @SerializedName("location")
    @Expose
    private Location location;
    @SerializedName("viewport")
    @Expose
    private Viewport viewport;

    public PlaceSearchGeometry() {
        location = new Location();
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Viewport getViewport() {
        return viewport;
    }

    public void setViewport(Viewport viewport) {
        this.viewport = viewport;
    }

}
