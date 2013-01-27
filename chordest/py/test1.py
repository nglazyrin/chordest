# -*- coding: utf-8 -*-
"""
Created on Wed Dec 19 23:24:52 2012

@author: Nikolay
"""
import csv
import cPickle

from chord_utils import list_spectrum_data, through_da

def autoencode_file(source, target):
    with open(source, 'rb') as i:
        reader = csv.reader(i)
        (before, chords) = list_spectrum_data(reader, components=240, allow_no_chord=True)
    result = through_da(before)
    with open(target, 'wb') as o:
        writer = csv.writer(o)
        writer.writerows(result)

with open('dA_spectrum/corruption_30.dat', 'rb') as d:
    da = cPickle.load(d)
    autoencode_file('data/track_short.csv', 'data/encoded_short.csv')
