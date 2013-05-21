# -*- coding: utf-8 -*-
"""
Trains SdA for bass note recognition with 1 out per note, 13 outs in total

Created on Wed Feb 13 23:52:25 2013

@author: Nikolay
"""

import theano.tensor as T
from base_train_SdA import SdATrainer
from Utils.util import to_chroma, asarray

class SdATrainerBass(SdATrainer):
    def prepare_chords(self, chords): # notes, not chords
        return map(lambda x: to_chroma(x), chords)

    def chords_to_array(self, chords): # notes, not chords
        return asarray(chords)
    
def main():
    tr = SdATrainerBass(12, 'model/SdAbass.dat', T.tanh, False,
               train_file='E:/Dev/git/my_repository/chordest/result/train_dA_bass_c.csv',
               layers_file='model/sda_layers_bass.dat')
    tr.train_SdA()

if __name__ == '__main__':
    main()
