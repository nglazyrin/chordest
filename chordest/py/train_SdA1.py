# -*- coding: utf-8 -*-
"""
Trains SdA with 1 out per chord, 25 outs in total

Created on Wed Feb 13 23:52:25 2013

@author: Nikolay
"""

import theano.tensor as T
from base_train_SdA import SdATrainer
from Utils.util import to_chord_indexes, as_int_array

class SdATrainer1PerChord(SdATrainer):
    def prepare_chords(self, chords):
        return to_chord_indexes(chords)

    def chords_to_array(self, chords):
        return as_int_array(chords)
    
def main():
    tr = SdATrainer1PerChord(25, 'model/SdA1.dat', T.nnet.softmax, True)
    tr.train_SdA()

if __name__ == '__main__':
    main()
