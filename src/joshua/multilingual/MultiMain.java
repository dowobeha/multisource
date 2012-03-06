package joshua.multilingual;

import iso639.Language;
import iso639.Language.ISO639_Part;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import joshua.pbmt.TranslationOptions;

public class MultiMain {

	private static final Logger logger =
			Logger.getLogger(MultiMain.class.getCanonicalName());
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		
		Set<Translate> set = new HashSet<Translate>();
		
		Language target = iso639.Part3.get("eng");
		for (String arg : args) {
			
			Language source = iso639.Part3.get(arg);
			String joshDir = 
					"/home/lane/joshua.workspace/multisource.git/europarl."
							+ source.identifier(ISO639_Part.Part3)
							+ "-"
							+ target.identifier(ISO639_Part.Part3)
							+ ".1-40.josh";
							
			String testSet = 
					"/home/lane/joshua.workspace/multisource.git/corpus/devtest.tok.norm."
//					"/home/lane/joshua.workspace/multisource.git/corpus/tiny."					
					+ source.identifier(ISO639_Part.Part1);
			
			int maxPhraseLength = 5;
			
			set.add(new Translate(source,target,joshDir,testSet,maxPhraseLength));
			
		}
		
		for (int sentenceNumber=1; sentenceNumber<=2489; sentenceNumber+=1) {
					
			Set<TranslationOptions> optionSet = new HashSet<TranslationOptions>();
			
			for (Translate translate : set) {
				if (sentenceNumber<75) {
					translate.skipNextSentence();
				} else {
					optionSet.add(translate.processNextSentence());
				}
			}
			
			if (sentenceNumber<75) {
				continue;
			}
			
			ReachableTranslations r = new ReachableTranslations(optionSet);
			r.reportStats();
			
			break;
		}
		
		
		logger.info("Done");
	}
	
}
