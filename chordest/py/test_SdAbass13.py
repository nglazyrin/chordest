# -*- coding: utf-8 -*-
"""
Created on Sun Mar 03 00:50:57 2013

@author: Nikolay
"""


import csv
import cPickle
import theano
import numpy

from collections import defaultdict
from Test.base_test import TestIterator
from test_SdA import restore_sda
from Utils import util

class test_SdA1(TestIterator):
    def load_model(self):
        with open('model/SdAbass.dat', 'rb') as f:
            (dA_layers, sigmoid_layers, log_layer) = cPickle.load(f)
        return restore_sda(dA_layers, sigmoid_layers, log_layer, is_vector_y=True)
    
    def process_file(self, source, target):
        with open(source, 'rb') as i:
            reader = csv.reader(i)
            (before, chords) = util.list_spectrum_data(reader, components=72, allow_no_chord=True)
        
#        before = self.get60columns(before, 0)
#        result = self.through_sda_bass(self.model, before)
#        toWrite = result
        
        result = None
        for offset in range(12):
            data = self.get60columns(before, offset)
            temp = self.through_sda_bass(self.model, data)
            if (result == None):
                result = []
                for i in range(len(temp)):
                    result.append([])
            temp = [self.roll1(x, offset) for x in temp]
            for i in range(len(temp)):
                result[i].append(util.notes[temp[i]])
        toWrite = [12] * len(result)
        for i in range(len(result)):
            d = defaultdict(int)
            for x in result[i]:
                d[x] += 1
            key = self.keywithmaxval(d)
            toWrite[i] = key
        
        with open(target, 'wb') as o:
            o.write(','.join(toWrite))

    def through_sda_bass(self, sda, data):
        m = theano.shared(numpy.asmatrix(data, dtype=theano.config.floatX),
                                         borrow=True)
        indexes = sda.get_pred(m)
        return indexes
#        return [util.notes[x] for x in indexes]
    
    def get60columns(self, data, offset):
        return [x[offset:offset+60] for x in data]
    
    def roll1(self, x, offset):
        if (12 == x):
            return x
        else:
            return (x + 12 - offset) % 12
    
    def keywithmaxval(self, d):
         """ a) create a list of the dict's keys and values; 
             b) return the key with the max value"""  
         v=list(d.values())
         k=list(d.keys())
         return k[v.index(max(v))]
    
def main():
    input_dir = "E:\\Dev\\git\\my_repository\\chordest\\csv\\test"
    output_dir = "E:\\Dev\\git\\my_repository\\chordest\\csv\\bass"
    it = test_SdA1(input_dir, output_dir)
    it.iterate()

if __name__ == '__main__':
    main()

