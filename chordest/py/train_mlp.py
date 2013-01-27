# -*- coding: utf-8 -*-
"""
Created on Tue Jan 08 15:07:38 2013

@author: Nikolay
"""
import os, sys
import time
import csv
import numpy
import cPickle

import theano
import theano.tensor as T

from chord_utils import list_spectrum_data, to_tonnetz, to_uniform, shuffle_2
from dl_code.mlp import MLP

def read_data(uniformize=False):
    with open('data/train_mlp.csv', 'rb') as f:
        reader = csv.reader(f)
        (array, chords) = list_spectrum_data(reader)
    if (uniformize):
        with open('data/lambdas.csv', 'rb') as fl:
            reader = csv.reader(fl)
            l = reader.next()
        l = map(float, l)
        array = map(lambda x: to_uniform(x, l), array) # to uniformly distributed columns
        
    (array, chords) = shuffle_2(array, chords)
    chords = to_tonnetz(chords)
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

def asarray(array):
    return theano.shared(numpy.asarray(array, dtype=theano.config.floatX),
                                 borrow=True)

def test_mlp(learning_rate=0.01, L1_reg=0.00, L2_reg=0.01, n_epochs=1000,
             batch_size=10, n_hidden=120):
    """
    Demonstrate stochastic gradient descent optimization for a multilayer
    perceptron

    This is demonstrated on autoencode features of chordest constant-Q
    spectra.

    :type learning_rate: float
    :param learning_rate: learning rate used (factor for the stochastic
    gradient

    :type L1_reg: float
    :param L1_reg: L1-norm's weight when added to the cost (see
    regularization)

    :type L2_reg: float
    :param L2_reg: L2-norm's weight when added to the cost (see
    regularization)

    :type n_epochs: int
    :param n_epochs: maximal number of epochs to run the optimizer


   """
    datasets = read_data(False)

    train_set_x, train_set_y = datasets[0]
    test_set_x, test_set_y = datasets[1]
    valid_set_x, valid_set_y = datasets[2]

    # compute number of minibatches for training, validation and testing
    n_train_batches = train_set_x.get_value(borrow=True).shape[0] / batch_size
    n_valid_batches = valid_set_x.get_value(borrow=True).shape[0] / batch_size
    n_test_batches = test_set_x.get_value(borrow=True).shape[0] / batch_size

    ######################
    # BUILD ACTUAL MODEL #
    ######################
    print '... building the model'

    # allocate symbolic variables for the data
    index = T.lscalar()  # index to a [mini]batch
    x = T.matrix('x')  # the data is presented as 1D vector of autoencoder features
    y = T.matrix('y')  # the labels are presented as 1D Tonnetz vector

    rng = numpy.random.RandomState(1234)

    # construct the MLP class
    classifier = MLP(rng=rng, input=x, n_in=200,
                     n_hidden=n_hidden, n_out=6)

    # the cost we minimize during training is the negative log likelihood of
    # the model plus the regularization terms (L1 and L2); cost is expressed
    # here symbolically
    cost = classifier.quadratic_loss(y) \
         + L1_reg * classifier.L1 \
         + L2_reg * classifier.L2_sqr

    # compiling a Theano function that computes the mistakes that are made
    # by the model on a minibatch
    test_model = theano.function(inputs=[index],
            outputs=classifier.quadratic_loss(y),
            givens={
                x: test_set_x[index * batch_size:(index + 1) * batch_size],
                y: test_set_y[index * batch_size:(index + 1) * batch_size]})

    validate_model = theano.function(inputs=[index],
            outputs=classifier.quadratic_loss(y),
            givens={
                x: valid_set_x[index * batch_size:(index + 1) * batch_size],
                y: valid_set_y[index * batch_size:(index + 1) * batch_size]})

    # compute the gradient of cost with respect to theta (sotred in params)
    # the resulting gradients will be stored in a list gparams
    gparams = []
    for param in classifier.params:
        gparam = T.grad(cost, param)
        gparams.append(gparam)

    # specify how to update the parameters of the model as a dictionary
    updates = {}
    # given two list the zip A = [a1, a2, a3, a4] and B = [b1, b2, b3, b4] of
    # same length, zip generates a list C of same size, where each element
    # is a pair formed from the two lists :
    #    C = [(a1, b1), (a2, b2), (a3, b3), (a4, b4)]
    for param, gparam in zip(classifier.params, gparams):
        updates[param] = param - learning_rate * gparam

    # compiling a Theano function `train_model` that returns the cost, but
    # in the same time updates the parameter of the model based on the rules
    # defined in `updates`
    train_model = theano.function(inputs=[index], outputs=cost,
            updates=updates,
            givens={
                x: train_set_x[index * batch_size:(index + 1) * batch_size],
                y: train_set_y[index * batch_size:(index + 1) * batch_size]})

    ###############
    # TRAIN MODEL #
    ###############
    print '... training'

    # early-stopping parameters
    patience = 20000  # look as this many examples regardless
    patience_increase = 2  # wait this much longer when a new best is
                           # found
    improvement_threshold = 0.995  # a relative improvement of this much is
                                   # considered significant
    validation_frequency = min(n_train_batches, patience / 2)
                                  # go through this many
                                  # minibatches before checking the network
                                  # on the validation set; in this case we
                                  # check every epoch

    best_params = None
    best_validation_loss = numpy.inf
    best_iter = 0
    test_score = 0.
    start_time = time.clock()

    epoch = 0
    done_looping = False

    while (epoch < n_epochs) and (not done_looping):
        epoch = epoch + 1
        for minibatch_index in xrange(n_train_batches):

            minibatch_avg_cost = train_model(minibatch_index)
            # iteration number
            iter = epoch * n_train_batches + minibatch_index

            if (iter + 1) % validation_frequency == 0:
                # compute zero-one loss on validation set
                validation_losses = [validate_model(i) for i
                                     in xrange(n_valid_batches)]
                this_validation_loss = numpy.mean(validation_losses)

                print('epoch %i, minibatch %i/%i, validation error %f' %
                     (epoch, minibatch_index + 1, n_train_batches,
                      this_validation_loss))

                # if we got the best validation score until now
                if this_validation_loss < best_validation_loss:
                    #improve patience if loss improvement is good enough
                    if this_validation_loss < best_validation_loss *  \
                           improvement_threshold:
                        patience = max(patience, iter * patience_increase)

                    best_validation_loss = this_validation_loss
                    best_iter = iter

                    # test it on the test set
                    test_losses = [test_model(i) for i
                                   in xrange(n_test_batches)]
                    test_score = numpy.mean(test_losses)

                    print(('     epoch %i, minibatch %i/%i, test error of '
                           'best model %f') %
                          (epoch, minibatch_index + 1, n_train_batches,
                           test_score))

            if patience <= iter:
                    done_looping = True
                    break

    with open('dA_spectrum/mlp.dat', 'wb') as f:
        cPickle.dump((classifier.hiddenLayer, classifier.logRegressionLayer), f)
    
    end_time = time.clock()
    print(('Optimization complete. Best validation score of %f '
           'obtained at iteration %i, with test performance %f') %
          (best_validation_loss, best_iter, test_score))
    print >> sys.stderr, ('The code for file ' +
                          os.path.split(__file__)[1] +
                          ' ran for %.2fm' % ((end_time - start_time) / 60.))


if __name__ == '__main__':
    test_mlp()
