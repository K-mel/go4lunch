package com.camel.go4lunch.mappers;

import com.camel.go4lunch.R;
import com.camel.go4lunch.models.Workmate;

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;

import static com.camel.go4lunch.utils.Utils.isToday;

public class WorkmateToDetailsMapper implements Function< Workmate, Workmate>{

    String mRestaurantUId;

    public WorkmateToDetailsMapper(String restaurantUId) {
        mRestaurantUId = restaurantUId;
    }

    @Override
    public Workmate apply(@NonNull Workmate workmate) {

        workmate.setWorkmateGoFabColor(R.color.orange);

        if(workmate.getChosenRestaurantDate() != null){
            if(isToday(workmate.getChosenRestaurantDate()) && mRestaurantUId.equals(workmate.getChosenRestaurantId())){
                workmate.setWorkmateGoFabColor(R.color.green);
            }
        }

        if(workmate.getLikedRestaurants().contains(mRestaurantUId)){
            workmate.setWorkmateLikedRestaurantTvText(R.string.liked);
            workmate.setWorkmateLikedRestaurantTvColor(R.color.green);
        } else {
            workmate.setWorkmateLikedRestaurantTvText(R.string.like);
            workmate.setWorkmateLikedRestaurantTvColor(R.color.orange);
        }

        return workmate;
    }
}
