package umc.cockple.demo.domain.exercise.service.query.model;

import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.utils.ExerciseMapBoundingBox;

import java.time.LocalDate;

public record ExerciseMapSearchQuery(
        LocalDate date,
        Double latitude,
        Double longitude,
        Double radiusKm
) {
    private static final double MIN_LATITUDE = -90.0;
    private static final double MAX_LATITUDE = 90.0;
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;

    public ExerciseMapSearchQuery {
        validateLocationCompleteness(latitude, longitude);
        validateCoordinate(latitude, longitude);
        validateRadius(radiusKm);
        validateBoundingBox(latitude, longitude, radiusKm);
    }

    public static ExerciseMapSearchQuery of(LocalDate date, Double latitude, Double longitude, Double radiusKm) {
        return new ExerciseMapSearchQuery(date, latitude, longitude, radiusKm);
    }

    public ExerciseMapSearchQuery withFallbackLocation(Double fallbackLatitude, Double fallbackLongitude) {
        if (latitude != null && longitude != null) {
            return this;
        }

        return new ExerciseMapSearchQuery(date, fallbackLatitude, fallbackLongitude, radiusKm);
    }

    private static void validateLocationCompleteness(Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new ExerciseException(ExerciseErrorCode.INCOMPLETE_LOCATION_INFO);
        }
    }

    private static void validateCoordinate(Double latitude, Double longitude) {
        if (latitude == null && longitude == null) {
            return;
        }

        if (!isValidLatitude(latitude) || !isValidLongitude(longitude)) {
            throw new ExerciseException(ExerciseErrorCode.INVALID_LOCATION_INFO);
        }
    }

    private static void validateRadius(Double radiusKm) {
        if (radiusKm == null || !Double.isFinite(radiusKm) || radiusKm <= 0) {
            throw new ExerciseException(ExerciseErrorCode.INVALID_LOCATION_INFO);
        }
    }

    private static void validateBoundingBox(Double latitude, Double longitude, Double radiusKm) {
        if (latitude == null && longitude == null) {
            return;
        }

        if (!ExerciseMapBoundingBox.from(latitude, longitude, radiusKm).isWithinCoordinateRange()) {
            throw new ExerciseException(ExerciseErrorCode.INVALID_LOCATION_INFO);
        }
    }

    private static boolean isValidLatitude(Double latitude) {
        return latitude != null
                && Double.isFinite(latitude)
                && latitude >= MIN_LATITUDE
                && latitude <= MAX_LATITUDE;
    }

    private static boolean isValidLongitude(Double longitude) {
        return longitude != null
                && Double.isFinite(longitude)
                && longitude >= MIN_LONGITUDE
                && longitude <= MAX_LONGITUDE;
    }
}
