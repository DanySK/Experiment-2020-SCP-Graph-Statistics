#!/usr/bin/env ruby
Dir["data/*"]
    .filter { | file_name | /.*\.txt$/ !~ file_name }
    .each { | file |
        puts "Renaming #{file}"
        File.rename(file, file + ".txt")
    }
