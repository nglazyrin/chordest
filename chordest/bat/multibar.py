import os
import matplotlib.pyplot as p
from matplotlib import cm
import csv
import numpy as np

def readErrors(csv_file):
    rows = []
    with open(csv_file, 'rb') as f:
        reader = csv.reader(f, delimiter=';')
        for row in reader:
            rows.append(row)
    return rows

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
        has_drawn_something = False
        bars = []
        for i,d in enumerate(arr):
            width = 1.0 / (len(arr) + 1)
            offset = i * width
            errors_csv = os.path.join(current_dir, d, 'logs', 'errors.csv')
            if (os.path.isfile(errors_csv)):
                rows = readErrors(errors_csv)
                errors = np.array( [[x[1], float(x[2])] for x in rows if x[0] == 'Triads'] )
                b = p.bar(np.arange(len(errors)) + offset, [float(x) for x in errors[:,1]], width, color=cm.jet(offset))
                bars.append(b)
                p.xticks(range(len(errors)), errors[:,0], rotation='vertical')
                has_drawn_something = True
        if has_drawn_something:
            p.legend( [b[0] for b in bars], arr )
            p.grid(axis='x')
            p.tight_layout()
            png_file = os.path.join(current_dir, prefix + '.png')
            p.savefig(png_file)
            p.clf()
            print png_file
