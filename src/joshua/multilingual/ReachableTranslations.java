package joshua.multilingual;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicLong;

import joshua.decoder.ff.tm.Trie;
import joshua.util.CoverageVector;

public class ReachableTranslations {

	private final AtomicLong nodeCounter = new AtomicLong();
	
	private final class Node {
		
		final long id;
		
		Node() {
			id = nodeCounter.getAndIncrement();
		}
		
		public boolean equals(Object o) {
			if (o instanceof Node) {
				Node other = (Node)o;
				return id==other.id;
			} else {
				return false;
			}
		}
		
		public int hashCode() {
			return (int)(id ^ (id >>> 32));
		}
	}
	
	public ReachableTranslations(Trie translationGrammar, int[] sourceSentence) {
		
		BitSet empty = CoverageVector.getEmptyCoverageVector();

	}
	
}
