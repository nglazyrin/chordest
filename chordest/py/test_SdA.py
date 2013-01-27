# -*- coding: utf-8 -*-
"""
Created on Thu Jan 17 12:29:41 2013

@author: Nikolay
"""

import csv
import os
import cPickle

from chord_utils import list_spectrum_data, restore_sda, through_sda

with open('dA_spectrum/SdA.dat', 'rb') as f:
    (dA_layers, sigmoid_layers, log_layer) = cPickle.load(f)
sda = restore_sda(dA_layers, sigmoid_layers, log_layer)

def process_file(source, target):
    with open(source, 'rb') as i:
        reader = csv.reader(i)
        (before, chords) = list_spectrum_data(reader, components=48, allow_no_chord=True)
    result = through_sda(sda, before)
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
