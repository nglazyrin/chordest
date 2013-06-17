# -*- coding: utf-8 -*-
"""
Created on Thu Jan 17 12:29:41 2013

@author: Nikolay
"""

import csv
import cPickle
import theano
import numpy

from DL.SdA_modified import SdA
from Test.base_test import TestIterator
from Utils import util

class test_SdA(TestIterator):
    def load_model(self):
        with open('model/SdA12.dat', 'rb') as f:
            (dA_layers, sigmoid_layers, log_layer) = cPickle.load(f)
        return restore_sda(dA_layers, sigmoid_layers, log_layer)
    
    def process_file(self, source, target):
        ins = 240
        with open(source, 'rb') as i:
            reader = csv.reader(i)
            (before, chords) = util.list_spectrum_data(reader, components=ins + 60, allow_no_chord=True)
        before = self.getFirstColumns(before, 0, ins)
        result = self.through_sda(self.model, before)
        with open(target, 'wb') as o:
            writer = csv.writer(o)
            writer.writerows(result)
    
    def through_sda(self, sda, data):
        m = theano.shared(numpy.asmatrix(data, dtype=theano.config.floatX),
                                         borrow=True)
        return sda.get_result(m)

def restore_sda(dA_layers, sigmoid_layers, log_layer, is_vector_y=False):
    layers = (dA_layers, sigmoid_layers)
    hidden_layers_sizes = map(lambda x: x.n_hidden, dA_layers)
    n_ins = dA_layers[0].n_visible
    n_outs = log_layer.W.shape[1]
    sda = SdA(n_ins=n_ins, n_outs=n_outs,
              hidden_layers_sizes=hidden_layers_sizes,
              layers=layers, log_layer=log_layer, is_vector_y=is_vector_y,
              log_activation=theano.tensor.tanh)
    return sda

def main():
    input_dir = "E:\\Dev\\git\\my_repository\\chordest\\csv\\test"
    output_dir = "E:\\Dev\\git\\my_repository\\chordest\\csv\\encoded"
    it = test_SdA(input_dir, output_dir)
    it.iterate()

if __name__ == '__main__':
    main()
