import random
import sys
import argparse

parser = argparse.ArgumentParser(description='Splits a list of files into a number of randomized partitions')
parser.add_argument('-l', dest='all', required=True, help='file containing the list of file names')
parser.add_argument('partitions', metavar='N', type=int, help='number of partitions')

args = parser.parse_args()

with open(args.all) as f:
	all = f.readlines()
all = map(str.strip, all)
random.shuffle(all)

p = args.partitions
index = args.all.rindex('.')
name_root = args.all[:index]
ext = args.all[index:]
for i in range(p):
	s = len(all) * i / p;
	f = len(all) * (i + 1) / p;
	test = all[s:f]
	train = all[:s] + all[f:]
	name_train = name_root + str(i) + 'train' + ext
	name_test = name_root + str(i) + 'test' + ext
	with open(name_train, 'w') as f:
		f.write('\n'.join(train))
		f.close()
	with open(name_test, 'w') as f:
		f.write('\n'.join(test))
		f.close()
