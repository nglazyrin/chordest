# -*- coding: utf-8 -*-
"""
Created on Sat Feb 16 00:03:43 2013

@author: Nikolay
"""

import theano.tensor as T
import numpy
import theano

class HiddenLayer(object):
    def __init__(self, rng, input, n_in, n_out, W=None, b=None,
                 activation=T.tanh):
        """
        Typical hidden layer of a MLP: units are fully-connected and have
        sigmoidal activation function. Weight matrix W is of shape (n_in,n_out)
        and the bias vector b is of shape (n_out,).

        NOTE : The nonlinearity used here is tanh

        Hidden unit activation is given by: tanh(dot(input,W) + b)

        :type rng: numpy.random.RandomState
        :param rng: a random number generator used to initialize weights

        :type input: theano.tensor.dmatrix
        :param input: a symbolic tensor of shape (n_examples, n_in)

        :type n_in: int
        :param n_in: dimensionality of input

        :type n_out: int
        :param n_out: number of hidden units

        :type activation: theano.Op or function
        :param activation: Non linearity to be applied in the hidden
                           layer
        """
        self.input = input

        # `W` is initialized with `W_values` which is uniformely sampled
        # from sqrt(-6./(n_in+n_hidden)) and sqrt(6./(n_in+n_hidden))
        # for tanh activation function
        # the output of uniform if converted using asarray to dtype
        # theano.config.floatX so that the code is runable on GPU
        # Note : optimal initialization of weights is dependent on the
        #        activation function used (among other things).
        #        For example, results presented in [Xavier10] suggest that you
        #        should use 4 times larger initial weights for sigmoid
        #        compared to tanh
        #        We have no info for other function, so we use the same as
        #        tanh.
        if W is None:
            W_values = numpy.asarray(rng.uniform(
                    low=-numpy.sqrt(6. / (n_in + n_out)),
                    high=numpy.sqrt(6. / (n_in + n_out)),
                    size=(n_in, n_out)), dtype=theano.config.floatX)
            if activation == theano.tensor.nnet.sigmoid:
                W_values *= 4

            W = theano.shared(value=W_values, name='W', borrow=True)

        if b is None:
            b_values = numpy.zeros((n_out,), dtype=theano.config.floatX)
            b = theano.shared(value=b_values, name='b', borrow=True)

        self.W = W
        self.b = b

        lin_output = T.dot(input, self.W) + self.b
        self.output = (lin_output if activation is None
                       else activation(lin_output))
        # parameters of the model
        self.params = [self.W, self.b]

class HiddenRecurrentLayer(HiddenLayer):
    def __init__(self, rng, input, n_in, n_out, W=None, b=None,
                 U=None, activation=T.tanh):
        HiddenLayer.__init__(self, rng, input, n_in, n_out, W, b, activation)
        if U is None:
            U_values = numpy.asarray(rng.uniform(
                    low=-numpy.sqrt(6. / (n_out + n_out)),
                    high=numpy.sqrt(6. / (n_out + n_out)),
                    size=(n_out, n_out)), dtype=theano.config.floatX)
            if activation == theano.tensor.nnet.sigmoid:
                U_values *= 4
            U = theano.shared(value=U_values, name='U', borrow=True)
        self.U = U

        prev_values = numpy.zeros((n_out,), dtype=theano.config.floatX)
        self.prev = theano.shared(value=prev_values, name='prev', borrow=True)
        
#        lin_output = T.dot(input, self.W) + self.b + T.dot(self.prev, self.U)
#        self.output = (lin_output if activation is None
#                       else activation(lin_output))
        # parameters of the model
        self.params = [self.W, self.b, self.U]
        
        # recurrent function (using tanh activation function) and linear output
        # activation function
        def step(x_t, h_tm1):
            lin_t = T.dot(x_t, self.W) + self.b + T.dot(h_tm1, self.U)
            y_t = activation(lin_t)
            return y_t, lin_t

        # the hidden state `h` for the entire sequence, and the output for the
        # entire sequence `y` (first dimension is always time)
        [self.output, self.lin_output], _ = theano.scan(step,
                                               sequences=self.input,
                                               outputs_info=[self.prev, None])