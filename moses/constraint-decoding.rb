#!/usr/bin/ruby -w
# -*- coding: utf-8 -*-

require 'optparse'
require 'ostruct'
require 'zlib'

options = OpenStruct.new
OptionParser.new { |opts|

  opts.banner = "Usage:	#{$0} [options]"

  opts.on("--ttable FILE", String, "Moses phrase table") { |v|
    options.ttable = File.expand_path(v)
  }

#  opts.on("--osymbols FILE", String, "Location for FST output symbol table") { |v|
#    options.osymbols = File.expand_path(v)
#  }

  opts.on("--isymbols FILE", String, "Location for FST input symbol table") { |v|
    options.isymbols = File.expand_path(v)
  }


  opts.on("--textfst FILE", String, "Location for FST output text file") { |v|
    options.textfst = File.expand_path(v)
  }

  opts.on("--source FILE", String, "Location for source sentence file") { |v|
    options.source = File.new(File.expand_path(v)).gets
  }

  opts.on("--constraint FILE", String, "Location for constraint sentence file") { |v|
    options.constraint = File.new(File.expand_path(v)).gets
  }


  if ARGV.length==0
    STDERR.puts opts
    exit
  end

}.parse!

if options.ttable==nil || options.isymbols==nil || options.textfst==nil || options.source==nil || options.constraint==nil
  STDERR.puts "Missing a parameter"
  exit
end


class Phrase

  attr_reader :phrase, :start_index, :end_index, :sentence_length

  def initialize(phrase,start_index,end_index,sentence_length)
    @phrase = phrase
    @start_index = start_index
    @end_index = end_index
    @sentence_length = sentence_length
  end

  def <=>(other)
    return_value = (@phrase <=> other.phrase)
    if ( return_value == 0 ) 
      return_value = (@start_index <=> other.start_index)
      if ( return_value == 0 ) 
        return_value = (@end_index <=> other.end_index)
      end
    end
    return return_value
  end

  def start_state
    return @start_index
  end

  def end_state
    return @end_index + 1
  end

  def to_s
    return "#{@start_index}-#{@end_index}\t#{@phrase}"
  end

end


class TranslationOption

  attr_reader :source_phrase, :constraining_phrase, :features, :other_part, :counts, :line_number

  def initialize(source_phrase,constraining_phrase,pt_line_parts,line_number)
    @source_phrase = source_phrase
    @constraining_phrase = constraining_phrase
    @features = pt_line_parts[2]
    @other_part = pt_line_parts[3]
    @counts = pt_line_parts[4]
    @line_number = line_number
  end

  def p_f_e
    return -Math.log(features.split[0].to_f)
  end

#  def <=>(other)
#    return_value = (@constraining_phrase.phrase <=> other.constraining_phrase)
#    if ( return_value == 0 ) 
#      return_value = (@source_phrase <=> other.source_phrase)
#    end
#    return return_value
#  end

  def <=>(other)
    return_value = (@constraining_phrase.start_state <=> other.constraining_phrase.start_state)
    if ( return_value == 0 )
      return_value = (@constraining_phrase.end_state <=> other.constraining_phrase.end_state)
    end
    return return_value
  end

  def coverage_vector
    result=""
    0.upto(source_phrase.start_index-1) { |i| result += "0" }
    source_phrase.start_index.upto(source_phrase.end_index) { |i| result += "1" }
    (source_phrase.end_index+1).upto(source_phrase.sentence_length-1) { |i| result += "0" }
    return result
  end

  def to_s
    return "#{@source_phrase.start_state}-#{@source_phrase.end_state}\t#{@constraining_phrase.start_state}-#{@constraining_phrase.end_state}\t#{@source_phrase.phrase} ||| #{@constraining_phrase.phrase} ||| #{features} ||| #{other_part} ||| #{counts}"
  end

end

def process(pt_file,source_phrases,constraining_phrases)
  result=Array.new
  phrase_index=0
  line_number=0
  pt_file.each_line {|line|
    line_number += 1
    parts=line.split(/ *\|\|\| */)
    source_phrases.each { |source_phrase|
      compare_sources = (source_phrase.phrase <=> parts[0])
      if (compare_sources==0)
        constraining_phrases.each { |constraining_phrase|
          if (constraining_phrase.phrase == parts[1])
            result.push(TranslationOption.new(source_phrase,constraining_phrase,parts,line_number))
#            puts result.last
#            puts "#{source_phrase.start_index}-#{source_phrase.end_index}\t#{constraining_phrase.start_index}-#{constraining_phrase.end_index}\t#{line}"        
          end
        }
#      elsif (compare_sources > 0)
#        break
      end
    }
  }
  return result
