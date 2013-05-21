# -*- coding: utf-8 -*-
"""
Created on Thu Feb 14 12:39:43 2013

@author: Nikolay
"""


import csv
import cPickle
import theano
import numpy

from Test.base_test import TestIterator
from test_SdA import restore_sda
from Utils import util

class test_SdA1(TestIterator):
    def load_model(self):
        with open('model/SdA1.dat', 'rb') as f:
            (dA_layers, sigmoid_layers, log_layer) = cPickle.load(f)
        return restore_sda(dA_layers, sigmoid_layers, log_layer, n_outs=25, is_vector_y=True)
    
    def process_file(self, source, target):
        with open(source, 'rb') as i:
            reader = csv.reader(i)
            (before, chords) = util.list_spectrum_data(reader, components=60, allow_no_chord=True)
        result = self.through_sda1(self.model, before)
        with open(target, 'wb') as o:
            o.write(','.join(result))

    def through_sda1(sda, data):
        m = theano.shared(numpy.asmatrix(data, dtype=theano.config.floatX),
                                         borrow=True)
        indexes = sda.get_pred(m)
        return [util.chord_list[x] for x in indexes]

def main():
    input_dir = "E:\\Dev\\git\\my_repository\\chordest\\csv\\test"
    output_dir = "E:\\Dev\\git\\my_repository\\chordest\\csv\\svm"
    it = test_SdA1(input_dir, output_dir)
    it.iterate()

if __name__ == '__main__':
    main()

