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
with open('model/sda_layers.dat', 'rb') as f:
    (da0, sigmoid0) = cPickle.load(f)
idx0 = 0
idx1 = 10
s = da[idx0].W[idx1]
#s0 = da0[idx0].W[idx1]
s0 = sigmoid.W[15]
f = theano.function([], s)
f0 = theano.function([], s0)
y = f()
y0 = f0()
x = range(len(y))
x0 = range(len(y0))
p.subplots = 2
p.plot(x,y)
p.plot(x0,y0)
p.legend(["autoencoder", "final"])
p.show()