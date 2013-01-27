# -*- coding: utf-8 -*-
"""
Created on Wed Jan 09 21:49:49 2013

@author: Nikolay
"""

import theano

from dl_code.mlp import MLP, HiddenLayer
from dl_code.logistic_sgd import LogisticRegression

class MLP2(MLP):
    
    def __init__(self, rng, input, hidden, out):

        self.hiddenLayer = HiddenLayer(rng=rng, input=input,
                                       n_in = 1,  # will be ignored
                                       n_out = 1, # will be ignored
                                       activation = theano.tensor.tanh,
                                       W = hidden.W,
                                       b = hidden.b)

        self.logRegressionLayer = LogisticRegression(
            input=self.hiddenLayer.output,
            n_in=1,  # will be ignored
            n_out=6, # will be ignored
            W = out.W,
            b = out.b)

        # L1 norm ; one regularization option is to enforce L1 norm to
        # be small
        self.L1 = abs(self.hiddenLayer.W).sum() \
                + abs(self.logRegressionLayer.W).sum()

        # square of L2 norm ; one regularization option is to enforce
        # square of L2 norm to be small
        self.L2_sqr = (self.hiddenLayer.W ** 2).sum() \
                    + (self.logRegressionLayer.W ** 2).sum()

        # negative log likelihood of the MLP is given by the negative
        # log likelihood of the output of the model, computed in the
        # logistic regression layer
        self.negative_log_likelihood = self.logRegressionLayer.negative_log_likelihood
        # quadratic loss
        self.quadratic_loss = self.logRegressionLayer.quadratic_loss
        # same holds for the function computing the number of errors
        self.errors = self.logRegressionLayer.errors

        # the parameters of the model are the parameters of the two layer it is
        # made out of
        self.params = self.hiddenLayer.params + self.logRegressionLayer.params

    def get_result(self, x):
        return self.logRegressionLayer.p_y_given_x
