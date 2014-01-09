import os
import matplotlib.pyplot as p
from matplotlib import cm
import csv
import numpy as np
import string
from collections import OrderedDict

class ChartData(object):
    def __init__(self, name):
        self.bars = []
        self.name = name
        self.col_names = []
        self.value = None

def readErrors(csv_file):
    rows = []
    with open(csv_file, 'rb') as f:
        reader = csv.reader(f, delimiter=';')
        for row in reader:
            rows.append(row)
    return rows

def drawBarChart(p, col_names, target_file):
    p.xticks(range(len(col_names)), col_names, rotation='vertical')
    p.grid(axis='x')
    p.tight_layout()
    p.savefig(target_file)
    p.clf()
    print target_file

def process(files, values):
    waor3 = 0
    waor4 = 0
    segm = 0
    rows = 0
    length = 0
    for file in files:
        [waor3, waor4, rows, length, segm] = process_file(file, values, waor3, waor4, rows, length, segm)
    waor3 = waor3 / length
    waor4 = waor4 / length
    segm = segm / rows
    return [waor3, waor4, segm]

def process_file(file0, values, waor3, waor4, rows, length, segm):
    with open(file0, 'rb') as s0:
        sr0 = csv.DictReader(s0, delimiter=';')
        for row in sr0:
            overlap3 = float(row['overlap3'].replace(',','.'))
            overlap4 = float(row['overlap4'].replace(',','.'))
            effective_length = float(row['effective_length'].replace(',','.'))
            segmentation = float(row['segmentation'].replace(',','.'))
            name = row['name']
            segm = segm + segmentation
            rows = rows + 1
            waor3 = waor3 + overlap3 * effective_length
            waor4 = waor4 + overlap4 * effective_length
            length = length + effective_length
            if (name in values):
                values[name].append(str(overlap3))
            else:
                values[name] = [ str(overlap3) ]
    return [waor3, waor4, rows, length, segm]

def save(file, current_dir, summary, waor3, waor4, segm):
    with open(file, 'wb') as s:
        s.write('Triads: ' + '{:.4}'.format(waor3) + '\n')
        s.write('Tetrads: ' + '{:.4}'.format(waor4) + '\n')
        s.write('Segmentation: ' + '{:.4}'.format(segm) + '\n')
    with open(summary, 'a+b') as s:
        s.write(current_dir + '\n')
        s.write('Triads: ' + '{:.4}'.format(waor3) + '\n')
        s.write('Tetrads: ' + '{:.4}'.format(waor4) + '\n')
        s.write('Segmentation: ' + '{:.4}'.format(segm) + '\n\n')

def save_dict(file, values, dirnames):
    od = OrderedDict(sorted(values.items(), key=lambda t: t[0]))
    with open(file, 'wb') as s:
        s.write('name,' + string.join(dirnames, sep=',') + '\n')
        for k in od.keys():
            s.write(k + ',' + string.join(od[k], sep=',') + '\n')

current_dir = '.'
for root, dirs, _ in os.walk(current_dir):
    dir_dict = {}
    for d in dirs:
        errors_csv = os.path.join(current_dir, d, 'logs', 'errors.csv')
        idx = d.find('=')
        if (idx >= 0 and os.path.isfile(errors_csv)):
            key = d[0:idx]
            if not dir_dict.has_key(key):
                dir_dict[key] = []
            dir_dict[key].append(d)
    
    for (prefix, arr) in dir_dict.items():
        data = [ChartData(os.path.join(current_dir, 'png', prefix + x + '.png')) for x in ['3', '4', '3a', '4a']]
        has_drawn_something = False
        dirnames = []
        values = dict()
        summary_txt = os.path.join(current_dir, 'summary', prefix + '.txt')
        summary_csv = os.path.join(current_dir, 'summary', prefix + '.csv')
        for i,d in enumerate(arr):
            width = 1.0 / (len(arr) + 1)
            offset = i * width
            errors_csv = os.path.join(current_dir, d, 'logs', 'errors.csv')
            similarity_csv = os.path.join(current_dir, d, 'logs', 'similarity.csv')
            stats_txt = os.path.join(current_dir, d, 'stats.txt')
            if (os.path.isfile(errors_csv)):
                rows = readErrors(errors_csv)
                temp = np.array( [[x[1], float(x[2])] for x in rows if x[0] == 'Triads' and x[1].find(',') < 0] )
                data[0].col_names, data[0].value = temp[:,0], temp[:,1]
                temp = np.array( [[x[1], float(x[2])] for x in rows if x[0] == 'Tetrads' and x[1].find(',') < 0] )
                data[1].col_names, data[1].value = temp[:,0], temp[:,1]
                for x in rows:
                    if x[0] == 'Triads' and x[1].find(',') > 0:
                        data[2].col_names = x[1].split(',')
                        data[2].value = x[-12:-(12 - len(data[2].col_names))]
                    if x[0] == 'Tetrads' and x[1].find(',') > 0:
                        data[3].col_names = x[1].split(',')
                        data[3].value = x[-12:-(12 - len(data[3].col_names))]
                for f in data:
                    if (f.value != None):
                        p.figure(f.name)
                        b = p.bar(np.arange(len(f.value)) + offset, [float(x) or 0.01 for x in f.value], width, color=cm.jet(offset))
                        f.bars.append(b)                 
                has_drawn_something = True
            if (os.path.isfile(similarity_csv)):
                dirnames.append(d)
                (waor3, waor4, segm) = process([ similarity_csv ], values)
                save(stats_txt, d, summary_txt, waor3, waor4, segm)
        if has_drawn_something:
            for f in data:
                p.figure(f.name)
                p.legend( [b[0] for b in f.bars], arr )
                drawBarChart(p, f.col_names, f.name)
        save_dict(summary_csv, values, dirnames)
