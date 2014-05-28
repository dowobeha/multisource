#!/usr/bin/ruby -w
#
# Author: Lane Schwartz <dowobeha@gmail.com>
# Script to convert Moses search graph to OpenFST fsm format
#
# Based on Loic Barrault's sg2dot.perl script

require 'optparse'
require 'ostruct'
require 'set'
require 'zlib'

options = OpenStruct.new
OptionParser.new { |opts|

  opts.banner = "Usage:	#{$0} [options]"

  opts.on("--search_graph FILE", String, "Moses search graph") { |v|
    options.search_graph = File.expand_path(v)
  }

  opts.on("--isymbols FILE", String, "Location for FST input symbol table") { |v|
    options.isymbols = File.expand_path(v)
  }

  opts.on("--textfst FILE", String, "Location for FST output text file") { |v|
    options.textfst = File.expand_path(v)
  }

  if ARGV.length==0
    STDERR.puts opts
    exit
  end

}.parse!

if options.search_graph==nil || options.isymbols==nil || options.textfst==nil
  STDERR.puts "Missing a parameter"
  exit
end

file=File.new(options.search_graph)
if (options.search_graph =~ /.*\.gz$/)
  file=Zlib::GzipReader.new(file)
end

final_states = Set.new

class SymbolTable

  def initialize(zero_key)
    @map = Hash.new
    @map[zero_key] = 0
  end

  def get(key)
    value = @map[key]
    if value==nil
      value = @map.size
      @map[key] = value
    end
    # puts "#{key} becomes #{value}"
    return value
  end

  def getNext
    value = @map.size
    @map[value] = value
    return value
  end

  def sort
    return @map.sort{|a,b| a[1]<=>b[1]}
  end

end

states=SymbolTable.new(0)
vocab=SymbolTable.new("<eps>")

outfile=File.new(options.textfst,"w")

file.each_line { |line|

  from = "";
  to = "";
  forward = ""
  score = "";
  out = "";
  validLine = false;

  line.strip!

  #Three kinds of lines in search-graph-extended
  #0 hyp=0 stack=0
  #0 hyp=47 stack=1 back=0 score=-8.651 transition=-8.651 forward=4645 fscore=-17.576 covered=2-2 scores=[ d: -2.000 w: -2.000 u: 0.000 d: 0.000 0.000 -1.609 0.000 0.000 0.000 lm: -16.126 tm: 0.000 -1.130 -3.466 -3.931 1.000 ] out=70|contributes 70
  #0 hyp=47 stack=1 back=0 score=-9.040 transition=-9.040 recombined=47 forward=4645 fscore=-17.576 covered=2-2 scores=[ d: -2.000 w: -2.000 u: 0.000 d: 0.000 0.000 -0.511 0.000 0.000 0.000 lm: -16.474 tm: 0.000 -0.927 -3.466 -6.858 1.000 ] out=70|over 70


  #0 hyp=5 stack=1 back=0 score=-0.53862 transition=-0.53862 forward=181 fscore=-205.36 covered=0-0 out=I am , pC=-0.401291, c=-0.98555
  #256 hyp=6566 stack=2 back=23 score=-2.15644 transition=-0.921959 recombined=6302 forward=15519 fscore=-112.807 covered=2-2 out=countries , , pC=-0.640574, c=-1.07215

  if (line =~ /.+back=(\d+).+recombined=(\d+) forward=(\S+).+ tm: (\S+) .+out=.+\|(.+)/)

    from = $1.strip.to_i;
    to = $2.strip.to_i;
    forward = $3.strip.to_i
    score = $4.to_f
    score = -score if score < 0
    out = $5.strip;	
    validLine = true;

  elsif (line =~ /hyp=(\d+).+back=(\d+).+forward=(\S+).+ tm: (\S+) .+out=.+\|(.+)/)

    to = $1.strip.to_i
    from = $2.strip.to_i
    forward = $3.strip.to_i
    score = $4.to_f
    score = -score if score < 0
    out = $5.strip;
    validLine = true;

  end

  if (validLine)

    #STDOUT.puts "************* #{out} ****************"
    words = out.split
    
    from = states.get(from)
    words.each_with_index { |word,index|
      word_id=vocab.get(word)
      if (index < words.size-1)
        myto=states.getNext
        myscore=""
      else
        myto=states.get(to)
        final_states.add(myto) if forward==-1
        myscore="\t#{score}"
      end
      outfile.puts "#{from}\t#{myto}\t#{word_id}#{myscore}" 
      from = myto
    }
    
    

  end

}

file.close

#goal_state=states.getNext
final_states.each { |final_state|
#  STDOUT.puts "#{final_state}\t#{goal_state}"
  outfile.puts final_state
}
#STDOUT.puts "#{goal_state}"

outfile.close

vocabfile=File.new(options.isymbols,"w")

vocab.sort.each { |key,value|
  vocabfile.puts "#{key}\t#{value}"
}

vocabfile.close

