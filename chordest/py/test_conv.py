# -*- coding: utf-8 -*-
"""
Created on Mon Jan 28 10:13:52 2013

@author: Nikolay
"""

import csv
import os
import cPickle

from chord_utils import list_spectrum_data, through_cnn, to_tile_array
from conv_modified import CNN

with open('dA_spectrum/conv.dat', 'rb') as f:
    layers = cPickle.load(f)
ins = 48
width = 7

def process_file(source, target):
    with open(source, 'rb') as i:
        reader = csv.reader(i)
        (before, chords) = list_spectrum_data(reader, components=48, allow_no_chord=True)
    before = to_tile_array(before, ins, width)
    cnn = CNN(len(before), layers)
    result = through_cnn(cnn, before)
    with open(target, 'wb') as o:
        writer = csv.writer(o)
        writer.writerows(result)

input_dir = os.path.abspath("E:\\Dev\\git\\my_repository\\chordest\\csv\\test")
output_dir = os.path.abspath("E:\\Dev\\git\\my_repository\\chordest\\csv\\encoded")

for dirname, dirnames, filenames in os.walk(input_dir):
    for filename in filenames:
        if filename.endswith('.csv'):
            source = os.path.join(dirname, filename)
            target = os.path.join(output_dir, filename)
            process_file(source, target)

print('Done')
