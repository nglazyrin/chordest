# -*- coding: utf-8 -*-
"""
Created on Thu Jan 10 00:49:38 2013

@author: Nikolay
"""

import csv
import os

from chord_utils import list_spectrum_data, through

def process_file(source, target, l=None):
    with open(source, 'rb') as i:
        reader = csv.reader(i)
        (before, chords) = list_spectrum_data(reader, components=240, allow_no_chord=True)
    result = through(before, l)
    with open(target, 'wb') as o:
        writer = csv.writer(o)
        writer.writerows(result)

input_dir = os.path.abspath("E:\\Dev\\git\\my_repository\\chordest\\csv\\test")
output_dir = os.path.abspath("E:\\Dev\\git\\my_repository\\chordest\\csv\\encoded")

with open('data/lambdas.csv', 'rb') as fl:
    reader = csv.reader(fl)
    l = reader.next()
l = map(float, l)

for dirname, dirnames, filenames in os.walk(input_dir):
    for filename in filenames:
        if filename.endswith('.csv'):
            source = os.path.join(dirname, filename)
            target = os.path.join(output_dir, filename)
            process_file(source, target)

print('Done')