import csv
import numpy as np
import scipy
from statsmodels.stats.multicomp import pairwise_tukeyhsd
from statsmodels.stats.multicomp import MultiComparison

def readData(dirName, name):
    dr0 = csv.DictReader(open(dirName + 'similarity-0.csv'))
    arr0 = np.array([float(row['overlap']) for row in dr0])
    dr1 = csv.DictReader(open(dirName + 'similarity-1.csv'))
    arr1 = np.array([float(row['overlap']) for row in dr1])
    arr = np.hstack((arr0, arr1))
    c = np.chararray(arr.shape, itemsize=4)
    c[:] = name
    return (arr, np.vstack((arr, c)).transpose())

p = 'E:/Dev/git/my_repository/chordest/work/ismir2013/'
#(a1, p1) = readData(p + '36,3-2-2,003-001,15-15,10/', 'lay1')
#(a2, p2) = readData(p + '48-36-24,3-2-2,003-001,15-15,10/', 'lay3')
#(a3, p3) = readData(p + '48-36,3-2,003-001,15-15,10/', 'lay2')
(a1, p1) = readData(p + 'sda/48-36,3-2,003-001,15-15,10/', 'sl2_')
(a2, p2) = readData(p + 'sda-nolog/48-36,3-2-2,003-001,15-15,10/', 'snl2')
(a3, p3) = readData(p + 'mlp/48-36,001,15,10/', 'mlp2')
#(a4, p4) = readData(p + 'sda/48-36-24,3-2-2,003-001,15-15,10/', 'sl3_')
#(a5, p5) = readData(p + 'sda-nolog/48-36-24,3-2-2,003-001,15-15,10/', 'snl3')
#(a6, p6) = readData(p + 'mlp/48-36-24,001,15,10/', 'mlp3')
(a7, p7) = readData(p + 'sda/36,3-2-2,003-001,15-15,10/', 'sl1_')
(a8, p8) = readData(p + 'sda-nolog/36,3-2-2,003-001,15-15,10/', 'snl1')
(a9, p9) = readData(p + 'mlp/36,001,15,10/', 'mlp1')
#(a1, p1) = readData(p + '36,001,15,10/', 'lay1')
#(a2, p2) = readData(p + '48-36-24,001,15,10/', 'lay3')
#(a3, p3) = readData(p + '48-36,001,15,10/', 'lay2')
a = np.vstack((p1, p2, p3, p7, p8, p9))

print 'Friedman chi-square test'
print scipy.stats.friedmanchisquare(a1, a2, a3, a7, a8, a9)

b = np.rec.array(a, dtype=[('val', '<f'), ('feature', '|S4')])
res2 = pairwise_tukeyhsd(b['val'], b['feature'])
mod = MultiComparison(b['val'], b['feature'])

print res2

import matplotlib.pyplot as plt
plt.plot([0,1,2, 3, 4, 5], res2[1][2], 'o')
plt.errorbar([0,1,2, 3, 4, 5], res2[1][2], yerr=np.abs(res2[1][4].T-res2[1][2]), ls='o')
xlim = -0.5, 2.5
plt.hlines(0, *xlim)
plt.xlim(*xlim)
pair_labels = mod.groupsunique[np.column_stack(res2[1][0])]
plt.xticks([0,1,2,3,4,5], pair_labels)
plt.title('Multiple Comparison of Means - Tukey HSD, FWER=0.05' +
          '\n Pairwise Mean Differences')
plt.show()