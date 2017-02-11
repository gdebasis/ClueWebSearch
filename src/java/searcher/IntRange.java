package searcher;

import java.util.Iterator;

/**
 * Utility class for an iterable integral close-open range
 */
public class IntRange implements Iterable<Integer> {

    private final int inclusiveStart;
    private final int exclusiveEnd;

    public IntRange(int inclusiveStart, int exclusiveEnd) {
        this.inclusiveStart = inclusiveStart;
        this.exclusiveEnd = exclusiveEnd;
    }
    
    public IntRange limit(int inclusiveStart, int exclusiveEnd) {
        return new IntRange(
                Math.max(inclusiveStart, this.inclusiveStart), 
                Math.min(exclusiveEnd, this.exclusiveEnd)
        );
    }

    public int getInclusiveStart() {
        return inclusiveStart;
    }

    public int getExclusiveEnd() {
        return exclusiveEnd;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            private int count = inclusiveStart;
            
            @Override
            public boolean hasNext() {
                return count < exclusiveEnd;
            }
            
            @Override
            public Integer next() {
                return count++;
            }            
        };
    }

    @Override
    public String toString() {
        return "[" + inclusiveStart + ", " + exclusiveEnd + ")";
    }
}
