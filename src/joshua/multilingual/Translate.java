package joshua.multilingual;

import iso639.Language;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Scanner;

import joshua.corpus.suffix_array.ParallelCorpusGrammarFactory;
import joshua.corpus.vocab.SymbolTable;
import joshua.pbmt.TranslationOptions;
import joshua.prefix_tree.ExtractRules;
import joshua.prefix_tree.PrefixTree;

public class Translate {

	private final SymbolTable vocab;
	private final PrefixTree prefixTree;
	private final Scanner scanner;
	private final int maxPhraseLength;
	
	private final Language source;
	private final Language target;
	
	public Translate(Language source, Language target, String joshDir, String testSet, int maxPhraseLength) throws IOException, ClassNotFoundException {
		this.maxPhraseLength = maxPhraseLength;
		this.source = source;
		this.target = target;
		
		ExtractRules extractRules = new ExtractRules();	
		extractRules.setUsePrecomputedFrequentPhrases(false);
		extractRules.setMaxNonterminals(0);
		extractRules.setMaxPhraseLength(maxPhraseLength);
		extractRules.setMaxPhraseSpan(maxPhraseLength);
		extractRules.setJoshDir(joshDir);
		
		ParallelCorpusGrammarFactory parallelCorpus = extractRules.getGrammarFactory();
		this.vocab = parallelCorpus.getSuffixArray().getVocabulary();
		this.prefixTree = new PrefixTree(parallelCorpus);
		
		this.scanner = new Scanner(new File(testSet),"UTF-8");
	}
	
	public void processTestSet() {
		while (scanner.hasNextLine()) {
			TranslationOptions t = processNextSentence();
			new ReachableTranslations(Collections.singleton(t));
		}
	}
	
	private TranslationOptions processNextSentence() {
		String sentence = scanner.nextLine();
		String[] words = sentence.split("\\s+");
		int[] wordIDs = vocab.addTerminals(words);
		prefixTree.add(wordIDs);
		System.out.println(sentence);
		
		TranslationOptions translationOptions = 
				new TranslationOptions(source,target,prefixTree.getTrieRoot(),wordIDs,vocab,maxPhraseLength);
		//System.out.println(translationOptions.toString());
		System.out.println("Total number of translation options: " + translationOptions.numberOfRules());
		
//		return new ReachableTranslations(translationOptions); 
		return translationOptions;
	}

	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		
		Language source = iso639.Part3.get(args[0]);
		Language target = iso639.Part3.get(args[1]);
		String joshDir = args[2];
		String testSet = args[3];
		int maxPhraseLength = 5;
		
		Translate translate = new Translate(source,target,joshDir,testSet,maxPhraseLength);
		translate.processTestSet();
		
		System.out.println("Done");
	}
	
}
