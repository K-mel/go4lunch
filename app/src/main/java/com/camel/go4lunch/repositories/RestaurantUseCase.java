package com.camel.go4lunch.repositories;

import android.util.Log;

import com.camel.go4lunch.mappers.NearbyPlacesResultToRestaurantMapper;
import com.camel.go4lunch.mappers.PlaceDetailsResultToRestaurantMapper;
import com.camel.go4lunch.models.Restaurant;
import com.camel.go4lunch.models.Workmate;
import com.camel.go4lunch.utils.NoMorePageException;

import java.util.HashMap;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class RestaurantUseCase{
    private final GooglePlacesRepository mGooglePlacesRepository;
    private final com.camel.go4lunch.repositories.RestaurantRepository mRestaurantRepository;
    private final com.camel.go4lunch.repositories.WorkmatesRepository mWorkmatesRepository;

    private Disposable mDisposable;

    private final PublishSubject<Exception> mErrorsObservable = PublishSubject.create();

    @Inject
    public RestaurantUseCase(GooglePlacesRepository googlePlacesRepository,
                             com.camel.go4lunch.repositories.RestaurantRepository restaurantRepository,
                             com.camel.go4lunch.repositories.WorkmatesRepository workmatesRepository){
        mGooglePlacesRepository = googlePlacesRepository;
        mRestaurantRepository = restaurantRepository;
        mWorkmatesRepository = workmatesRepository;
    }

    public void getNearbyPlaces(double latitude, double longitude, double radius) {
        mRestaurantRepository.clearRestaurantList();
        mDisposable = mGooglePlacesRepository.getNearbyPlaces(latitude, longitude, radius)
                .subscribeOn(Schedulers.io())
                .map(new NearbyPlacesResultToRestaurantMapper())
                .map(restaurants -> {
                    mRestaurantRepository.setNewListSize(restaurants.size());
                    return restaurants;
                })
                .switchMap(Observable::fromIterable)
                .flatMap(this::getInterestedWorkmates)
                .flatMap(this::getDetailsForPlace)
                .subscribe(
                        mRestaurantRepository::addNewRestaurant,
                        throwable -> {
                            mErrorsObservable.onNext(new Exception(throwable));
                            Log.e("RestaurantUseCase", "getNearbyPlaces: " + throwable.toString());
                        });
    }

    private Observable<Restaurant> getInterestedWorkmates(Restaurant restaurant){
        return mWorkmatesRepository.getInterestedWorkmatesForRestaurants(restaurant.getUId())
                .subscribeOn(Schedulers.io())
                .map(interestedWorkmates -> {
                    restaurant.getInterestedWorkmates().clear();
                    for(Workmate workmate : interestedWorkmates) {
                        restaurant.getInterestedWorkmates().add(workmate.getUId());
                    }
                    return restaurant;
                });
    }

    private Observable<Restaurant> getDetailsForPlace(Restaurant restaurant){
        return mGooglePlacesRepository.getDetailsForPlaceId(restaurant.getUId())
                .subscribeOn(Schedulers.io())
                .map(new PlaceDetailsResultToRestaurantMapper(restaurant));
    }

    public void loadNextPage() {
        if(mGooglePlacesRepository.haveNextPageToken()) {
            mDisposable = mGooglePlacesRepository.getNextPageNearbyPlaces()
                    .map(new NearbyPlacesResultToRestaurantMapper())
                    .map(restaurants -> {
                        mRestaurantRepository.setAddListSize(restaurants.size());
                        return restaurants;
                    })
                    .switchMap(Observable::fromIterable)
                    .flatMap(this::getInterestedWorkmates)
                    .flatMap(this::getDetailsForPlace)
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            mRestaurantRepository::addNewRestaurant,
                            throwable -> {
                                mErrorsObservable.onNext(new Exception(throwable));
                                Log.e("RestaurantUseCase", "getNearbyPlaces: " + throwable.toString());
                            });
        } else {
            mErrorsObservable.onNext(new NoMorePageException());
        }
    }

    public Observable<Restaurant> getRestaurantWithId(String restaurantId){
        return mRestaurantRepository.isRestaurantPresent(restaurantId)
                .flatMap(restaurantPresent -> {
                    if(restaurantPresent){
                        return mRestaurantRepository.getRestaurantWithId(restaurantId);
                    }else {
                        return getDetailsForPlace(new Restaurant(restaurantId));
                    }
                })
                .subscribeOn(Schedulers.io());
    }

    public Observable<HashMap<String, Restaurant>> observeRestaurantList(){
        return mRestaurantRepository.observeRestaurantList();
    }

    public Observable<Exception> observeErrors(){
        return mErrorsObservable;
    }

    public void clearDisposable(){
        if(mDisposable != null) {
            mDisposable.dispose();
        }
    }
}
