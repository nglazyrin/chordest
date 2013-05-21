# -*- coding: utf-8 -*-
"""
Created on Tue Dec 18 23:08:17 2012

@author: Nikolay
"""

import csv
import cPickle
import matplotlib.pyplot as plt

from util import list_spectrum_data

def draw_data():
    with open('data/train_dA.csv', 'rb') as f:
        reader = csv.reader(f)
        (spectrum, chords) = list_spectrum_data(reader, components=240)
    vector = spectrum[3400];
    with open('model/corruption_30.dat', 'rb') as f1:
    # with open('model/no_corruption.dat', 'rb') as f1:
        da = cPickle.load(f1)
        encoded = da.autoencode(vector)
    
    ax = plt.subplot(111)
    xs = range(0,240)
    ys = vector
    ys1 = encoded
    ax.plot(xs, ys)
    ax.plot(xs, ys1)
    plt.show()


if __name__ == '__main__':
    draw_data()
