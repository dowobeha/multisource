package joshua.multilingual;

import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import joshua.decoder.ff.tm.Rule;
import joshua.pbmt.TranslationOptions;
import joshua.util.CoverageVector;

public class ReachableTranslations {
	
	private final class Node {
		
		final long id;
		final Set<BitSet> coverageVectors;
		final Map<Integer,Node> children;
		final int depth;
		
		Node() {
			this(0);
		}
		
		private Node(int depth) {
			this.depth = depth;
			id = nodeCounter.getAndIncrement();
			coverageVectors = new HashSet<BitSet>();
			children = new TreeMap<Integer,Node>();
		}
		
		public void annotate(BitSet coverageVector) {
			coverageVectors.add(coverageVector);
		}
		
		public Node expand(int word) {
			Node child = children.get(word);
			if (child==null) {
				child = new Node(this.depth+1);
				children.put(word, child);
			}
			return child;
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
	
	
	private final AtomicLong nodeCounter;
	
	public ReachableTranslations(TranslationOptions translationOptions) {
		
		this.nodeCounter = new AtomicLong(0);
		
		Queue<Node> queue = new LinkedList<Node>();
		
		Node root = new Node();
		root.annotate(CoverageVector.getEmptyCoverageVector());
		
		queue.add(root);

		Set<BitSet> extendableCoverageVectors = new HashSet<BitSet>();
		
		while (! queue.isEmpty()) {

			Node startNode = queue.remove();
			
			if (! startNode.coverageVectors.isEmpty()) {
				
				for (Map.Entry<BitSet, List<Rule>> entry : translationOptions.entrySet()) {

					BitSet coverageVector = entry.getKey();
					
					extendableCoverageVectors.clear();
					for (BitSet startVector : startNode.coverageVectors) {
						if (! coverageVector.intersects(startVector)) {
							extendableCoverageVectors.add(startVector);
						}
					}
					
					if (! extendableCoverageVectors.isEmpty()) {
					
						Node node = startNode;
						
						for (Rule rule : entry.getValue()) {
							
							int[] translationOption = rule.getEnglish();
							
							for (int targetWord : translationOption) {
								node = node.expand(targetWord);
								
							}
							
							
						}
					}				
				}
			}
			
		}
		
	}
	
}
