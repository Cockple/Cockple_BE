package umc.cockple.demo.domain.exercise.utils;

/*
 * 월간 지도 반경 검색용 bounding box 계산 값 객체다.
 * 입력 검증과 MySQL WKT 생성은 담당하지 않고, 중심 좌표와 반경으로 사각 후보 영역만 계산한다.
 */
public record ExerciseMapBoundingBox(
        double minLatitude,
        double maxLatitude,
        double minLongitude,
        double maxLongitude
) {

    private static final double KILOMETERS_PER_LATITUDE_DEGREE = 111.045;
    private static final double MIN_LONGITUDE_COSINE = 1.0e-12;
    private static final double MIN_LATITUDE = -90.0;
    private static final double MAX_LATITUDE = 90.0;
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;

    public static ExerciseMapBoundingBox from(double latitude, double longitude, double radiusKm) {
        double deltaLatitude = radiusKm / KILOMETERS_PER_LATITUDE_DEGREE;
        double latitudeCosine = Math.cos(Math.toRadians(latitude));
        double safeLatitudeCosine = Math.max(Math.abs(latitudeCosine), MIN_LONGITUDE_COSINE);
        double deltaLongitude = radiusKm / (KILOMETERS_PER_LATITUDE_DEGREE * safeLatitudeCosine);

        return new ExerciseMapBoundingBox(
                latitude - deltaLatitude,
                latitude + deltaLatitude,
                longitude - deltaLongitude,
                longitude + deltaLongitude
        );
    }

    public boolean isWithinCoordinateRange() {
        return isValidLatitude(minLatitude)
                && isValidLatitude(maxLatitude)
                && isValidLongitude(minLongitude)
                && isValidLongitude(maxLongitude);
    }

    private static boolean isValidLatitude(double latitude) {
        return Double.isFinite(latitude)
                && latitude >= MIN_LATITUDE
                && latitude <= MAX_LATITUDE;
    }

    private static boolean isValidLongitude(double longitude) {
        return Double.isFinite(longitude)
                && longitude >= MIN_LONGITUDE
                && longitude <= MAX_LONGITUDE;
    }
}
