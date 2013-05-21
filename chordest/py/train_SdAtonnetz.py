# -*- coding: utf-8 -*-
"""
Trains SdA that outputs Tonnetz vectors

Created on Wed Jan 16 22:44:28 2013

@author: Nikolay
"""

import theano.tensor as T
from base_train_SdA import SdATrainer
from Utils.util import to_tonnetz, asarray

class SdATrainerTonnetz(SdATrainer):
    def prepare_chords(self, chords):
        return to_tonnetz(chords)

    def chords_to_array(self, chords):
        return asarray(chords)
    
def main():
    tr = SdATrainerTonnetz(6, 'model/SdA.dat', T.tanh, False)
    tr.train_SdA()

if __name__ == '__main__':
    main()
