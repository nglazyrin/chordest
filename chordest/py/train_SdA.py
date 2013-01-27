# -*- coding: utf-8 -*-
"""
Created on Wed Jan 16 22:44:28 2013

@author: Nikolay
"""

import os, sys
import time
import csv
import numpy
import cPickle
import theano

from SdA_modified import SdA
from chord_utils import list_spectrum_data, shuffle_2, to_tonnetz, asarray

train_file = 'E:/Dev/git/my_repository/chordest/result/train_dA.csv'
layers_file = 'dA_spectrum/sda_layers_p.dat'
sda_file = 'dA_spectrum/SdA.dat'
ins = 48
layers_sizes = [40, 40]
outs = 6

def read_data():
    with open(train_file, 'rb') as f:
        reader = csv.reader(f)
        (array, chords) = list_spectrum_data(reader, components=ins)
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

def load_layers():
    da = []
    sigmoid = []
    if (not os.path.isfile(layers_file)):
        return None
    with open(layers_file, 'rb') as f:
        (da, sigmoid) = cPickle.load(f)
#    return (da, sigmoid)
    return None

def train_SdA(finetune_lr=0.01, pretraining_epochs=20,
             pretrain_lr=0.03, training_epochs=1000, batch_size=3):
    """
    Demonstrates how to train and test a stochastic denoising autoencoder.

    This is demonstrated on MNIST.

    :type learning_rate: float
    :param learning_rate: learning rate used in the finetune stage
    (factor for the stochastic gradient)

    :type pretraining_epochs: int
    :param pretraining_epochs: number of epoch to do pretraining

    :type pretrain_lr: float
    :param pretrain_lr: learning rate to be used during pre-training

    :type n_iter: int
    :param n_iter: maximal number of iterations ot run the optimizer

    """

    datasets = read_data()

    train_set_x, train_set_y = datasets[0]
    valid_set_x, valid_set_y = datasets[1]
    test_set_x, test_set_y = datasets[2]

    # compute number of minibatches for training, validation and testing
    n_train_batches = train_set_x.get_value(borrow=True).shape[0]
    n_train_batches /= batch_size

    # numpy random generator
    numpy_rng = numpy.random.RandomState(89677)
    print '... building the model'

    # construct the stacked denoising autoencoder class
    layers = load_layers()
    if (layers):
        sda = SdA(numpy_rng=numpy_rng, n_ins=ins, layers=layers,
                  hidden_layers_sizes=layers_sizes, n_outs=outs)
    else:
        sda = SdA(numpy_rng=numpy_rng, n_ins=ins,
                  hidden_layers_sizes=layers_sizes, n_outs=outs)

    #########################
    # PRETRAINING THE MODEL #
    #########################
    if (not layers):
        print '... getting the pretraining functions'
        pretraining_fns = sda.pretraining_functions(train_set_x=train_set_x,
                                                    batch_size=batch_size)
    
        print '... pre-training the model'
        start_time = time.clock()
        ## Pre-train layer-wise
        corruption_levels = [.1, .2, .3]
        for i in xrange(sda.n_layers):
            # go through pretraining epochs
            for epoch in xrange(pretraining_epochs):
                # go through the training set
                c = []
                for batch_index in xrange(n_train_batches):
                    c.append(pretraining_fns[i](index=batch_index,
                             corruption=corruption_levels[i],
                             lr=pretrain_lr))
                print 'Pre-training layer %i, epoch %d, cost ' % (i, epoch),
                print numpy.mean(c)
        with open('dA_spectrum/sda_layers_p.dat', 'wb') as f:
            cPickle.dump((sda.dA_layers, sda.sigmoid_layers), f)
    
        end_time = time.clock()
    
        print >> sys.stderr, ('The pretraining code for file ' +
                              os.path.split(__file__)[1] +
                              ' ran for %.2fm' % ((end_time - start_time) / 60.))

    ########################
    # FINETUNING THE MODEL #
    ########################

    # get the training, validation and testing function for the model
    print '... getting the finetuning functions'
    train_fn, validate_model, test_model = sda.build_finetune_functions(
                datasets=datasets, batch_size=1, #batch_size=batch_size,
                learning_rate=finetune_lr)

    print '... finetunning the model'
    # early-stopping parameters
    patience = 10 * n_train_batches  # look as this many examples regardless
    patience_increase = 2.  # wait this much longer when a new best is
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
    test_score = 0.
    start_time = time.clock()

    done_looping = False
    epoch = 0

    while (epoch < training_epochs) and (not done_looping):
        for minibatch_index in xrange(n_train_batches):
            minibatch_avg_cost = train_fn(minibatch_index)
            iter = epoch * n_train_batches + minibatch_index

            if (iter + 1) % validation_frequency == 0:
                validation_losses = validate_model()
                this_validation_loss = numpy.mean(validation_losses)
                print('epoch %i, minibatch %i/%i, validation error %f %%' %
                      (epoch, minibatch_index + 1, n_train_batches,
                       this_validation_loss * 100.))

                # if we got the best validation score until now
                if this_validation_loss < best_validation_loss:

                    #improve patience if loss improvement is good enough
                    if (this_validation_loss < best_validation_loss *
                        improvement_threshold):
                        patience = max(patience, iter * patience_increase)

                    # save best validation score and iteration number
                    best_validation_loss = this_validation_loss
                    best_iter = iter

                    # test it on the test set
                    test_losses = test_model()
                    test_score = numpy.mean(test_losses)
                    print(('     epoch %i, minibatch %i/%i, test error of '
                           'best model %f %%') %
                          (epoch, minibatch_index + 1, n_train_batches,
                           test_score * 100.))

            if patience <= iter:
                done_looping = True
                break
        epoch = epoch + 1

    end_time = time.clock()
    print(('Optimization complete with best validation score of %f %%,'
           'with test performance %f %%') %
                 (best_validation_loss * 100., test_score * 100.))
    print >> sys.stderr, ('The training code for file ' +
                          os.path.split(__file__)[1] +
                          ' ran for %.2fm' % ((end_time - start_time) / 60.))

    with open(sda_file, 'wb') as f:
        cPickle.dump((sda.dA_layers, sda.sigmoid_layers, sda.logLayer), f)
    

if __name__ == '__main__':
    train_SdA()
