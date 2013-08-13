# -*- coding: utf-8 -*-
"""
Created on Sat Feb 16 00:43:40 2013

@author: Nikolay
"""

import os, sys
import time
import csv
import numpy
import cPickle
import theano

from DL.SdA_modified import SdA
from Utils.util import list_spectrum_data, shuffle_2, asarray

class SdATrainer(object):
    
    def __init__(self, outs, sda_file, log_activation, is_vector_y,
                 train_file='E:/Dev/git/my_repository/chordest/result/train_dA_c.csv',
                 layers_file='model/sda_layers.dat'):
        self.train_file = train_file
        self.layers_file = layers_file
        self.sda_file = sda_file
        self.ins = 48
        self.layers_sizes = [96, 48]
        self.recurrent_layer = -1
        self.corruption_levels = [.2, .2, .2]
        self.outs = outs
        self.pretrain_lr=0.03
        self.finetune_lr=0.01 # was 0.01
        self.pretraining_epochs=15
        self.finetune_epochs=15
        self.training_epochs=1000
        self.batch_size=5
        self.log_activation = log_activation
        self.is_vector_y = is_vector_y

    def prepare_chords(self, chords):
        raise NotImplementedError("error message")

    def chords_to_array(self, chords):
        raise NotImplementedError("error message")

    def prepare_data(self, array):
        return array

    def read_data(self):
        with open(self.train_file, 'rb') as f:
            reader = csv.reader(f)
            (array, chords) = list_spectrum_data(reader, components=self.ins)
        (array, chords) = shuffle_2(array, chords)
        array = self.prepare_data(array)
        chords = self.prepare_chords(chords)
        train = int(0.7 * len(array))
        test = int(0.85 * len(array))
        train_array = asarray(array[:train])
        train_chords = self.chords_to_array(chords[:train])
        
        test_array = asarray(array[train:test])
        test_chords = self.chords_to_array(chords[train:test])
        
        valid_array = asarray(array[test:])
        valid_chords = self.chords_to_array(chords[test:])
        
        return [[train_array, train_chords], [test_array, test_chords], \
                [valid_array, valid_chords]]

    def load_layers(self):
        da = []
        sigmoid = []
        if (not os.path.isfile(self.layers_file)):
            return None
        with open(self.layers_file, 'rb') as f:
            (da, sigmoid) = cPickle.load(f)
        return (da, sigmoid)
#        return None
    
    def train_SdA(self):
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
    
        datasets = self.read_data()
    
        train_set_x, train_set_y = datasets[0]
        valid_set_x, valid_set_y = datasets[1]
        test_set_x, test_set_y = datasets[2]
    
        # compute number of minibatches for training, validation and testing
        n_train_batches = train_set_x.get_value(borrow=True).shape[0]
        n_train_batches /= self.batch_size
    
        # numpy random generator
        print '... building the model'
    
        # construct the stacked denoising autoencoder class
        layers = self.load_layers()
        if (layers):
            sda = SdA(n_ins=self.ins, hidden_layers_sizes=self.layers_sizes,
                      n_outs=self.outs, log_activation=self.log_activation,
                      is_vector_y=self.is_vector_y, layers=layers)
        else:
            sda = SdA(n_ins=self.ins, hidden_layers_sizes=self.layers_sizes,
                      n_outs=self.outs, log_activation=self.log_activation,
                      is_vector_y=self.is_vector_y,
                      recurrent_layer = self.recurrent_layer)
    
        #########################
        # PRETRAINING THE MODEL #
        #########################
        if (not layers):
            print '... getting the pretraining functions'
            pretraining_fns = sda.pretraining_functions(train_set_x=train_set_x,
                                                        batch_size=self.batch_size)
        
            print '... pre-training the model'
            start_time = time.clock()
            ## Pre-train layer-wise
            for i in xrange(sda.n_layers):
                # go through pretraining epochs
                for epoch in xrange(self.pretraining_epochs):
                    # go through the training set
                    c = []
                    for batch_index in xrange(n_train_batches):
                        c.append(pretraining_fns[i](index=batch_index,
                                 corruption=self.corruption_levels[i],
                                 lr=self.pretrain_lr))
                    print 'Pre-training layer %i, epoch %d, cost ' % (i, epoch),
                    print numpy.mean(c)
            with open(self.layers_file, 'wb') as f:
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
                    datasets=datasets, batch_size=self.batch_size,
                    learning_rate=self.finetune_lr, useQuadratic=not self.is_vector_y)
    
        print '... finetunning the model'
        # early-stopping parameters
        #n_train_batches = train_set_x.get_value(borrow=True).shape[0]
        patience = self.finetune_epochs * n_train_batches  # look as this many examples regardless
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
    
        while (epoch < self.training_epochs) and (not done_looping):
            for minibatch_index in xrange(n_train_batches):
                [pre_act, minibatch_avg_cost] = train_fn(minibatch_index)
                iter = epoch * n_train_batches + minibatch_index
    
                if (iter + 1) % validation_frequency == 0:
                    validation_losses = validate_model()
                    this_validation_loss = numpy.mean(validation_losses, axis=0)[1]
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
    
        with open(self.sda_file, 'wb') as f:
            cPickle.dump((sda.dA_layers, sda.sigmoid_layers, sda.logLayer), f)
        
