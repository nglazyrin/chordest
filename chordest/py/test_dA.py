# -*- coding: utf-8 -*-
"""
Created on Fri Jan 04 20:10:46 2013

@author: Nikolay
"""

import os
from test1 import autoencode_file

input_dir = os.path.abspath("E:\\Dev\\git\\my_repository\\chordest\\csv\\test")
output_dir = os.path.abspath("E:\\Dev\\git\\my_repository\\chordest\\csv\\encoded")

for dirname, dirnames, filenames in os.walk(input_dir):
    for filename in filenames:
        if filename.endswith('.csv'):
            source = os.path.join(dirname, filename)
            target = os.path.join(output_dir, filename)
            autoencode_file(source, target)
