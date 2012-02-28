package joshua.multilingual;

import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import joshua.pbmt.TranslationOptions;
import joshua.util.CoverageVector;

public class ReachableTranslations {
	
	private final class Node {
		
		final long id;
		final Set<BitSet> coverageVectors;
		final Map<Integer,Node> children;
		
		Node() {
			id = nodeCounter.getAndIncrement();
			coverageVectors = new HashSet<BitSet>();
			children = new TreeMap<Integer,Node>();
		}
		
		public void annotate(BitSet coverageVector) {
			coverageVectors.add(coverageVector);
		}
		
//		public Iterable<BitSet> extendableBy(BitSet coverageVector) {
//			return new Iterable<BitSet>() {
//				@Override
//				public Iterator<BitSet> iterator() {
//					return new Iterator<BitSet>() {
//
//						final Iterator<BitSet> i = coverageVectors.iterator();
//						
//						BitSet nextElement = null;
//						
//						@Override
//						public boolean hasNext() {
//							
//							while (i.hasNext()) {
//								
//							}
//							
//							return false;
//						}
//
//						@Override
//						public BitSet next() {
//							return null;
//						}
//
//						@Override
//						public void remove() {
//							
//						}
//						
//					};
//				}
//				
//			};
//		}
		
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
	
	
	private final AtomicLong nodeCounter;
	
	public ReachableTranslations(TranslationOptions translationOptions) {
		
		this.nodeCounter = new AtomicLong(0);
		
//		Queue<Node> queue = new LinkedList<Node>();
//		
//		Node root = new Node();
//		root.annotate(CoverageVector.getEmptyCoverageVector());
//		
//		queue.add(root);
//
//		while (! queue.isEmpty()) {
//
//			Node startNode = queue.remove();
//			
//			if (! startNode.coverageVectors.isEmpty()) {
//				
//				
//				
//			}
//			
//		}
		
	}
	
}
