# -*- coding: utf-8 -*-
"""
Created on Tue Jan 08 18:32:54 2013

@author: Nikolay
"""

import math
import numpy
import cPickle
import theano
import theano.tensor as T

from random import shuffle
from mlp_modified import MLP2
from SdA_modified import SdA

class Phi(object):
    def __init__(self):
        self.R = [1, 1, 0.5]
        self.PHI = [
            map(lambda x: self.R[0] * math.sin(x * 7 * math.pi / 6), range(12)),
            map(lambda x: self.R[0] * math.cos(x * 7 * math.pi / 6), range(12)),
            map(lambda x: self.R[1] * math.sin(x * 3 * math.pi / 2), range(12)),
            map(lambda x: self.R[1] * math.cos(x * 3 * math.pi / 2), range(12)),
            map(lambda x: self.R[2] * math.sin(x * 2 * math.pi / 3), range(12)),
            map(lambda x: self.R[2] * math.cos(x * 2 * math.pi / 3), range(12))
        ]
    
    def prod(self, chroma):
        if (not chroma):
            return None
        phi = numpy.matrix(self.PHI)
        c = numpy.array(chroma)
        p = numpy.dot(phi, c)
        p = numpy.squeeze(numpy.asarray(p)) # 1x6 matrix to 1-dim array
        n = numpy.linalg.norm(p, 2)
        if (n <= 0): n = 1 # in case of vector consisting of 0s
        p = map(lambda x: x / n, p)
        return p

def asarray(array):
    return theano.shared(numpy.asarray(array, dtype=theano.config.floatX),
                                 borrow=True)

def asmatrix(array):
    return theano.shared(numpy.asmatrix(array, dtype=theano.config.floatX),
                                 borrow=True)

def to_note_list(chord):
    return chord.split('-')

def to_note_numbers(note_list):
    if ('N' == note_list[0]):
        return []
    notes = ('C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B')
    return map(lambda x: notes.index(x), note_list)
    
def to_chroma_template(note_numbers):
    return [int(x in note_numbers) for x in range(12)]

def to_chroma(chord):
    if (chord):
        return to_chroma_template(to_note_numbers(to_note_list(chord)))
    return None

def to_tonnetz(chords):
    phi = Phi()
    return map(lambda x: phi.prod(to_chroma(x)), chords)

def to_uniform(row, l):
    return [math.exp(-float(x) * l[i]) for i,x in enumerate(row)]

def to_tile_array(array, height, width):
    half = int(math.floor(width / 2))
    temp_len = len(array) + 2 * half
    temp = [[0 for col in range(height)] for row in range(temp_len)]
    for i in range(len(array)):
        temp[i + half] = array[i]
    return [to_tile(temp[i:i+2*half+1], height*width) for i in range(len(array))]

def to_tile(array, size):
    result = numpy.asarray(array, dtype=theano.config.floatX).reshape(-1)
    if (len(result.tolist()) < size):
        return [0] * size
    return result.tolist()

def list_spectrum_data(reader, components=200, allow_no_chord=False):
    spectral_components = components
    array = []
    chords = []
    for row in reader:
        if (len(row) != spectral_components + 1):
            raise OverflowError()
        chord = row[-1]
        if (chord or allow_no_chord):
            r = [ float(x) for x in row[:-1] ]
            array.append(r)
            chords.append(row[-1])
    return (array, chords)

def through_cnn(cnn, data):
    m = theano.shared(numpy.asmatrix(data, dtype=theano.config.floatX),
                                     borrow=True)
    return cnn.get_result(m)

def through_da(data):
    with open('model/corruption_30.dat', 'rb') as d:
        da = cPickle.load(d)
    return da.encode_m(data)

def through_mlp(data, l=None):
    with open('model/mlp.dat', 'rb') as f:
        (hidden, out) = cPickle.load(f)
    if (l):
        data = map(lambda x: to_uniform(x, l), data)
    m = theano.shared(numpy.asmatrix(data, dtype=theano.config.floatX),
                                     borrow=True)
    rng = numpy.random.RandomState(1234)
    x = T.matrix('x')
    mlp = MLP2(rng=rng, input=x, hidden=hidden, out=out)
    t = mlp.get_result(x)
    f = theano.function([], t, givens={x: m})
    return f()

def restore_sda(dA_layers, sigmoid_layers, log_layer):
    layers = (dA_layers, sigmoid_layers)
    hidden_layers_sizes = map(lambda x: x.n_hidden, dA_layers)
    n_ins = dA_layers[0].n_visible
    rng = numpy.random.RandomState(1234)
    sda = SdA(numpy_rng=rng, n_ins=n_ins, n_outs=6,
              hidden_layers_sizes=hidden_layers_sizes,
              layers=layers, log_layer=log_layer)
    return sda

def through_sda(sda, data):
    m = theano.shared(numpy.asmatrix(data, dtype=theano.config.floatX),
                                     borrow=True)
    return sda.get_result(m)

def through_sda_layers(sda, data):
    m = theano.shared(numpy.asmatrix(data, dtype=theano.config.floatX),
                                     borrow=True)
    return sda.get_sda_features(m)

def through(data, l=None):
    features = through_da(data)
    return through_mlp(features, l)

def shuffle_2(list1, list2):
    list1_shuf = []
    list2_shuf = []
    index_shuf = range(len(list1))
    shuffle(index_shuf)
    for i in index_shuf:
        list1_shuf.append(list1[i])
        list2_shuf.append(list2[i])
    return (list1_shuf, list2_shuf)

#chords = ('B-E-G#', 'A-C#-E', 'N', None)
#print to_tonnetz(chords)
