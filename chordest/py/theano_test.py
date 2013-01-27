# -*- coding: utf-8 -*-
"""
Created on Tue Jan 08 23:10:24 2013

@author: Nikolay
"""

import theano
import theano.tensor as T

a, b, c = T.vector(), T.vector(), T.vector()
W = T.matrix()
x = T.dot(a, W) + b
c = T.sqr(T.sub(x, a))
#c = T.sqrt(T.sum(T.sqr(T.sub(x, a))))
# -T.mean(T.sqrt(T.sum(T.sqr(T.sub(self.p_y_given_x, y)))))
s = T.sum(W, axis=1)

calc = theano.function(
    inputs=[a,b,W],
    outputs=[c])

sss = theano.function(
    inputs=[W],
    outputs=[s])

#print calc((1, 2), (5, 6), ((3,4), (-7,8)))
print sss(((1, 2, 3), (4, 5, 6), (7, 8, 9)))