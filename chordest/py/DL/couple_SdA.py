# -*- coding: utf-8 -*-
"""
Created on Mon Mar 18 00:28:44 2013

@author: Nikolay
"""

import numpy
import theano
import theano.tensor as T

from logistic_regression_modified import LogisticRegression

class couple_SdA(object):
    """
    This class couples a pair of SdAs and adds one more logistic regression
    layer on top of them. The top layer has 25 outs, 1 per each major and 
    minor chord plus 1 for no chord.
    """
    
    def __init__(self, sda_chord, sda_bass, logLayer=None):
        print("Coupling 2 SdAs...")
        
        self.y = T.ivector('y')
        
        self.sda_chord = sda_chord
        self.sda_bass = sda_bass
        joint_input = T.concatenate([sda_chord.logLayer.p_y_given_x, sda_bass.logLayer.p_y_given_x], axis=1)
        if (logLayer):
            self.logLayer = LogisticRegression(
                         input=joint_input,
                         n_in=24, n_out=25,
                         W=logLayer.W, b=logLayer.b,
                         activation=T.nnet.softmax)
        else:
            self.logLayer = LogisticRegression(
                         input=joint_input,
                         n_in=24, n_out=25,
                         W=None, b=None,
                         activation=T.nnet.softmax)
                         
        self.params = []
        self.params.extend(self.logLayer.params)
        self.params.extend(sda_chord.params)
        self.params.extend(sda_bass.params)
        
        self.finetune_cost = self.logLayer.negative_log_likelihood(self.y)
        self.errors = self.logLayer.errors(self.y)
        
        self.output = self.logLayer.y_pred
    
    def build_finetune_functions(self, datasets, batch_size, learning_rate):
        '''Generates a function `train` that implements one step of
        finetuning, a function `validate` that computes the error on
        a batch from the validation set, and a function `test` that
        computes the error on a batch from the testing set

        :type datasets: list of pairs of theano.tensor.TensorType
        :param datasets: It is a list that contain all the datasets;
                         the has to contain three pairs, `train`,
                         `valid`, `test` in this order, where each pair
                         is formed of two Theano variables, one for the
                         datapoints, the other for the labels

        :type batch_size: int
        :param batch_size: size of a minibatch

        :type learning_rate: float
        :param learning_rate: learning rate used during finetune stage
        '''

        (train_set_x, train_set_y) = datasets[0]
        (valid_set_x, valid_set_y) = datasets[1]
        (test_set_x, test_set_y) = datasets[2]

        # compute number of minibatches for training, validation and testing
        n_valid_batches = valid_set_x.get_value(borrow=True).shape[0]
        n_valid_batches /= batch_size
        n_test_batches = test_set_x.get_value(borrow=True).shape[0]
        n_test_batches /= batch_size

        index = T.lscalar('index')  # index to a [mini]batch

        # compute the gradients with respect to the model parameters
        gparams = T.grad(self.finetune_cost, self.params)

        # compute list of fine-tuning updates
        updates = {}
        for param, gparam in zip(self.params, gparams):
            updates[param] = param - gparam * learning_rate

        train_fn = theano.function(inputs=[index],
              outputs=self.finetune_cost,
              updates=updates,
              givens={
                self.sda_chord.x: train_set_x[index * batch_size:
                                    (index + 1) * batch_size],
                self.sda_bass.x: train_set_x[index * batch_size:
                                    (index + 1) * batch_size],
                self.y: train_set_y[index * batch_size:
                                    (index + 1) * batch_size]})
        test_score_i = theano.function([index], self.errors,
                 givens={
                   self.sda_chord.x: test_set_x[index * batch_size:
                                      (index + 1) * batch_size],
                   self.sda_bass.x: test_set_x[index * batch_size:
                                      (index + 1) * batch_size],
                   self.y: test_set_y[index * batch_size:
                                      (index + 1) * batch_size]})

        valid_score_i = theano.function([index], self.errors,
              givens={
                 self.sda_chord.x: valid_set_x[index * batch_size:
                                     (index + 1) * batch_size],
                 self.sda_bass.x: valid_set_x[index * batch_size:
                                     (index + 1) * batch_size],
                 self.y: valid_set_y[index * batch_size:
                                     (index + 1) * batch_size]})

        # Create a function that scans the entire validation set
        def valid_score():
            return [valid_score_i(i) for i in xrange(n_valid_batches)]

        # Create a function that scans the entire test set
        def test_score():
            return [test_score_i(i) for i in xrange(n_test_batches)]

        return train_fn, valid_score, test_score

    def get_result(self, m):
        result = theano.function([], self.output,
                                 givens={self.sda_chord.x: m, self.sda_bass.x: m})
        return result()
