# -*- coding: utf-8 -*-
"""
Trains SdA that outputs chroma vectors

Created on Sat Feb 16 01:27:16 2013

@author: Nikolay
"""

import theano.tensor as T
from base_train_SdA import SdATrainer
from Utils.util import to_chroma, asarray

class SdATrainerChroma(SdATrainer):
    def prepare_chords(self, chords):
        return map(lambda x: to_chroma(x), chords)

    def chords_to_array(self, chords):
        return asarray(chords)
    
def main():
    tr = SdATrainerChroma(12, 'model/SdA12.dat', T.tanh, False)
    tr.train_SdA()

if __name__ == '__main__':
    main()
