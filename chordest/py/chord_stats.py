# -*- coding: utf-8 -*-
"""
Created on Tue Jan 01 23:57:36 2013

@author: Nikolay
"""

import os, glob, operator

chords = {}

def scandirs(path):
    for currentFile in glob.glob( os.path.join(path, '*') ):
        if os.path.isdir(currentFile):
            scandirs(currentFile)
        elif currentFile.endswith('.lab'):
            process_lab(currentFile)

def process_lab(lab):
    with open(lab) as f:
        content = f.readlines()
    for line in content:
        (start, end, chord) = line.split()
        if (chord in chords):
            before = chords[chord]
        else:
            before = 0
        chords[chord] = before + float(end) - float(start)

scandirs('E:\Dev\git\my_repository\chordest\lab')
s_chords = sorted(chords.iteritems(), key=operator.itemgetter(1))
for k,v in s_chords:
    print '%s: %.2f' % (k, v)
# print(s_chords)