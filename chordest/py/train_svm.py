# -*- coding: utf-8 -*-
"""
Created on Sun Feb 03 23:11:51 2013

@author: Nikolay
"""

import csv

from chord_utils import list_spectrum_data, shuffle_2
from libsvm import svmutil

limit = 3000
model_file = 'model/svm.dat'
source_file = 'E:/Dev/git/my_repository/chordest/result/train_svm_c.csv'

def load_data():
    with open(source_file, 'rb') as i:
        reader = csv.reader(i)
        (data, chords) = list_spectrum_data(reader, components=42, allow_no_chord=True)
    chords = [int(x) for x in chords]
    new_data = []
    new_chords = []
    for i,row in enumerate(data):
        row.append(chords[i])
    for i in range(25):
        (d, c) = filter_chord(data, i)
        new_data = new_data + d
        new_chords = new_chords + c
    return shuffle_2(new_data, new_chords)

def filter_chord(data, chord):
    data1 = filter(lambda x: x[-1] == chord, data)
    r_data = []
    r_chords = []
    for x in data1:
        r_data.append(x[:-1])
        r_chords.append(x[-1])
        if (len(r_data) == limit):
            break
    return (r_data, r_chords)

def train():
    (data, chords) = load_data()
    print len(data)
    model = svmutil.svm_train(chords, data)
    svmutil.svm_save_model(model_file, model)

if __name__ == '__main__':
    train()
