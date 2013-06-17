# -*- coding: utf-8 -*-
"""
Created on Tue Jan 08 18:32:54 2013

@author: Nikolay
"""

import math
import numpy
import cPickle
import theano

from random import shuffle

chord_list = ['A#-C#-F',    # F#
              'A#-C#-F#',   # A#:min
              'A#-D#-F#',   # D#:min
              'A#-D#-G',    # D#
              'A#-D-F',     # A#
              'A#-D-G',     # G:min
              'A-C#-E',     # A
              'A-C#-F#',    # F#:min
              'A-C-E',      # A:min
              'A-C-F',      # F
              'A-D-F',      # D:min
              'A-D-F#',     # D
              'B-D#-F#',    # B
              'B-D#-G#',    # G#:min
              'B-D-F#',     # B:min
              'B-D-G',      # G
              'B-E-G',      # E:min
              'B-E-G#',     # E
              'C#-E-G#',    # C#:min
              'C#-F-G#',    # C#
              'C-D#-G',     # C:min
              'C-D#-G#',    # G#
              'C-E-G',      # C
              'C-F-G#',     # F:min
              'N']
          
notes = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B', 'N']

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

def as_int_array(array):
    return theano.shared(numpy.asarray(array, dtype='int32'),
                                 borrow=True)

def asmatrix(array):
    return theano.shared(numpy.asmatrix(array, dtype=theano.config.floatX),
                                 borrow=True)

def to_note_list(chord):
    return chord.split('-')

def to_note_numbers(note_list):
    if ('N' == note_list[0]):
        return []
    return map(lambda x: notes.index(x), note_list)
    
def to_chroma_template(note_numbers):
    return [int(x in note_numbers) for x in range(12)]

def to_chroma(chord):
    if (chord):
        return to_chroma_template(to_note_numbers(to_note_list(chord)))
    return None

def to_chord_index(chord):
    result = [0] * len(chord_list)
    result[chord_list.index(chord)] = 1
    return result

def to_chord_indexes(chords):
    return map(lambda x: chord_list.index(x), chords)

def to_note_indexes(notes_list):
    return [notes.index(x) for x in notes_list]
    
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

def list_spectrum_data(reader, components=200, allow_no_chord=False, allow_non_majmin=True):
    array = []
    chords = []
    for row in reader:
        if (len(row) != components + 1):
            raise OverflowError()
        chord = row[-1]
        if ((chord or allow_no_chord) and (allow_non_majmin or chord in chord_list)):
            r = [ float(x) for x in row[:-1] ]
            array.append(r)
            chords.append(row[-1])
    return (array, chords)

def list_spectrum_only(reader, components=200):
    array = []
    for row in reader:
        if (len(row) != components):
            raise OverflowError()
        r = [ float(x) for x in row ]
        array.append(r)
    return array

def through_da(data):
    with open('model/corruption_30.dat', 'rb') as d:
        da = cPickle.load(d)
    return da.encode_m(data)

def through_sda_layers(sda, data):
    m = theano.shared(numpy.asmatrix(data, dtype=theano.config.floatX),
                                     borrow=True)
    return sda.get_sda_features(m)

def shuffle_2(list1, list2):
    list1_shuf = []
    list2_shuf = []
    index_shuf = range(len(list1))
    shuffle(index_shuf)
    for i in index_shuf:
        list1_shuf.append(list1[i])
        list2_shuf.append(list2[i])
    return (list1_shuf, list2_shuf)

def get_root(chord):
    root = None
    for x in notes:
        if chord.startswith(x): root = x
    return root
        

#print notes.index(get_root("A:min"))