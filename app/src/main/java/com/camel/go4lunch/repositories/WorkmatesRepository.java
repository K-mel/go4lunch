package com.camel.go4lunch.repositories;

import static com.camel.go4lunch.utils.Utils.isToday;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.camel.go4lunch.api.WorkmateHelper;
import com.camel.go4lunch.models.Workmate;
import com.camel.go4lunch.utils.liveEvent.ErrorLiveEvent;
import com.camel.go4lunch.utils.liveEvent.LiveEvent;
import com.camel.go4lunch.utils.liveEvent.SignOutLiveEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

@Singleton
public class WorkmatesRepository{
    private static final String TAG = "WorkmatesRepository";

    private final PublishSubject<LiveEvent> mTaskResultObservable = PublishSubject.create();
    private final BehaviorSubject<Workmate> mCurrentUserObservable = BehaviorSubject.create();
    private final BehaviorSubject<List<Workmate>> mWorkmateListObservable = BehaviorSubject.create();

    private Workmate mCurrentUser;

    @Inject
    public WorkmatesRepository() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser != null){
            startCurrentUserObserver(currentUser.getUid());
        }
    }

    public void attemptToCreateWorkmate(Workmate workmate) {
        mCurrentUser = workmate;
        WorkmateHelper.getWorkmateWithId(mCurrentUser.getUId())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {                        // The workmate is already created, start observer
                            mCurrentUser = Objects.requireNonNull(document.toObject(Workmate.class));
                            startCurrentUserObserver(mCurrentUser.getUId());
                        } else {                                        // The workmate is already created, create and start observer
                            WorkmateHelper.setWorkmate(mCurrentUser)
                                    .addOnSuccessListener(aVoid -> startCurrentUserObserver(mCurrentUser.getUId()))
                                    .addOnFailureListener(error -> {
                                        mTaskResultObservable.onNext(new ErrorLiveEvent(error));
                                        Log.e(TAG, "authWorkmate: ", error);
                                    });
                        }
                    }
                })
                .addOnFailureListener(error -> {
                    mTaskResultObservable.onNext(new ErrorLiveEvent(error));
                    Log.e(TAG, "authWorkmate: ", error);
                });
    }

    public void startCurrentUserObserver(String uId){
        WorkmateHelper.getWorkmateReferenceWithId(uId)
                .addSnapshotListener((value, error) -> {
                    if(error != null){
                        mTaskResultObservable.onNext(new ErrorLiveEvent(error));
                        Log.e(TAG, "startCurrentUserObserver: error.toString() : ", error);
                        return;
                    }
                    if(value != null && value.exists()) {
                        mCurrentUser = Objects.requireNonNull(value.toObject(Workmate.class));
                        mCurrentUserObservable.onNext(mCurrentUser);
                    } else {
                        mTaskResultObservable.onNext(new SignOutLiveEvent());
                    }
                });
    }

    public Observable<Workmate> observeCurrentUser() {
        return mCurrentUserObservable;
    }

    public Observable<List<Workmate>> observeWorkmates(){
        WorkmateHelper.getWorkmates()
                .addSnapshotListener((value, error) -> {
                    if(error != null){
                        mTaskResultObservable.onNext(new ErrorLiveEvent(error));
                        Log.e(TAG, "observeWorkmates: ", error);
                    }
                    if(value != null) {
                        List<Workmate> workmateList = new ArrayList<>();
                        for (DocumentSnapshot document : value.getDocuments()) {
                            Workmate workmate = document.toObject(Workmate.class);
                            if(workmate.getChosenRestaurantDate() != null && !isToday(workmate.getChosenRestaurantDate())){
                                workmate.setChosenRestaurantName("");
                            }
                            workmateList.add(workmate);
                        }
                        mWorkmateListObservable.onNext(workmateList);
                    }
                });
        return mWorkmateListObservable;
    }

    public Observable<List<Workmate>> getInterestedWorkmatesForRestaurants(String restaurantId){
        return Observable.create(
                emitter ->
                        WorkmateHelper.getTodayWorkmateInterestedForRestaurantId(restaurantId)
                                .addSnapshotListener((value, error) -> {
                                    if(error != null){
                                        mTaskResultObservable.onNext(new ErrorLiveEvent(error));
                                        Log.e(TAG, "setChosenRestaurantForUserId: ", error);
                                    }
                                    if(value != null){
                                        List<Workmate> interestedWorkmates = new ArrayList<>();
                                        for (DocumentSnapshot doc : value.getDocuments()) {
                                            interestedWorkmates.add(doc.toObject(Workmate.class));
                                        }
                                        emitter.onNext(interestedWorkmates);
                                    }
                                }));
    }

    public void setChosenRestaurantForUserId(String restaurantUId, String restaurantName) {
        WorkmateHelper.setChosenRestaurantForUserId(mCurrentUser.getUId(), restaurantUId, restaurantName)
                .addOnFailureListener(error -> {
                    mTaskResultObservable.onNext(new ErrorLiveEvent(error));
                    Log.e(TAG, "setChosenRestaurantForUserId: ", error);
                });
    }

    public void removeChosenRestaurantForUserId() {
        WorkmateHelper.removeChosenRestaurantForUserId(mCurrentUser.getUId())
                .addOnFailureListener(error -> {
                    mTaskResultObservable.onNext(new ErrorLiveEvent(error));
                    Log.e(TAG, "removeChosenRestaurantForUserId: ", error);
                });
    }

    public void setLikedRestaurants(List<String> likedRestaurants) {
        WorkmateHelper.setLikedRestaurantForCurrentUser(mCurrentUser.getUId(), likedRestaurants)
                .addOnFailureListener(error -> {
                    mTaskResultObservable.onNext(new ErrorLiveEvent(error));
                    Log.e(TAG, "setLikedRestaurants: ", error);
                });
    }

    public Observable<LiveEvent> observeTasksResults(){
        return mTaskResultObservable;
    }

    public void updateCurrentUserNickname(String nickname) {
        if(!nickname.isEmpty() && !nickname.equals(mCurrentUser.getNickname())) {
            WorkmateHelper.updateWorkmateNickname(mCurrentUser.getUId(), nickname);
        }
    }

    public void updateCurrentUserProfileUrl(String url) {
        WorkmateHelper.updateCurrentUserProfileUrl(mCurrentUser.getUId(), url);
    }

    public void updateCurrentUserProfilePic(Uri uriNewProfilePic) {
        if(uriNewProfilePic != null) {
            String uuid = UUID.randomUUID().toString();

            StorageReference profilePicRef = FirebaseStorage.getInstance().getReference(uuid);
            profilePicRef.putFile(uriNewProfilePic)
                    .addOnSuccessListener(taskSnapshot ->
                            profilePicRef.getDownloadUrl()
                                    .addOnSuccessListener(uri ->
                                            updateCurrentUserProfileUrl(uri.toString())))
                    .addOnFailureListener(error -> {
                        mTaskResultObservable.onNext(new ErrorLiveEvent(error));
                        Log.e(TAG, "setLikedRestaurants: ", error);
                    });
        }
    }
}


