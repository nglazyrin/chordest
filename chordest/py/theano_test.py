# -*- coding: utf-8 -*-
"""
Created on Tue Jan 08 23:10:24 2013

@author: Nikolay
"""

import numpy
import theano
import theano.tensor as T

a, b, c, W = T.vector(), T.vector(), T.vector(), T.matrix()
x = T.dot(a, W) + b
#c = T.sqr(T.sub(x, a))
c = T.mean(T.sqrt(T.sum(T.sqr(T.sub(a, b)))))
# -T.mean(T.sqrt(T.sum(T.sqr(T.sub(self.p_y_given_x, y)))))
reg = T.sum(W, axis=0) / T.shape(W)[0] # sum over training set
rho = T.constant(0.05)
#reg1 = (1-rho) / (1-reg)
reg1 = rho * T.log(rho / reg) + (1-rho) * T.log((1-rho) / (1-reg))
reg2 = T.sum(reg1)
s = T.sum(W, axis=0)

calc = theano.function(
    inputs=[a,b],
    outputs=[c])

sss = theano.function(
    inputs=[W],
    outputs=[reg, reg1, reg2])

#print calc([1, 0, 0.4, 0, 0, 0.7], [1.2, -0.4, 0.3, -0.2, 0.1, 0.5])
print sss([[0.1, 0.2, 0.3, 0.4], [0.4, 0.5, 0.6, 0.7], [0.7, 0.8, 0.9, 1.0]])

x = T.matrix('x')
lr = T.scalar('lr')

s = T.nnet.sigmoid(x)
l = T.log(1 - s)
c = l.mean()
ux = x - lr * theano.grad(c, x)

# Before the optimization, inf and NaN will be produced in the graph,
# and DebugMode will complain. Everything is fine afterwards.
f = theano.function([x, lr], ux)
ux_v = f([[50]], 0.1)
print numpy.isnan(ux_v)