package com.camel.go4lunch.mappersTests;

import com.camel.go4lunch.mappers.NearbyPlacesResultToRestaurantMapper;
import com.camel.go4lunch.models.Restaurant;
import com.camel.go4lunch.models.googlePlaceResult.PlaceSearch;
import com.camel.go4lunch.models.googlePlaceResult.PlaceSearchResults;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.camel.go4lunch.mappers.NearbyPlacesResultToRestaurantMapper.OPERATIONAL_BUSINESS_STATUS;

import static org.junit.Assert.assertEquals;

public class NearbyPlacesResultToRestaurantMapperTest {

    @Test
    public void test_getExpectedRestaurantsAmount(){
        PlaceSearchResults placeSearchResults = generateNearbySearchWith2GoodsPlacesAnd2Wrong();
        NearbyPlacesResultToRestaurantMapper mapper = new NearbyPlacesResultToRestaurantMapper();

        List<Restaurant> restaurantList = mapper.apply(placeSearchResults);
        assertEquals(2, restaurantList.size());
    }

    // ---------------
    // Generate data
    // ---------------

    private PlaceSearchResults generateNearbySearchWith2GoodsPlacesAnd2Wrong() {
        PlaceSearch placeSearch1 = new PlaceSearch();
        placeSearch1.setName("Le viand'art");
        placeSearch1.setPlaceId("ChIJH274sClwjEcRniBZAsyAtH0");
        placeSearch1.setBusinessStatus(OPERATIONAL_BUSINESS_STATUS);

        PlaceSearch placeSearch2 = new PlaceSearch();
        placeSearch2.setName("Mizuki");
        placeSearch2.setPlaceId("ChIJmZKgsilwjEcRKVdZd_cE-4k");
        placeSearch2.setBusinessStatus(OPERATIONAL_BUSINESS_STATUS);

        PlaceSearch placeSearch3 = new PlaceSearch();
        placeSearch3.setName("Keko");
        placeSearch3.setPlaceId("ilwdsgdghgkjEcRKVdZd_cklùklù");
        placeSearch3.setBusinessStatus("CLOSED_PERMANENTLY");

        PlaceSearch placeSearch4 = new PlaceSearch();
        placeSearch4.setName("Bab");
        placeSearch4.setPlaceId("ihfjfdfghlwdsgdghgkjEcRKsdgsd");

        List<PlaceSearch> placeSearchList = new ArrayList<>();
        placeSearchList.add(placeSearch1);
        placeSearchList.add(placeSearch2);
        placeSearchList.add(placeSearch3);
        placeSearchList.add(placeSearch4);

        PlaceSearchResults placeSearchResults = new PlaceSearchResults();
        placeSearchResults.setPlaceSearches(placeSearchList);
        placeSearchResults.setNextPageToken("nextPageToken");
        placeSearchResults.setStatus("OK");

        return placeSearchResults;
    }
}
