# -*- coding: utf-8 -*-
"""
Created on Sat Jan 26 00:55:11 2013

@author: Nikolay
"""

import cPickle
import os
import sys
import time
import csv
import numpy

import theano
import theano.tensor as T

from math import floor
from chord_utils import list_spectrum_data, shuffle_2, to_tonnetz, asarray, asmatrix
from conv_modified import LeNetConvPoolLayer
from dl_code.logistic_sgd import LogisticRegression
from dl_code.mlp import HiddenLayer


train_file = 'E:/Dev/git/my_repository/chordest/result/train_dA.csv'
conv_file = 'dA_spectrum/conv.dat'

ins = 48
outs = 6
width = 7
size = ins * width
nkerns0 = 20
nkerns1 = 50
n_sigm = 400

l0rows = ins
l0cols = width
f0rows = 7
f0cols = 2
pool_row = 1
pool_col = 2
l1rows = (l0rows - f0rows + 1) / pool_row
l1cols = (l0cols - f0cols + 1) / pool_col
f1rows = 15
f1cols = 2
l2rows = (l1rows - f1rows + 1) / pool_row
l2cols = (l1cols - f1cols + 1) / pool_col


def read_data():
    with open(train_file, 'rb') as f:
        reader = csv.reader(f)
        (array, chords) = list_spectrum_data(reader, components=ins)
    array = to_tile_array(array)
    array = array[4:-4]
    chords = chords[4:-4]
    print len(array)
    chords = to_tonnetz(chords)
    (array, chords) = shuffle_2(array, chords)
    train = int(0.7 * len(array))
    test = int(0.9 * len(array))
    train_array = asarray(array[:train])
    train_chords = asarray(chords[:train])
    
    test_array = asarray(array[train:test])
    test_chords = asarray(chords[train:test])
    
    valid_array = asarray(array[test:])
    valid_chords = asarray(chords[test:])
    
    return [[train_array, train_chords], [test_array, test_chords], \
            [valid_array, valid_chords]]

def to_tile_array(array):
    half = int(floor(width / 2))
    return [to_tile(array[i-half:i+half+1]) for i,x in enumerate(array)]

def to_tile(array):
    result = numpy.asarray(array, dtype=theano.config.floatX).reshape(-1)
    if (len(result.tolist()) < size):
        return [0] * size
    return result.tolist()

