# -*- coding: utf-8 -*-
"""
Created on Wed Jan 16 22:34:44 2013

@author: Nikolay
"""

import collections
import numpy
import pandas
import theano
import theano.tensor as T
from theano.tensor.shared_randomstreams import RandomStreams

from logistic_regression_modified import LogisticRegression
from hidden_layer_modified import HiddenLayer, HiddenRecurrentLayer
from dA_modified import dA

class SdA(object):
    """Stacked denoising auto-encoder class (SdA)

    A stacked denoising autoencoder model is obtained by stacking several
    dAs. The hidden layer of the dA at layer `i` becomes the input of
    the dA at layer `i+1`. The first layer dA gets as input the input of
    the SdA, and the hidden layer of the last dA represents the output.
    Note that after pretraining, the SdA is dealt with as a normal MLP,
    the dAs are only used to initialize the weights.
    """

    def __init__(self, n_ins, hidden_layers_sizes, n_outs,
                 layers=None, log_layer=None, 
                 is_vector_y=False, log_activation=T.nnet.softmax):
        """ This class is made to support a variable number of layers.

        :type theano_rng: theano.tensor.shared_randomstreams.RandomStreams
        :param theano_rng: Theano random generator; if None is given one is
                           generated based on a seed drawn from `rng`

        :type n_ins: int
        :param n_ins: dimension of the input to the sdA

        :type n_layers_sizes: list of ints
        :param n_layers_sizes: intermediate layers size, must contain
                               at least one value

        :type n_outs: int
        :param n_outs: dimension of the output of the network

        """
        
        print("Creating a stacked denoising autoencoder with:")
        print("\tinput size: " + str(n_ins))
        print("\thidden layers sizes: " + str(hidden_layers_sizes))
        print("\toutput size: " + str(n_outs))

        self.sigmoid_layers = []
        self.dA_layers = []
        self.n_layers = len(hidden_layers_sizes)
            
        self.params = []
        assert self.n_layers > 0

        numpy_rng = numpy.random.RandomState(89677)
        theano_rng = RandomStreams(numpy_rng.randint(2 ** 30))
        # allocate symbolic variables for the data
        self.x = T.matrix('x')  # the spectrum data
        if (is_vector_y):
            self.y = T.ivector('y')
        else:
            self.y = T.matrix('y')  # target tonnetz vectors

        # The SdA is an MLP, for which all weights of intermediate layers
        # are shared with a different denoising autoencoders
        # We will first construct the SdA as a deep multilayer perceptron,
        # and when constructing each sigmoidal layer we also construct a
        # denoising autoencoder that shares weights with that layer
        # During pretraining we will train these autoencoders (which will
        # lead to chainging the weights of the MLP as well)
        # During finetunining we will finish training the SdA by doing
        # stochastich gradient descent on the MLP

        for i in xrange(self.n_layers):
            # construct the sigmoidal layer

            # the size of the input is either the number of hidden units of
            # the layer below or the input size if we are on the first layer
            if i == 0:
                input_size = n_ins
            else:
                input_size = hidden_layers_sizes[i - 1]

            # the input to this layer is either the activation of the hidden
            # layer below or the input of the SdA if you are on the first
            # layer

            if i == 0:
                layer_input = self.x
            else:
                layer_input = self.sigmoid_layers[-1].output

            if (layers):
                if isinstance(layers[1][i], HiddenRecurrentLayer):
                    sigmoid_layer = HiddenRecurrentLayer(rng=numpy_rng,
                                        input=layer_input,
                                        n_in=input_size,
                                        n_out=hidden_layers_sizes[i],
                                        activation=T.nnet.sigmoid,
                                        W=layers[1][i].W,
                                        b=layers[1][i].b,
                                        U=layers[1][i].U)
                else:
                    sigmoid_layer = HiddenLayer(rng=numpy_rng,
                                        input=layer_input,
                                        n_in=input_size,
                                        n_out=hidden_layers_sizes[i],
                                        activation=T.nnet.sigmoid,
                                        W=layers[1][i].W,
                                        b=layers[1][i].b)
            else:
                if i == self.n_layers - 5:  # no recurrent layers
                    sigmoid_layer = HiddenRecurrentLayer(rng=numpy_rng,
                                        input=layer_input,
                                        n_in=input_size,
                                        n_out=hidden_layers_sizes[i],
                                        activation=T.nnet.sigmoid)
                else:
                    sigmoid_layer = HiddenLayer(rng=numpy_rng,
                                        input=layer_input,
                                        n_in=input_size,
                                        n_out=hidden_layers_sizes[i],
                                        activation=T.nnet.sigmoid)
            # add the layer to our list of layers
            self.sigmoid_layers.append(sigmoid_layer)
            
            # Construct a denoising autoencoder that shared weights with this
            # layer
            dA_layer = dA(numpy_rng=numpy_rng,
                      theano_rng=theano_rng,
                      input=layer_input,
                      n_visible=input_size,
                      n_hidden=hidden_layers_sizes[i],
                      W=sigmoid_layer.W,
                      bhid=sigmoid_layer.b)
            self.dA_layers.append(dA_layer)
                
            # its arguably a philosophical question...
            # but we are going to only declare that the parameters of the
            # sigmoid_layers are parameters of the StackedDAA
            # the visible biases in the dA are parameters of those
            # dA, but not the SdA
            self.params.extend(sigmoid_layer.params)

        # We now need to add a logistic layer on top of the MLP
        if (log_layer):
            self.logLayer = LogisticRegression(
                             input=self.sigmoid_layers[-1].output,
                             n_in=hidden_layers_sizes[-1], n_out=n_outs,
                             W=log_layer.W, b=log_layer.b,
                             activation=log_activation)
        else:
            self.logLayer = LogisticRegression(
                             input=self.sigmoid_layers[-1].output,
                             n_in=hidden_layers_sizes[-1], n_out=n_outs,
                             activation=log_activation)

        self.params.extend(self.logLayer.params)
        
        # construct a function that implements one step of finetunining

        if (is_vector_y):
            # compute the cost for second phase of training,
            # defined as the negative log likelihood
            [self.pre_act, self.finetune_cost] = self.logLayer.negative_log_likelihood(self.y)
            # compute the gradients with respect to the model parameters
            # symbolic variable that points to the number of errors made on the
            # minibatch given by self.x and self.y
            self.errors = self.logLayer.errors(self.y)
        else:
            [self.pre_act, self.quadratic_loss] = self.logLayer.quadratic_loss(self.y)

    def pretraining_functions(self, train_set_x, batch_size):
        ''' Generates a list of functions, each of them implementing one
        step in trainnig the dA corresponding to the layer with same index.
        The function will require as input the minibatch index, and to train
        a dA you just need to iterate, calling the corresponding function on
        all minibatch indexes.

        :type train_set_x: theano.tensor.TensorType
        :param train_set_x: Shared variable that contains all datapoints used
                            for training the dA

        :type batch_size: int
        :param batch_size: size of a [mini]batch

        :type learning_rate: float
        :param learning_rate: learning rate used during training for any of
                              the dA layers
        '''

        # index to a [mini]batch
        index = T.lscalar('index')  # index to a minibatch
        corruption_level = T.scalar('corruption')  # % of corruption to use
        learning_rate = T.scalar('lr')  # learning rate to use
        # number of batches
        n_batches = train_set_x.get_value(borrow=True).shape[0] / batch_size
        # begining of a batch, given `index`
        batch_begin = index * batch_size
        # ending of a batch given `index`
        batch_end = batch_begin + batch_size

        def detect_nan(i, node, fn):
            for output in fn.outputs:
                if numpy.any(pandas.isnull(output[0])):
                    print '*** NaN detected ***'
                    print 'Inputs : %s' % [input[0] for input in fn.inputs]
