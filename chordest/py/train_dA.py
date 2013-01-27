# -*- coding: utf-8 -*-
"""
Created on Sun Dec 16 19:21:29 2012

@author: Nikolay
"""

import os, sys
import time
import csv
import numpy
import cPickle

import theano
import theano.tensor as T
from theano.tensor.shared_randomstreams import RandomStreams
from dl_code.dA import dA
from chord_utils import list_spectrum_data

def read_data():
    with open('data/train_dA.csv', 'rb') as f:
        reader = csv.reader(f)
        (array, chords) = list_spectrum_data(reader)
    result = theano.shared(numpy.asarray(array, dtype=theano.config.floatX),
                                 borrow=True)
    return result

def test_dA(learning_rate=0.1, training_epochs=15,
            batch_size=20, output_folder='dA_spectrum'):

    """
    This demo is tested on chordest constant-Q spectra

    :type learning_rate: float
    :param learning_rate: learning rate used for training the DeNosing
                          AutoEncoder

    :type training_epochs: int
    :param training_epochs: number of epochs used for training

    :type dataset: string
    :param dataset: path to the picked dataset

    """
    train_set = read_data()

    # compute number of minibatches for training, validation and testing
    n_train_batches = train_set.get_value(borrow=True).shape[0] / batch_size

    # allocate symbolic variables for the data
    index = T.lscalar()    # index to a [mini]batch
    x = T.matrix('x')  # the data is presented as rasterized images

    if not os.path.isdir(output_folder):
        os.makedirs(output_folder)
    os.chdir(output_folder)
    ####################################
    # BUILDING THE MODEL NO CORRUPTION #
    ####################################

    rng = numpy.random.RandomState(123)
    theano_rng = RandomStreams(rng.randint(2 ** 30))

    da = dA(numpy_rng=rng, theano_rng=theano_rng, input=x,
            n_visible=240, n_hidden=200)

    cost, updates = da.get_cost_updates(corruption_level=0.,
                                        learning_rate=learning_rate)

    train_da = theano.function([index], cost, updates=updates,
         givens={x: train_set[index * batch_size:
                                (index + 1) * batch_size]})

    start_time = time.clock()

    ############
    # TRAINING #
    ############

    # go through training epochs
    for epoch in xrange(training_epochs):
        # go through trainng set
        c = []
        for batch_index in xrange(n_train_batches):
            c.append(train_da(batch_index))

        print 'Training epoch %d, cost ' % epoch, numpy.mean(c)

    end_time = time.clock()

    training_time = (end_time - start_time)

    print >> sys.stderr, ('The no corruption code for file ' +
                          os.path.split(__file__)[1] +
                          ' ran for %.2fm' % ((training_time) / 60.))

    with open('no_corruption.dat', 'wb') as f:
        cPickle.dump(da, f)

    #####################################
    # BUILDING THE MODEL CORRUPTION 30% #
    #####################################

    rng = numpy.random.RandomState(123)
    theano_rng = RandomStreams(rng.randint(2 ** 30))

    da = dA(numpy_rng=rng, theano_rng=theano_rng, input=x,
            n_visible=240, n_hidden=200)

    cost, updates = da.get_cost_updates(corruption_level=0.3,
                                        learning_rate=learning_rate)

    train_da = theano.function([index], cost, updates=updates,
         givens={x: train_set[index * batch_size:
                                  (index + 1) * batch_size]})

    start_time = time.clock()

    ############
    # TRAINING #
    ############

    # go through training epochs
    for epoch in xrange(training_epochs):
        # go through trainng set
        c = []
        for batch_index in xrange(n_train_batches):
            c.append(train_da(batch_index))

        print 'Training epoch %d, cost ' % epoch, numpy.mean(c)

    end_time = time.clock()

    training_time = (end_time - start_time)

    print >> sys.stderr, ('The 30% corruption code for file ' +
                          os.path.split(__file__)[1] +
                          ' ran for %.2fm' % (training_time / 60.))

    with open('corruption_30.dat', 'wb') as f:
        cPickle.dump(da, f)
    
    os.chdir('../')


if __name__ == '__main__':
    test_dA()
