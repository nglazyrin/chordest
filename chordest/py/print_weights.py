# -*- coding: utf-8 -*-
"""
Created on Sat Apr 06 09:53:49 2013

@author: Nikolay
"""

import cPickle
import theano
import matplotlib.pyplot as p

with open('model/SdA12.dat', 'rb') as f:
    (da, logl, sigmoid) = cPickle.load(f)
s = da[0].W[3]
#s = sigmoid.W[2]
f = theano.function([], s)
y = f()
x = range(len(y))
p.plot(x,y)
p.show()