end



#source="افغانستان : 70 سے زایٔد ہلاکتیں"
#source="پہلے واقعہ میں خود کش حملہ آور نے صوبایٔی پولیس اسٹیشن کی حدود میں خود کو اڑا دیا جس میں تین اہلکار مارے گیٔے جبکہ دوسرے حملہ آور کو حملے سے قبل ہی ہلاک کردیا گیا ."
source=options.source
source_words=source.strip.split
source_phrases=Array.new
0.upto(source_words.size-1) { |i|
  i.upto(source_words.size-1) { |j|
    source_phrases.push(Phrase.new(source_words.slice(i,j-i+1).join(" "),i,j,source_words.size))
  }
}
source_phrases.sort!
#puts "#{source_words.size} source words"

#constraint="afghanistan : more than 70 deaths"
#constraint="in the first incident , suicide bomber blew himself up within the boundary of the provincial police station which killed three officials while the second attacker was killed before he could make an attack ."
constraint=options.constraint
constraint_words=constraint.strip.split
constraint_map = Array.new(constraint_words.size) {|i| Array.new(constraint_words.size-i+1) }

constraining_phrases = Array.new
0.upto(constraint_words.size-1) { |i|
  i.upto(constraint_words.size-1) { |j|
    constraint_map[i][j] = constraint_words.slice(i,j-i+1).join(" ")
    constraining_phrases.push(Phrase.new(constraint_map[i][j],i,j,constraint_words.size))
  }
}

constraining_phrases.sort!
#.each { |phrase| puts phrase }

file=File.new(options.ttable)
if (options.ttable =~ /.*\.gz$/)
  file=Zlib::GzipReader.new(file)
end

translation_options=process(file,source_phrases,constraining_phrases)
file.close



output_symbol_map=Hash.new
output_symbol_map["<eps>"] = 0
#dummy_coverage_vectors=Array.new
#0.upto(constraint_words.size-1) { |start_state|
#  end_state=start_state+1
#  coverage_vector=""
#  0.upto(start_state-1) { |i| coverage_vector += "0" }
#  start_state.upto(end_state-1) { |i| coverage_vector += "1" }
#  (end_state).upto(constraint_words.size-1) { |i| coverage_vector += "0" }  
#  output_symbol_map[coverage_vector] = output_symbol_map.size
#  dummy_coverage_vectors.push(coverage_vector)
#}
empty_coverage_vector = ""
source_words.size.times { |i| empty_coverage_vector += "0" }
output_symbol_map[empty_coverage_vector] = output_symbol_map.size

translation_options.each { |translation_option|
  output_symbol = translation_option.coverage_vector #+ " #{translation_option.source_phrase.start_index}-#{translation_option.source_phrase.end_index} #{translation_option.source_phrase.phrase}"
  unless output_symbol_map.member?(output_symbol)
    output_symbol_map[output_symbol] = output_symbol_map.size
  end
}

#File.open(options.osymbols, "w") { |osymbols_file|
#  output_symbol_map.sort{|a,b| a[1] <=> b[1] }.each { |pair| 
#    osymbols_file.puts "#{pair[0]}\t#{pair[1]}" 
#  }
#}

File.open(options.isymbols, "w") { |isymbols_file|
  isymbols_file.puts "<eps>\t0"
}

File.open(options.textfst, "w") { |textfst_file|

  #dummy_coverage_vectors.each_with_index {|coverage_vector,index|
  #  textfst_file.puts "#{index}\t#{index+1}\t<eps>\t#{coverage_vector}\t1000.0"
  #}
  0.upto(constraint_words.size-1) { |index|
    textfst_file.puts "#{index}\t#{index+1}\t0\t#{empty_coverage_vector}\t1000.0"
  }

  translation_options.sort.each { |translation_option|
    textfst_file.puts "#{translation_option.constraining_phrase.start_state}\t#{translation_option.constraining_phrase.end_state}\t#{translation_option.line_number}\t#{translation_option.coverage_vector}\t#{translation_option.p_f_e}"
  }

  textfst_file.puts "#{constraint_words.size}\t0"

}
