package umc.cockple.demo.domain.exercise.service.support.calculator;

import org.springframework.stereotype.Component;

@Component
public class ExerciseDistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371;

    public double calculate(double latitude, double longitude, double latitude1, double longitude1) {
        double latDistance = Math.toRadians(latitude1 - latitude);
        double lonDistance = Math.toRadians(longitude1 - longitude);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(latitude1))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (float) (EARTH_RADIUS_KM * c);
    }
}