#                theano.printing.debugprint(node)
                    print 'Outputs: %s' % [output[0] for output in fn.outputs]
                    raise Exception('Nan detected')
                break

        pretrain_fns = []
        for dA in self.dA_layers:
            # get the cost and the updates list
            cost, updates = dA.get_cost_updates(corruption_level,
                                                learning_rate)
            # compile the theano function
            fn = theano.function(inputs=[index,
                              theano.Param(corruption_level, default=0.2),
                              theano.Param(learning_rate, default=0.1)],
                                 outputs=cost,
                                 updates=updates,
                                 givens={self.x: train_set_x[batch_begin:
                                                             batch_end]})#,
                                #mode=theano.compile.MonitorMode(
                                #post_func=detect_nan))
            # append `fn` to the list of functions
            pretrain_fns.append(fn)

        return pretrain_fns

    def build_finetune_functions(self, datasets, batch_size, learning_rate, useQuadratic=True):
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
        if (useQuadratic):
            gparams = T.grad(self.quadratic_loss, self.params)
        else:
            gparams = T.grad(self.finetune_cost, self.params)

        # compute list of fine-tuning updates
        updates = {}
        for param, gparam in zip(self.params, gparams):
            updates[param] = param - gparam * learning_rate
        updates=collections.OrderedDict(updates.items())
