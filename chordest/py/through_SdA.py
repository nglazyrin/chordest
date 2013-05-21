# -*- coding: utf-8 -*-
"""
Created on Mon Apr 01 23:36:36 2013

@author: Nikolay
"""

import csv
import cPickle
import theano
import numpy

from Test.base_test import TestIterator
from test_SdA import restore_sda
from Utils import util

class Through_SdA(TestIterator):
        
    def __init__(self):
        self.model = self.load_model()

    def load_model(self):
        with open('model/SdA12.dat', 'rb') as f:
            (dA_layers, sigmoid_layers, log_layer) = cPickle.load(f)
        return restore_sda(dA_layers, sigmoid_layers, log_layer)
    
    def process_file(self, source, target):
        with open(source, 'rb') as i:
            reader = csv.reader(i)
            (before, chords) = util.list_spectrum_data(reader, components=72, allow_no_chord=True)
        result = None
        for offset in range(12):
            data = self.get60columns(before, offset)
            temp = self.through_sda(self.model, data)
            temp = numpy.roll(temp, offset, axis=1)
            if result == None:
                result = numpy.zeros(temp.shape)
            result = numpy.add(result, temp)
#        result = self.through_sda(self.model, before)
        towrite = []
        for (index, row) in enumerate(result):
            chord = chords[index]
            root = util.get_root(chord)
            row = numpy.roll(row, 12-util.notes.index(root))
            row = row / numpy.linalg.norm(row, ord=numpy.inf)
            l = row.tolist()
            if (chords[index]):
                l.append(self.nmajmin(chord))
                towrite.append(l)
        with open(target, 'wb') as o:
            writer = csv.writer(o)
            writer.writerows(towrite)
    
    def nmajmin(self, chord):
        if (chord.startswith('N')):
            return 'N'
        elif (chord.endswith('min')):
            return 'min'
        else:
            return 'maj'
    
    def through_sda(self, sda, data):
        m = theano.shared(numpy.asmatrix(data, dtype=theano.config.floatX),
                                         borrow=True)
        return sda.get_result(m)
    
    def get60columns(self, data, offset):
        return [x[offset:offset+60] for x in data]

def main():
    input_chord = "E:\\Dev\\git\\my_repository\\chordest\\result\\train_hmm_chord.csv"
    output_chord = "E:\\Dev\\git\\my_repository\\chordest\\result\\train_hmm_chord_sda.csv"
#    input_seq = "E:\\Dev\\git\\my_repository\\chordest\\result\\train_hmm_seq.csv"
#    output_seq = "E:\\Dev\\git\\my_repository\\chordest\\result\\train_hmm_seq_sda.csv"
    ts = Through_SdA()
    ts.process_file(input_chord, output_chord)
    print('Done with ' + output_chord)
#    ts.process_file(input_seq, output_seq)
#    print('Done with ' + output_seq)

if __name__ == '__main__':
    main()
