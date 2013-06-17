# -*- coding: utf-8 -*-
"""
Created on Wed Jan 09 21:49:49 2013

@author: Nikolay
"""

import numpy
import theano
import theano.tensor as T

from hidden_layer_modified import HiddenLayer, HiddenRecurrentLayer
from logistic_regression_modified import LogisticRegression

class MLP(object):
    
    def __init__(self, layers_size, hidden=None, out=None,
                 log_activation=T.nnet.softmax):
        rng = numpy.random.RandomState(89677)
        n_in = layers_size[0]
        n_hid = layers_size[1]
        n_out = layers_size[2]

        self.x = T.matrix('x')  # the chroma data
        self.y = T.matrix('y')  # target vectors

        if (hidden):
            self.hiddenLayer = HiddenRecurrentLayer(
                                        rng=rng,
                                        input=self.x,
                                        n_in=n_in,
                                        n_out=n_hid,
                                        activation=theano.tensor.tanh,
                                        W=hidden.W,
                                        b=hidden.b,
                                        U=hidden.U)
        else:
            self.hiddenLayer = HiddenRecurrentLayer(
                                        rng=rng,
                                        input=self.x,
                                        n_in=n_in,
                                        n_out=n_hid,
                                        activation=theano.tensor.tanh)

        if (out):
            self.logRegressionLayer = LogisticRegression(
                input=self.hiddenLayer.output,
                n_in=n_hid,
                n_out=n_out,
                W = out.W,
                b = out.b,
                activation=log_activation)
        else:
            self.logRegressionLayer = LogisticRegression(
                             input=self.hiddenLayer.output,
                             n_in=n_hid,
                             n_out=n_out,
                             activation=log_activation)

        # L1 norm ; one regularization option is to enforce L1 norm to
        # be small
        self.L1 = abs(self.hiddenLayer.W).sum() + abs(self.hiddenLayer.U).sum() \
                + abs(self.logRegressionLayer.W).sum()

        # square of L2 norm ; one regularization option is to enforce
        # square of L2 norm to be small
        self.L2_sqr = (self.hiddenLayer.W ** 2).sum() + abs(self.hiddenLayer.U ** 2).sum()\
                    + (self.logRegressionLayer.W ** 2).sum()

        # negative log likelihood of the MLP is given by the negative
        # log likelihood of the output of the model, computed in the
        # logistic regression layer
#        self.negative_log_likelihood = self.logRegressionLayer.negative_log_likelihood
        # quadratic loss
        [self.pre_act, self.quadratic_loss] = self.logRegressionLayer.quadratic_loss(self.y)
        self.lin_hidden = self.hiddenLayer.lin_output
        # same holds for the function computing the number of errors
        #self.errors = self.logRegressionLayer.errors(self.y)

        # the parameters of the model are the parameters of the two layer it is
        # made out of
        self.params = self.hiddenLayer.params + self.logRegressionLayer.params

    def get_result(self, x):
        return self.logRegressionLayer.p_y_given_x
