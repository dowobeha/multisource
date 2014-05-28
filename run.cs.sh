#!/bin/bash

#$ -S /bin/bash
#$ -l mem_free=24g
#$ -V
#$ -cwd
#$ -j y
#$ -o /home/lane/joshua.workspace/multisource.git/log.ces-eng

java -Djava.util.logging.config.file=/home/lane/joshua.workspace/multisource.git/logging.console.properties -cp bin:../joshua.git/bin:lib/guava-11.0.1.jar:lib/iso639.jar -Xms24g -Xmx24g joshua.multilingual.MultiMain ces

