package com.camel.go4lunch.repositories;

import com.camel.go4lunch.BuildConfig;
import com.camel.go4lunch.models.PlaceDetailsResult.PlaceDetailsResults;
import com.camel.go4lunch.models.PlaceResult.PlaceResults;
import com.camel.go4lunch.api.PlacesService;
import java.util.concurrent.TimeUnit;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class PlacesDataRepository {


    public Observable<PlaceResults> getNearbyPlaces(String latlng, String radius, String type){
        PlacesService placesService = PlacesService.retrofit.create(PlacesService.class);

        return placesService.fetchNearbyPlaces(BuildConfig.MAPS_API_KEY, latlng, radius, type)
                .subscribeOn(Schedulers.io())
                .timeout(10, TimeUnit.SECONDS);
    }

    public Observable<PlaceResults> getNextPageNearbyPlaces(String pageToken) {
        PlacesService placesService = PlacesService.retrofit.create(PlacesService.class);

        return placesService.fetchNextPageNearbyPlaces(BuildConfig.MAPS_API_KEY, pageToken)
                .subscribeOn(Schedulers.io())
                .timeout(10, TimeUnit.SECONDS);
    }

    public Observable<PlaceDetailsResults> getDetailsForPlaceId(String placeId) {
        PlacesService placesService = PlacesService.retrofit.create(PlacesService.class);

        String fields = "place_id,"
                + "name,"
                + "address_component,"
                + "geometry,"
                + "opening_hours,"
                + "photo,"
                + "international_phone_number,"
                + "website,"
                + "rating";

        return placesService.fetchDetailsForPlaceId(BuildConfig.MAPS_API_KEY, placeId, fields)
                .subscribeOn(Schedulers.io())
                .timeout(10, TimeUnit.SECONDS);
    }

}