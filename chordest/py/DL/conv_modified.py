# -*- coding: utf-8 -*-
"""
Created on Sat Jan 26 00:45:43 2013

@author: Nikolay
"""


import numpy

import theano
import theano.tensor as T
from theano.tensor.signal import downsample
from theano.tensor.nnet import conv
from dl_code.logistic_sgd import LogisticRegression
from dl_code.mlp import HiddenLayer


class LeNetConvPoolLayer(object):
    """Pool Layer of a convolutional network """

    def __init__(self, rng, input, filter_shape, image_shape,
                 poolsize=(2, 2), W=None, b=None):
        """
        Allocate a LeNetConvPoolLayer with shared variable internal parameters.

        :type rng: numpy.random.RandomState
        :param rng: a random number generator used to initialize weights

        :type input: theano.tensor.dtensor4
        :param input: symbolic image tensor, of shape image_shape

        :type filter_shape: tuple or list of length 4
        :param filter_shape: (number of filters, num input feature maps,
                              filter height,filter width)

        :type image_shape: tuple or list of length 4
        :param image_shape: (batch size, num input feature maps,
                             image height, image width)

        :type poolsize: tuple or list of length 2
        :param poolsize: the downsampling (pooling) factor (#rows,#cols)
        """

        assert image_shape[1] == filter_shape[1]
        self.input = input

        # there are "num input feature maps * filter height * filter width"
        # inputs to each hidden unit
        fan_in = numpy.prod(filter_shape[1:])
        # each unit in the lower layer receives a gradient from:
        # "num output feature maps * filter height * filter width" /
        #   pooling size
        fan_out = (filter_shape[0] * numpy.prod(filter_shape[2:]) /
                   numpy.prod(poolsize))
        
        if (W):
            #self.W = theano.shared(numpy.asarray(W, dtype=theano.config.floatX), borrow=True)
            self.W = W
        else:
            # initialize weights with random weights
            W_bound = numpy.sqrt(6. / (fan_in + fan_out))
            self.W = theano.shared(numpy.asarray(
                rng.uniform(low=-W_bound, high=W_bound, size=filter_shape),
                dtype=theano.config.floatX),
                               borrow=True)
        

        if (b):
            #self.b = theano.shared(value=b, borrow=True)
            self.b = b
        else:
            # the bias is a 1D tensor -- one bias per output feature map
            b_values = numpy.zeros((filter_shape[0],), dtype=theano.config.floatX)
            self.b = theano.shared(value=b_values, borrow=True)

        # convolve input feature maps with filters
        conv_out = conv.conv2d(input=input, filters=self.W,
                filter_shape=filter_shape, image_shape=image_shape)

        # downsample each feature map individually, using maxpooling
        pooled_out = downsample.max_pool_2d(input=conv_out,
                                            ds=poolsize, ignore_border=True)

        # add the bias term. Since the bias is a vector (1D array), we first
        # reshape it to a tensor of shape (1,n_filters,1,1). Each bias will
        # thus be broadcasted across mini-batches and feature map
        # width & height
        self.output = T.tanh(pooled_out + self.b.dimshuffle('x', 0, 'x', 'x'))

        # store parameters of this layer
        self.params = [self.W, self.b]

class CNN(object):
    def __init__(self, batch_size, layers, rng=None):

        ins = 48
        outs = 6
        width = 7
        nkerns0 = 20
        nkerns1 = 50
        nkerns = [nkerns0, nkerns1]
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

        if (rng == None):
            rng = numpy.random.RandomState(23455)
        self.x = T.matrix('x')  # the spectrum data
        self.y = T.matrix('y')  # target tonnetz vectors

        # Reshape matrix of rasterized images of shape (batch_size,48*7)
        # to a 4D tensor, compatible with our LeNetConvPoolLayer
        layer0_input = self.x.reshape((batch_size, 1, ins, width))
        
        # Construct the first convolutional pooling layer:
        # filtering reduces the image size to (48-7+1,7-2+1)=(42,6)
        # maxpooling reduces this further to (42/1,6/2) = (42,3)
        # 4D output tensor is thus of shape (batch_size,nkerns[0],42,3)
        self.layer0 = LeNetConvPoolLayer(rng, input=layer0_input,
            image_shape=(batch_size, 1, l0rows, l0cols),
            filter_shape=(nkerns[0], 1, f0rows, f0cols),
            poolsize=(pool_row, pool_col), W=layers[0][0], b=layers[0][1])
    
        # Construct the second convolutional pooling layer
        # filtering reduces the image size to (42-15+1,3-2+1)=(28,2)
        # maxpooling reduces this further to (28/2,2/2) = (14,1)
        # 4D output tensor is thus of shape (nkerns[0],nkerns[1],14,1)
        self.layer1 = LeNetConvPoolLayer(rng, input=self.layer0.output,
            image_shape=(batch_size, nkerns[0], l1rows, l1cols),
            filter_shape=(nkerns[1], nkerns[0], f1rows, f1cols),
            poolsize=(pool_row, pool_col), W=layers[1][0], b=layers[1][1])

        # the TanhLayer being fully-connected, it operates on 2D matrices of
        # shape (batch_size,num_pixels) (i.e matrix of rasterized images).
        # This will generate a matrix of shape (20,32*14*1) = (20,448)
        layer2_input = self.layer1.output.flatten(2)

        # construct a fully-connected sigmoidal layer
        self.layer2 = HiddenLayer(rng, input=layer2_input,
                         n_in=nkerns[1] * l2rows * l2cols,
                         n_out=n_sigm, activation=T.tanh,
                         W=layers[2][0], b=layers[2][1])

        # classify the values of the fully-connected sigmoidal layer
        self.layer3 = LogisticRegression(input=self.layer2.output,
                                         n_in=n_sigm, n_out=outs,
                                         W=layers[3][0], b=layers[3][1])
        
        self.params = self.layer3.params + self.layer2.params + \
                self.layer1.params + self.layer0.params

    def get_cost(self, y):
        return self.layer3.quadratic_loss(y)

    def get_result(self, m):
        z = self.layer3.p_y_given_x
        result = theano.function([], z, givens={self.x: m})
        return result()
