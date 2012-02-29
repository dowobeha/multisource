package joshua.util;

import java.util.BitSet;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;


/**
 * Convenience class for manipulating source language coverage vectors
 * in the context of phrase-based machine translation.
 * 
 * This class attempts to minimize memory footprint by maintaining a
 * 
 * @author Lane Schwartz
 */
public final class CoverageVector extends CacheLoader<int[],BitSet> {

	public BitSet load(int[] coveredIndices) {
		
		int lastArrayIndex = coveredIndices.length-1;
		BitSet vector;
		
		if (lastArrayIndex >= 0) {
			int maxIndex = coveredIndices[coveredIndices.length-1];
			vector = new BitSet(maxIndex);
			
			for (int index : coveredIndices) {
				vector.set(index);
			}
			
		} else {
			vector = new BitSet(0);
		}

		return vector;
	}
	
	private static final LoadingCache<int[],BitSet> cache =
			CacheBuilder.newBuilder().softValues().build(new CoverageVector());
		
	
	public static final BitSet getEmptyCoverageVector() {
		return cache.getUnchecked(new int[]{});
	}
	
	public static final BitSet get(int index) {
		return cache.getUnchecked(new int[]{index});
	}
	
	public static final BitSet get(int startIndex, int endIndex) {
		
		int numberOfIndices = endIndex-startIndex+1;
		int[] coveredIndices = new int[numberOfIndices];
		for (int i=0; i<numberOfIndices; i+=1) {
			coveredIndices[i] = startIndex + i;
		}
		
		return cache.getUnchecked(coveredIndices);
	}
	
	public static final BitSet get(int[] coveredIndices) {
		
		return cache.getUnchecked(coveredIndices);

	}
	
	
	public static final BitSet merge(BitSet a, BitSet b) {
		
		int aLength = a.length();
		int bLength = b.length();
		
		int maxIndex = (aLength >= bLength) ? aLength : bLength;
		
		final BitSet result = new BitSet(maxIndex);
		result.or(a);
		result.or(b);
				
		int numberOfBitsSet=result.cardinality();
		int[] coveredIndices = new int[numberOfBitsSet];
		int fromIndex = 0;
		for (int i=0; i<numberOfBitsSet; i+=1) {
			coveredIndices[i] = result.nextSetBit(fromIndex);
			fromIndex = coveredIndices[i] + 1; 
		}
		
		BitSet vector = cache.getIfPresent(coveredIndices);
		
		if (vector==null) {
			cache.put(coveredIndices, result);
			vector = result;
		}
		
		return vector;
	}
	
}
