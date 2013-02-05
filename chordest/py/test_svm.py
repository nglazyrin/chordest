# -*- coding: utf-8 -*-
"""
Created on Mon Feb 04 22:52:22 2013

@author: Nikolay
"""

import csv
import os

from libsvm import svmutil
from prepare_libsvm import load_sda
from chord_utils import list_spectrum_data, through_sda_layers


model_file = 'model/svm.dat'

sda = load_sda()
model = svmutil.svm_load_model(model_file)
chord_list = ['A#-C#-F', 'A#-C#-F#', 'A#-D#-F#', 'A#-D#-G',
              'A#-D-F', 'A#-D-G', 'A-C#-E', 'A-C#-F#', 'A-C-E',
              'A-C-F', 'A-D-F', 'A-D-F#', 'B-D#-F#', 'B-D#-G#',
              'B-D-F#', 'B-D-G', 'B-E-G', 'B-E-G#', 'C#-E-G#',
              'C#-F-G#', 'C-D#-G', 'C-D#-G#', 'C-E-G', 'C-F-G#', 'N']

def process_file(source, target):
    with open(source, 'rb') as i:
        reader = csv.reader(i)
        (before, chords) = list_spectrum_data(reader, components=60, allow_no_chord=True)
    sda_features = through_sda_layers(sda, before)
    # append chord labels as integer numbers
    sda_features = [x.tolist() for x in sda_features]
    (labels, acc, vals) = svmutil.svm_predict([0] * len(sda_features), sda_features, model)
    result = [chord_list[int(x)] for x in labels]
    with open(target, 'wb') as o:
        o.write(','.join(result))

input_dir = os.path.abspath("E:\\Dev\\git\\my_repository\\chordest\\csv\\test")
output_dir = os.path.abspath("E:\\Dev\\git\\my_repository\\chordest\\csv\\svm")

for dirname, dirnames, filenames in os.walk(input_dir):
    for filename in filenames:
        if filename.endswith('.csv'):
            source = os.path.join(dirname, filename)
            target = os.path.join(output_dir, filename)
            process_file(source, target)

print('Done')
