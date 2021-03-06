package com.camel.go4lunch.ui.fragment.listView;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.camel.go4lunch.R;
import com.camel.go4lunch.mappers.RestaurantToListViewMapper;
import com.camel.go4lunch.models.Restaurant;
import com.camel.go4lunch.repositories.RestaurantUseCase;
import com.camel.go4lunch.repositories.UserDataRepository;
import com.camel.go4lunch.utils.SingleLiveEvent;
import com.camel.go4lunch.utils.liveEvent.LiveEvent;
import com.camel.go4lunch.utils.liveEvent.ShowSnackbarLiveEvent;
import com.camel.go4lunch.utils.liveEvent.StopRefreshLiveEvent;

import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

@HiltViewModel
public class ListViewViewModel extends ViewModel {
    private static final String TAG = "ListViewViewModel";

    private final RestaurantUseCase mRestaurantUseCase;
    private final UserDataRepository mUserDataRepository;

    private final CompositeDisposable mDisposable = new CompositeDisposable();
    private final MutableLiveData<List<Restaurant>> mRestaurantListLiveData = new MutableLiveData<>();
    private final SingleLiveEvent<LiveEvent> mSingleLiveEvent = new SingleLiveEvent<>();

    @Inject
    public ListViewViewModel(RestaurantUseCase restaurantUseCase,
                             UserDataRepository userDataRepository) {
        mRestaurantUseCase = restaurantUseCase;
        mUserDataRepository = userDataRepository;
    }

    public void startObservers(){
        mDisposable.add(mRestaurantUseCase.observeErrors()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::getErrorLiveEvents,
                        throwable -> {
                            Log.e(TAG, "mRestaurantUseCase.observeErrors: ", throwable);
                            mSingleLiveEvent.setValue(new ShowSnackbarLiveEvent(R.string.error));
                        }
                ));

        mDisposable.add(mRestaurantUseCase.observeRestaurantList()
                .subscribeOn(Schedulers.computation())
                .map(new RestaurantToListViewMapper(mUserDataRepository.getLocation(),
                        mUserDataRepository.getDistanceUnit(),
                        Calendar.getInstance()))
                .subscribe(mRestaurantListLiveData::postValue,
                        throwable -> {
                    Log.e(TAG, "mRestaurantRepository.observeRestaurantList: ", throwable);
                    mSingleLiveEvent.postValue(new ShowSnackbarLiveEvent(R.string.error));
                }));
    }

    public LiveData<List<Restaurant>> observeRestaurantList() {
        return mRestaurantListLiveData;
    }

    public LiveData<LiveEvent> observeEvents(){
        return mSingleLiveEvent;
    }

    private void getErrorLiveEvents(Throwable throwable){
        if(throwable.getMessage().contains("TimeoutException")){
            mSingleLiveEvent.setValue(new ShowSnackbarLiveEvent(R.string.error_timeout));
        }
        else if(throwable.getMessage().contains("UnknownHostException")) {
            mSingleLiveEvent.setValue(new ShowSnackbarLiveEvent(R.string.error_no_internet));
        }
        else if(throwable.getMessage().contains("NoMorePageException")) {
            mSingleLiveEvent.setValue(new StopRefreshLiveEvent());
        }
        else {
            mSingleLiveEvent.setValue(new ShowSnackbarLiveEvent(R.string.error));
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        clearDisposables();
    }

    public void clearDisposables(){
        mDisposable.clear();
    }

    public void loadNextPage() {
        mRestaurantUseCase.loadNextPage();
    }
}
