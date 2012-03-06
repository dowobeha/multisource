package joshua.pbmt;


import iso639.Language;

import java.util.AbstractMap;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.Trie;
import joshua.util.CoverageVector;

public class TranslationOptions extends AbstractMap<BitSet,List<Rule>> /*implements Iterable<String>*/ {

	private final Map<BitSet, List<Rule>> entries;
	private final List<BitSet> coverageVectors;
	private final SymbolTable vocab;
	private final Language source;
	private final Language target;
	private final int[] sourceSentence;
	private final int reorderingLimit;
	
	public TranslationOptions(Language source, Language target, Trie translationGrammar, int[] sourceSentence, SymbolTable vocab, int maxPhraseLength, int reorderingLimit) {
		this.entries = new HashMap<BitSet, List<Rule>>();
		this.coverageVectors = new LinkedList<BitSet>();
		this.vocab = vocab;
		this.source = source;
		this.target = target;
		this.sourceSentence = sourceSentence;
		this.reorderingLimit = reorderingLimit;
		
		for (int startIndex=0, endOfSentence=sourceSentence.length-1;
				startIndex<=endOfSentence; startIndex+=1) {
			
			Trie trie = translationGrammar;
			
			for (int endIndex=startIndex,
					// the phrase could be as long as maxPhraseLength
					possibleLastEndIndex=startIndex+maxPhraseLength-1,
					// but the phrase can't go past the end of the sentence
					lastEndIndex=(possibleLastEndIndex<endOfSentence) ? possibleLastEndIndex : endOfSentence;
					endIndex<=lastEndIndex; endIndex+=1) {
				
				trie = trie.matchOne(sourceSentence[endIndex]);
				
				if (trie==null || !trie.hasRules()) {
					break;
				} else {
					List<Rule> rules = trie.getRules().getRules();
					BitSet coverageVector = CoverageVector.get(startIndex, endIndex);
					coverageVectors.add(coverageVector);
					entries.put(coverageVector, rules);
				}
			
			}
			
		}
	}

	public SymbolTable getVocabulary() {
		return this.vocab;
	}
	
	public int getSourceSentenceLength() {
		return this.sourceSentence.length;
	}
	
	public Language getSourceLanguage() {
		return this.source;
	}
	
	public Language getTargetLanguage() {
		return this.target;
	}
	
	public int getReorderingLimit() {
		return this.reorderingLimit;
	}
	
	public int numberOfRules() {
		int count=0;
		for (Map.Entry<BitSet, List<Rule>> entry : entries.entrySet()) {
			count+=entry.getValue().size();
		}
		return count;
	}
	
	@Override
	public Set<Map.Entry<BitSet, List<Rule>>> entrySet() {
		return entries.entrySet();
	}

	public String toString() {
		StringBuilder s = new StringBuilder();
		String newLine = String.format("%n");
		
		for (BitSet coverageVector : coverageVectors) {
			List<Rule> rules = entries.get(coverageVector);
			for (Rule rule : rules) {
				s.append(vocab.getWords(rule.getEnglish()));
				s.append(newLine);
			}
		}
		
		return s.toString();
	}

//	@Override
//	public Iterator<String> iterator() {
//		
//	}
	
}
