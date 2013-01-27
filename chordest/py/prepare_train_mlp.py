# -*- coding: utf-8 -*-
"""
Created on Tue Jan 08 13:39:44 2013

@author: Nikolay
"""

import csv
import cPickle
from chord_utils import list_spectrum_data

source = 'data/train_dA.csv'
target = 'data/train_mlp.csv'
#source = 'data/track_short.csv'
#target = 'data/encoded_short.csv'

with open('dA_spectrum/corruption_30.dat', 'rb') as d:
    da = cPickle.load(d)
with open(source, 'rb') as i:
    reader = csv.reader(i)
    (before, chords) = list_spectrum_data(reader, 240)
    
encoded = da.encode_m(before)
result = []

for (index, row) in enumerate(encoded):
    l = row.tolist()
    if (chords[index]):
        l.append(chords[index])
        result.append(l)

with open(target, 'wb') as o:
    writer = csv.writer(o)
    writer.writerows(result)