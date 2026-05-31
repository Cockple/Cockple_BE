package umc.cockple.demo.domain.exercise.repository.support;

import java.util.Locale;

/*
 * 월간 지도 spatial 조회 조건 값 객체다.
 *
 * 입력 값 검증은 ExerciseMapBuildingsDTO.Query 책임이다.
 * 이 객체는 검증이 끝난 좌표와 반경을 MySQL spatial query 파라미터로 변환하는 adapter 역할만 한다.
 * MySQL ST_GeomFromText(..., 'axis-order=long-lat') 계약에 맞춰
 * WKT는 항상 longitude latitude 순서로 생성한다.
 */
public record ExerciseMapSpatialSearchCondition(
        double latitude,
        double longitude,
        double radiusKm,
        String centerPointWkt,
        String boundingBoxWkt
) {

    private static final double KILOMETERS_PER_LATITUDE_DEGREE = 111.045;
    private static final double MIN_LONGITUDE_COSINE = 1.0e-12;

    public static ExerciseMapSpatialSearchCondition from(double latitude, double longitude, double radiusKm) {
        BoundingBox boundingBox = BoundingBox.from(latitude, longitude, radiusKm);

        return new ExerciseMapSpatialSearchCondition(
                latitude,
                longitude,
                radiusKm,
                pointWkt(longitude, latitude),
                polygonWkt(
                        boundingBox.minLongitude(),
                        boundingBox.minLatitude(),
                        boundingBox.maxLongitude(),
                        boundingBox.maxLatitude())
        );
    }

    private static String pointWkt(double longitude, double latitude) {
        return String.format(Locale.ROOT, "POINT(%s %s)", longitude, latitude);
    }

    private static String polygonWkt(double minLongitude, double minLatitude,
                                     double maxLongitude, double maxLatitude) {
        return String.format(Locale.ROOT,
                "POLYGON((%s %s,%s %s,%s %s,%s %s,%s %s))",
                minLongitude, minLatitude,
                maxLongitude, minLatitude,
                maxLongitude, maxLatitude,
                minLongitude, maxLatitude,
                minLongitude, minLatitude);
    }

    private record BoundingBox(
            double minLatitude,
            double maxLatitude,
            double minLongitude,
            double maxLongitude
    ) {
        private static BoundingBox from(double latitude, double longitude, double radiusKm) {
            double deltaLatitude = radiusKm / KILOMETERS_PER_LATITUDE_DEGREE;
            double latitudeCosine = Math.cos(Math.toRadians(latitude));
            double safeLatitudeCosine = Math.max(Math.abs(latitudeCosine), MIN_LONGITUDE_COSINE);
            double deltaLongitude = radiusKm / (KILOMETERS_PER_LATITUDE_DEGREE * safeLatitudeCosine);

            return new BoundingBox(
                    latitude - deltaLatitude,
                    latitude + deltaLatitude,
                    longitude - deltaLongitude,
                    longitude + deltaLongitude
            );
        }
    }
}