#        if isinstance(self.sigmoid_layers[-1], HiddenRecurrentLayer):
#            updates[self.sigmoid_layers[-1].prev] = self.sigmoid_layers[-1].output

        if (useQuadratic):
            train_fn = theano.function(inputs=[index],
                  outputs=[self.pre_act, self.quadratic_loss],
                  updates=updates,
                  givens={
                    self.x: train_set_x[index * batch_size:
                                        (index + 1) * batch_size],
                    self.y: train_set_y[index * batch_size:
                                        (index + 1) * batch_size]})
            test_score_i = theano.function([index], [self.pre_act, self.quadratic_loss],
                  givens={
                    self.x: test_set_x[index * batch_size:
                                        (index + 1) * batch_size],
                    self.y: test_set_y[index * batch_size:
                                        (index + 1) * batch_size]})
            valid_score_i = theano.function([index], [self.pre_act, self.quadratic_loss],
                  givens={
                    self.x: valid_set_x[index * batch_size:
                                        (index + 1) * batch_size],
                    self.y: valid_set_y[index * batch_size:
                                        (index + 1) * batch_size]})
        else:
            train_fn = theano.function(inputs=[index],
                  outputs=self.finetune_cost,
                  updates=updates,
                  givens={
                    self.x: train_set_x[index * batch_size:
                                        (index + 1) * batch_size],
                    self.y: train_set_y[index * batch_size:
                                        (index + 1) * batch_size]})
            test_score_i = theano.function([index], self.errors,
                     givens={
                       self.x: test_set_x[index * batch_size:
                                          (index + 1) * batch_size],
                       self.y: test_set_y[index * batch_size:
                                          (index + 1) * batch_size]})
    
            valid_score_i = theano.function([index], self.errors,
                  givens={
                     self.x: valid_set_x[index * batch_size:
                                         (index + 1) * batch_size],
                     self.y: valid_set_y[index * batch_size:
                                         (index + 1) * batch_size]})

        # Create a function that scans the entire validation set
        def valid_score():
            return [valid_score_i(i)[1] for i in xrange(n_valid_batches)]

        # Create a function that scans the entire test set
        def test_score():
            return [test_score_i(i)[1] for i in xrange(n_test_batches)]

        return train_fn, valid_score, test_score

    def get_result(self, m):
        z = self.logLayer.p_y_given_x
        result = theano.function([], z, givens={self.x: m})
        return result()

    def get_pred(self, m):
        z = self.logLayer.y_pred
        result = theano.function([], z, givens={self.x: m})
        return result()

    def get_sda_features(self, m):
        z = self.sigmoid_layers[-1].output
        result = theano.function([], z, givens={self.x: m})
        return result()