def train_conv(learning_rate=0.001, n_epochs=200,
                    nkerns=[nkerns0, nkerns1], batch_size=500):
    """ Demonstrates lenet on audio spectrum data

    :type learning_rate: float
    :param learning_rate: learning rate used (factor for the stochastic
                          gradient)

    :type n_epochs: int
    :param n_epochs: maximal number of epochs to run the optimizer

    :type nkerns: list of ints
    :param nkerns: number of kernels on each layer
    """

    rng = numpy.random.RandomState(23455)

    datasets = read_data()

    train_set_x, train_set_y = datasets[0]
    valid_set_x, valid_set_y = datasets[1]
    test_set_x, test_set_y = datasets[2]

    # compute number of minibatches for training, validation and testing
    n_train_batches = train_set_x.get_value(borrow=True).shape[0]
    n_valid_batches = valid_set_x.get_value(borrow=True).shape[0]
    n_test_batches = test_set_x.get_value(borrow=True).shape[0]
    n_train_batches /= batch_size
    n_valid_batches /= batch_size
    n_test_batches /= batch_size

    # allocate symbolic variables for the data
    index = T.lscalar()  # index to a [mini]batch
    x = T.matrix('x')   # the spectrum data
    y = T.matrix('y')  # target tonnetz vectors

    #ishape = (48, 7)  # this is the size of spectrum tile

    ######################
    # BUILD ACTUAL MODEL #
    ######################
    print '... building the model'

    # Reshape matrix of rasterized images of shape (batch_size,48*7)
    # to a 4D tensor, compatible with our LeNetConvPoolLayer
    layer0_input = x.reshape((batch_size, 1, ins, width))

    # Construct the first convolutional pooling layer:
    # filtering reduces the image size to (48-7+1,7-2+1)=(42,6)
    # maxpooling reduces this further to (42/1,6/2) = (42,3)
    # 4D output tensor is thus of shape (batch_size,nkerns[0],42,3)
    layer0 = LeNetConvPoolLayer(rng, input=layer0_input,
            image_shape=(batch_size, 1, l0rows, l0cols),
            filter_shape=(nkerns[0], 1, f0rows, f0cols),
            poolsize=(pool_row, pool_col))

    # Construct the second convolutional pooling layer
    # filtering reduces the image size to (42-15+1,3-2+1)=(28,2)
    # maxpooling reduces this further to (28/2,2/2) = (14,1)
    # 4D output tensor is thus of shape (nkerns[0],nkerns[1],14,1)
    layer1 = LeNetConvPoolLayer(rng, input=layer0.output,
            image_shape=(batch_size, nkerns[0], l1rows, l1cols),
            filter_shape=(nkerns[1], nkerns[0], f1rows, f1cols),
            poolsize=(pool_row, pool_col))

    # the TanhLayer being fully-connected, it operates on 2D matrices of
    # shape (batch_size,num_pixels) (i.e matrix of rasterized images).
    # This will generate a matrix of shape (20,32*14*1) = (20,448)
    layer2_input = layer1.output.flatten(2)

    # construct a fully-connected sigmoidal layer
    layer2 = HiddenLayer(rng, input=layer2_input,
                         n_in=nkerns[1] * l2rows * l2cols,
                         n_out=n_sigm, activation=T.tanh)

    # classify the values of the fully-connected sigmoidal layer
    layer3 = LogisticRegression(input=layer2.output, n_in=n_sigm, n_out=outs)

    # the cost we minimize during training is the NLL of the model
    #cost = layer3.negative_log_likelihood(y)
    cost = layer3.quadratic_loss(y)

    # create a function to compute the mistakes that are made by the model
    test_model = theano.function([index], layer3.quadratic_loss(y),
             givens={
                x: test_set_x[index * batch_size: (index + 1) * batch_size],
                y: test_set_y[index * batch_size: (index + 1) * batch_size]})

    validate_model = theano.function([index], layer3.quadratic_loss(y),
            givens={
                x: valid_set_x[index * batch_size: (index + 1) * batch_size],
                y: valid_set_y[index * batch_size: (index + 1) * batch_size]})

    # create a list of all model parameters to be fit by gradient descent
    params = layer3.params + layer2.params + layer1.params + layer0.params

    # create a list of gradients for all model parameters
    grads = T.grad(cost, params)

    # train_model is a function that updates the model parameters by
    # SGD Since this model has many parameters, it would be tedious to
    # manually create an update rule for each model parameter. We thus
    # create the updates dictionary by automatically looping over all
    # (params[i],grads[i]) pairs.
    updates = {}
    for param_i, grad_i in zip(params, grads):
        updates[param_i] = param_i - learning_rate * grad_i

    train_model = theano.function([index], cost, updates=updates,
          givens={
            x: train_set_x[index * batch_size: (index + 1) * batch_size],
            y: train_set_y[index * batch_size: (index + 1) * batch_size]})

    ###############
    # TRAIN MODEL #
    ###############
    print '... training'
    # early-stopping parameters
    patience = 10000  # look as this many examples regardless
    patience_increase = 2  # wait this much longer when a new best is
                           # found
    improvement_threshold = 0.995  # a relative improvement of this much is
                                   # considered significant
    validation_frequency = min(n_train_batches, patience / 2)
                                  # go through this many
                                  # minibatche before checking the network
                                  # on the validation set; in this case we
                                  # check every epoch

    best_params = None
    best_validation_loss = numpy.inf
    best_iter = 0
    test_score = 0.
    start_time = time.clock()

    epoch = 0
    done_looping = False
    print n_train_batches

    while (epoch < n_epochs) and (not done_looping):
        epoch = epoch + 1
        for minibatch_index in xrange(n_train_batches):

            iter = epoch * n_train_batches + minibatch_index

            #if iter % 100 == 0:
            print 'training @ iter = ', iter
            cost_ij = train_model(minibatch_index)
            print 'cost = ' + str(cost_ij)

            if (iter + 1) % validation_frequency == 0:

                # compute zero-one loss on validation set
                validation_losses = [validate_model(i) for i
                                     in xrange(n_valid_batches)]
                this_validation_loss = numpy.mean(validation_losses)
                print('epoch %i, minibatch %i/%i, validation error %f %%' % \
                      (epoch, minibatch_index + 1, n_train_batches, \
                       this_validation_loss * 100.))

                # if we got the best validation score until now
                if this_validation_loss < best_validation_loss:

                    #improve patience if loss improvement is good enough
                    if this_validation_loss < best_validation_loss *  \
                       improvement_threshold:
                        patience = max(patience, iter * patience_increase)

                    # save best validation score and iteration number
                    best_validation_loss = this_validation_loss
                    best_iter = iter

                    # test it on the test set
                    test_losses = [test_model(i) for i in xrange(n_test_batches)]
                    test_score = numpy.mean(test_losses)
                    print(('     epoch %i, minibatch %i/%i, test error of best '
                           'model %f %%') %
                          (epoch, minibatch_index + 1, n_train_batches,
                           test_score * 100.))

            if patience <= iter:
                done_looping = True
                break

    end_time = time.clock()
    print('Optimization complete.')
    print('Best validation score of %f %% obtained at iteration %i,'\
          'with test performance %f %%' %
          (best_validation_loss * 100., best_iter, test_score * 100.))
    print >> sys.stderr, ('The code for file ' +
                          os.path.split(__file__)[1] +
                          ' ran for %.2fm' % ((end_time - start_time) / 60.))
    with open(conv_file, 'wb') as f:
        cPickle.dump(((layer0.W, layer0.b),
                      (layer1.W, layer1.b),
                      (layer2.W, layer2.b),
                      (layer3.W, layer3.b)), f)

if __name__ == '__main__':
    train_conv()


def experiment(state, channel):
    train_conv(state.learning_rate, dataset=state.dataset)
