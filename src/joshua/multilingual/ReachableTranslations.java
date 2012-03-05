package joshua.multilingual;

import iso639.Language;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.vocab.SymbolTable;
import joshua.corpus.vocab.Vocabulary;
import joshua.decoder.ff.tm.Rule;
import joshua.pbmt.TranslationOptions;
import joshua.util.CoverageVector;

public class ReachableTranslations {
	
	private static final Logger logger =
			Logger.getLogger(ReachableTranslations.class.getCanonicalName());
	
	private final class Node {
		
		final long id;
		final Map<Language,Set<BitSet>> coverageVectors;
		final Map<Integer,Node> children;
		final int depth;
		final Node parent;
		
		Node() {
			this(null);
		}
		
		private Node(Node parent) {
			this.parent = parent;
			if (parent==null) {
				this.depth = 0;
			} else {
				this.depth = parent.depth + 1;
			}
			id = nodeCounter.getAndIncrement();
			coverageVectors = new HashMap<Language,Set<BitSet>>();
			children = new HashMap<Integer,Node>();
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
				child = new Node(this);
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
		
		public String toString() {
			StringBuilder s = new StringBuilder();
			Stack<Integer> stack = new Stack<Integer>();

			for (Node parent=this.parent, child=Node.this; 
					parent!=null; child=parent,parent=child.parent) {
				
				for (Map.Entry<Integer, Node> entries : parent.children.entrySet()) {
					Node node = entries.getValue();
					Integer wordID = entries.getKey();
					if (node.equals(child)) {
						stack.push(wordID);
						break;
					}
				}
			}
			
			while (! stack.isEmpty()) {
				Integer wordID = stack.pop();
				String word = vocab.getWord(wordID);
				s.append(word);
				if (! stack.isEmpty()) {
					s.append(' ');
				}
			}
			
			return s.toString();
		}
	}
	
	
	private final AtomicLong nodeCounter;
	
	private final Node root;
	private final Map<Language,BitSet> completedVectors;
	private final AtomicLong prunedNodeCounter;
	
	public void reportStats() {
		logger.info("Total number of nodes: " + nodeCounter.get());
		logger.info("Total number of pruned nodes: " + prunedNodeCounter.get());
		
		
		AtomicLong leafCounter = new AtomicLong(0);
		AtomicLong completeCounter = new AtomicLong(0);
		Collection<Node> completedChildren = new HashSet<Node>();
		countLeafNodes(root,leafCounter,completeCounter,completedChildren);
		logger.info("Total number of leaf nodes: " + leafCounter.get());
		logger.info("Total number of completed leaf nodes: " + completeCounter.get());
		
		if (logger.isLoggable(Level.FINEST)) {
			StringBuilder s = new StringBuilder();
			for (Node node : completedChildren) {
				s.append(node.toString());
				s.append('\n');
			}
			s.append(completedChildren.size());
			s.append('\n');
			logger.finest("Complete translations:\n" + s.toString());
		}
	}
	
	private void countLeafNodes(Node node, AtomicLong leafCounter, AtomicLong completeCounter, Collection<Node> completedChildren) {
		
		for (Node child : node.children.values()) {
			if (child.children.isEmpty()) {
				leafCounter.incrementAndGet();
				
				boolean childComplete = true;
				for (Map.Entry<Language, BitSet> entry : completedVectors.entrySet()) {
					Language sourceLanguage = entry.getKey();
					Set<BitSet> nodeVectors = child.coverageVectors.get(sourceLanguage);
					if (!nodeVectors.contains(entry.getValue())) {
						childComplete = false;
						break;
					}
				}
				if (childComplete) {
					completeCounter.incrementAndGet();
					completedChildren.add(child);
				}
				
			} else {
				countLeafNodes(child,leafCounter,completeCounter,completedChildren);
			}
		}
	}
	
	private final SymbolTable vocab = new Vocabulary(); 
	
	public ReachableTranslations(Collection<TranslationOptions> languages) {
		
		this.completedVectors = new HashMap<Language,BitSet>();
		for (TranslationOptions translationOptions : languages) {
			int length = translationOptions.getSourceSentenceLength();
			BitSet vector = CoverageVector.get(0, length-1);
			this.completedVectors.put(translationOptions.getSourceLanguage(), vector);
		}
		
		this.nodeCounter = new AtomicLong(0);
		this.prunedNodeCounter = new AtomicLong(0);
		
		Queue<Node> queue = new LinkedList<Node>();
		
		this.root = new Node();
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
				SymbolTable languageVocabulary = translationOptions.getVocabulary();
				
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

						List<Rule> rules = entry.getValue();
						
						// if set s is not empty
						if (!extendableCoverageVectors.isEmpty() && !rules.isEmpty()) {

							// for each target word w in o
							for (Rule rule : entry.getValue()) {
								Node node = startNode;
								int[] translationOption = rule.getEnglish();
								for (int targetWord : translationOption) {
									String targetString = languageVocabulary.getWord(targetWord);
									
									// node = expand_trie(node,w)
									node = node.expand(vocab.getID(targetString));

									// annotate node as reachable using l
									node.reachable(sourceLanguage);
									
								}
								
								if (node==startNode) {
									throw new RuntimeException("node==startNode, but it shouldn't");
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
			

			Collection<Integer> toPrune = new ArrayList<Integer>();
			// for each child of start_node
			pruning_children:
			for (Map.Entry<Integer, Node> entry : startNode.children.entrySet()) {
			//for (Node child : startNode.children.values()) {
				Node child = entry.getValue();
				Integer word = entry.getKey();
				
				// for each language l
				for (TranslationOptions translationOptions : languages) {
					Language sourceLanguage = translationOptions.getSourceLanguage();
					
					// if child is not reachable using l
					if (! child.coverageVectors.containsKey(sourceLanguage)) {

						// prune child
						toPrune.add(word);
						//startNode.children.remove(child);
						
						// continue pruning_children
						continue pruning_children;
						
					}
								
				}
				
				// add child to queue
				queue.add(child);
				
			}
			
			for (Integer word : toPrune) {
				startNode.children.remove(word);
				this.prunedNodeCounter.incrementAndGet();
			}
			
		}
		
	}
	
}
