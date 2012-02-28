package joshua.multilingual;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import joshua.corpus.suffix_array.ParallelCorpusGrammarFactory;
import joshua.corpus.vocab.SymbolTable;
import joshua.prefix_tree.ExtractRules;
import joshua.prefix_tree.PrefixTree;

public class Translate {

	private final SymbolTable vocab;
	private final PrefixTree prefixTree;
	private final Scanner scanner;
	
	
	public Translate(String joshDir, String testSet) throws IOException, ClassNotFoundException {
		ExtractRules extractRules = new ExtractRules();	
		extractRules.setUsePrecomputedFrequentPhrases(false);
		extractRules.setMaxNonterminals(0);
		extractRules.setMaxPhraseLength(5);
		extractRules.setMaxPhraseSpan(5);
		extractRules.setJoshDir(joshDir);
		
		ParallelCorpusGrammarFactory parallelCorpus = extractRules.getGrammarFactory();
		this.vocab = parallelCorpus.getSuffixArray().getVocabulary();
		this.prefixTree = new PrefixTree(parallelCorpus);
		
		this.scanner = new Scanner(new File(testSet),"UTF-8");
	}
	
	public void processTestSet() {
		while (scanner.hasNextLine()) {
			processNextSentence();
		}
	}
	
	private ReachableTranslations processNextSentence() {
		String sentence = scanner.nextLine();
		String[] words = sentence.split("\\s+");
		int[] wordIDs = vocab.addTerminals(words);
		prefixTree.add(wordIDs);
		System.out.println(sentence);
		
		return new ReachableTranslations(prefixTree.getTrieRoot(),wordIDs); 
		
	}

	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		
		String joshDir = args[0];
		String testSet = args[1];
				
		Translate translate = new Translate(joshDir,testSet);
		translate.processTestSet();
		
		System.out.println("Done");
	}
	
}
