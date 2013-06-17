# -*- coding: utf-8 -*-
"""
Created on Wed Feb 20 00:30:12 2013

@author: Nikolay
"""

import csv
import cPickle
import theano
import numpy

from Test.base_test import TestIterator
from test_SdA import restore_sda
from Utils import util

class test_SdA_circ(TestIterator):
    def load_model(self):
        with open('model/SdA12.dat', 'rb') as f:
            (dA_layers, sigmoid_layers, log_layer) = cPickle.load(f)
        return restore_sda(dA_layers, sigmoid_layers, log_layer)
    
    def process_file(self, source, target):
        ins = 72
        subnotes = 1
        with open(source, 'rb') as i:
            reader = csv.reader(i)
            (before, chords) = util.list_spectrum_data(reader, components=ins, allow_no_chord=True)
        result = None
        for offset in range(12):
            data = self.getFirstColumns(before, offset * subnotes, ins-12 * subnotes)
            temp = self.through_sda(self.model, data)
            temp = numpy.roll(temp, offset, axis=1)
            if result == None:
                result = numpy.zeros(temp.shape)
            result = numpy.add(result, temp)
#        result = self.through_sda(self.model, before)
        with open(target, 'wb') as o:
            writer = csv.writer(o)
            writer.writerows(result)
    
    def through_sda(self, sda, data):
        m = theano.shared(numpy.asmatrix(data, dtype=theano.config.floatX),
                                         borrow=True)
        return sda.get_result(m)
    
    def rotateLeft(self, data, n):
        return [x.tolist()[n:] + x.tolist()[:n] for x in data]

def main():
    input_dir = "E:\\Dev\\git\\my_repository\\chordest\\csv\\test"
    output_dir = "E:\\Dev\\git\\my_repository\\chordest\\csv\\encoded"
    it = test_SdA_circ(input_dir, output_dir)
    it.iterate()

if __name__ == '__main__':
    main()
