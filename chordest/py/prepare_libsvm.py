# -*- coding: utf-8 -*-
"""
Created on Sun Feb 03 20:41:45 2013

@author: Nikolay
"""

import os
import csv
import cPickle
import numpy

from SdA_modified import SdA
from chord_utils import list_spectrum_data, through_sda_layers

source_file1 = 'E:/Dev/git/my_repository/chordest/result/train_dA.csv'
source_file = 'E:/Dev/git/my_repository/chordest/result/train_dA_c.csv'
target_file = 'E:/Dev/git/my_repository/chordest/result/train_svm_c.csv'
layers_file = 'model/sda_layers_p.dat'
outs = 6 # ignored when generating data for libsvm

def load_sda():
    layers = load_layers()
    layers_sizes = map(lambda x: x.n_hidden, layers[0])
    ins = layers[0][0].n_visible
    rng = numpy.random.RandomState(1234)
    return SdA(numpy_rng=rng, n_ins=ins, layers=layers,
                  hidden_layers_sizes=layers_sizes, n_outs=outs)

def load_layers():
    da = []
    sigmoid = []
    if (not os.path.isfile(layers_file)):
        return None
    with open(layers_file, 'rb') as f:
        (da, sigmoid) = cPickle.load(f)
    return (da, sigmoid)
#    return None

def process_file():
    sda = load_sda()
    with open(source_file, 'rb') as i:
        reader = csv.reader(i)
        (before, chords) = list_spectrum_data(reader, components=60, allow_no_chord=True)
    chord_list = sorted(set(chords))
    result = through_sda_layers(sda, before)
    # append chord labels as integer numbers
    result = [x.tolist() for x in result]
    for i,x in enumerate(result):
        x.append(chord_list.index(chords[i]))
    with open(target_file, 'wb') as o:
        writer = csv.writer(o)
        writer.writerows(result)

def print_chords():
    with open(source_file, 'rb') as i:
        reader = csv.reader(i)
        (before, chords) = list_spectrum_data(reader, components=60, allow_no_chord=True)
    print sorted(set(chords))

if __name__ == '__main__':
    print_chords()
