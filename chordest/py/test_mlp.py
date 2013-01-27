# -*- coding: utf-8 -*-
"""
Created on Wed Jan 09 21:53:56 2013

@author: Nikolay
"""

import csv

from chord_utils import list_spectrum_data, through_mlp

with open('data/encoded_short.csv', 'rb') as i:
    reader = csv.reader(i)
    (before, chords) = list_spectrum_data(reader)

with open('data/tonnetz_short.csv', 'wb') as o:
    writer = csv.writer(o)
    writer.writerows(through_mlp(before))
