package joshua.multilingual;

import iso639.Language;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
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
		final Map<Language,Set<BitSet>> coverageVectors;
		final Map<Integer,Node> children;
		final int depth;
		
		Node() {
			this(0);
		}
		
		private Node(int depth) {
			this.depth = depth;
			id = nodeCounter.getAndIncrement();
			coverageVectors = new HashMap<Language,Set<BitSet>>();
			children = new TreeMap<Integer,Node>();
		}
		
		public void reachable(Language sourceLanguage) {
			if (! coverageVectors.containsKey(sourceLanguage)) {
				coverageVectors.put(sourceLanguage, new HashSet<BitSet>());
			}
		}
		
		public void annotate(Language sourceLanguage, BitSet coverageVector) {
			Set<BitSet> set = coverageVectors.get(sourceLanguage);
			if (set==null) {
				throw new Error("Bug in the code: Node.reachable should have been previously called, but was not");
			}
			set.add(coverageVector);
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
	
	public ReachableTranslations(Collection<TranslationOptions> languages) {
		
		this.nodeCounter = new AtomicLong(0);
		
		Queue<Node> queue = new LinkedList<Node>();
		
		Node root = new Node();
		for (TranslationOptions translationOptions : languages) {
			Language sourceLanguage = translationOptions.getSourceLanguage();
			root.reachable(sourceLanguage);
			root.annotate(sourceLanguage,CoverageVector.getEmptyCoverageVector());
		}
		
		queue.add(root);

		Set<BitSet> extendableCoverageVectors = new HashSet<BitSet>();
		
		while (! queue.isEmpty()) {

			Node startNode = queue.remove();
			
			// For each language
			for (TranslationOptions translationOptions : languages) {
				
				Language sourceLanguage = translationOptions.getSourceLanguage();
				Set<BitSet> coverageVectors = startNode.coverageVectors.get(sourceLanguage);
				
				// If start_node has been previously annotated with a source l coverage vector
				if (! coverageVectors.isEmpty()) {
				
					// For each translation option
					for (Map.Entry<BitSet, List<Rule>> entry : translationOptions.entrySet()) {

						// q = o's source l coverage vector
						BitSet coverageVector = entry.getKey();

						// from all coverage vectors stored at start_node,
						// collect the set of coverage vectors s that 
						// can be legally extended by q
						extendableCoverageVectors.clear();
						for (BitSet startVector : coverageVectors) {
							if (! coverageVector.intersects(startVector)) {
								extendableCoverageVectors.add(startVector);
							}
						}

						// if set s is not empty
						if (! extendableCoverageVectors.isEmpty()) {

							Node node = startNode;

							// for each target word w in o
							for (Rule rule : entry.getValue()) {
								int[] translationOption = rule.getEnglish();
								for (int targetWord : translationOption) {
									
									// node = expand_trie(node,w)
									node = node.expand(targetWord);

									// annotate node as reachable using l
									node.reachable(sourceLanguage);
									
								}
							}
							
							// for each coverage vector p in s		
							for (BitSet extendableStartVector : extendableCoverageVectors) {

								// coverage vector r = p intersect q
								BitSet extendedVector = 
										CoverageVector.merge(extendableStartVector, coverageVector);
								
								// annotate node with new coverage vector r
								node.annotate(sourceLanguage, extendedVector);
								
							}
						}				
					}
				}
			}
			
		}
		
	}
	
}
