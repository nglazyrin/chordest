import os
import re
import sys

if len(sys.argv) < 4:
    sys.exit('Usage: python list.py /path/to/dir file_mask_regexp result_file.txt')

all = open(sys.argv[3], 'w')

for dirname, dirnames, filenames in os.walk(os.path.abspath(sys.argv[1])):
    for filename in filenames:
#        if filename.endswith(sys.argv[2]):
         if re.search(sys.argv[2], filename):
            all.write(os.path.join(dirname, filename) + '\r')
all.close()

