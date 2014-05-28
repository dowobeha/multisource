#!/usr/bin/ruby -w

vector=nil

STDIN.each_line { |line|

  puts line
  parts = line.strip.split("") 
  if vector==nil
    vector=parts
#    vector.each_with_index {|b,i| puts "#{b}\t#{i}" }
  else
    parts.each_with_index {|b,i| 
      if (b=="1")
        vector[i] = b.to_i + vector[i].to_i
      end
    }
  end

}
puts
puts vector.join("")
