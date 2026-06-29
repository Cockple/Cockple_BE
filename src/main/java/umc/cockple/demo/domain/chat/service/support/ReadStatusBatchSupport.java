package umc.cockple.demo.domain.chat.service.support;

import java.util.ArrayList;
import java.util.List;

public final class ReadStatusBatchSupport {

    public static final int IN_CLAUSE_CHUNK_SIZE = 500;

    private ReadStatusBatchSupport() {
    }

    public static <T> List<List<T>> chunk(List<T> values) {
        if (values.isEmpty()) {
            return List.of();
        }

        List<List<T>> chunks = new ArrayList<>(chunkCount(values.size()));
        for (int start = 0; start < values.size(); start += IN_CLAUSE_CHUNK_SIZE) {
            int end = Math.min(start + IN_CLAUSE_CHUNK_SIZE, values.size());
            chunks.add(values.subList(start, end));
        }

        return chunks;
    }

    private static int chunkCount(int size) {
        return (size + IN_CLAUSE_CHUNK_SIZE - 1) / IN_CLAUSE_CHUNK_SIZE;
    }
}
