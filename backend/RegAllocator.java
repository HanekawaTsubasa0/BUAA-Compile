package backend;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public class RegAllocator {
    private final Deque<String> free = new ArrayDeque<>();

    public RegAllocator() {
        free.addAll(Arrays.asList("$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7"));
    }

    public String alloc() {
        if (free.isEmpty()) {
            throw new RuntimeException("Ran out of temporaries");
        }
        return free.pop();
    }

    public void release(String reg) {
        free.push(reg);
    }
}
