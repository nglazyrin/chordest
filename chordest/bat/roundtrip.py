import os
import subprocess
import matplotlib.pyplot as p
import csv
import sys
import numpy as np

def runCommand(command, cwd):
    p = subprocess.Popen(command,
                     stdout=subprocess.PIPE,
                     stderr=subprocess.STDOUT,
                     cwd=cwd)
    return iter(p.stdout.readline, b'')

def doRoundtrip(dir):
    java_path = 'C:/Java/jdk1.7.0_05/bin/java.exe'
    classpath = '../../../target/chordest.jar;../../../config'
    file_list_path = '../../../src/main/resources/filelists/q_bin.txt'
    lab_path = '../../../lab/' # must end with /
    c = os.path.join(dir, 'chordest.properties')
    if (os.path.isfile(c)):
        command = '%(java_path)s -cp %(classpath)s chordest.main.Roundtrip chordest.properties %(file_list_path)s %(lab_path)s' % \
            {"java_path": java_path, "classpath": classpath, "file_list_path": file_list_path, "lab_path": lab_path}
        print '> ' + command
        result = runCommand(command, dir)
        drawErrors(os.path.join(dir, 'logs', 'errors.csv'), os.path.join(dir, 'logs', 'errors.png'))
        return result
    return []

def drawErrors(csv_file, png_file):
    rows = []
    with open(csv_file, 'rb') as f:
        reader = csv.reader(f, delimiter=';')
        for row in reader:
            rows.append(row)
    errors = np.array( [[x[1], float(x[2])] for x in rows if x[0] == 'Triads'] )
    p.bar(range(len(errors)), [float(x) for x in errors[:,1]], align='center')
    p.xticks(range(len(errors)), errors[:,0], rotation='vertical')
    p.tight_layout()
    p.savefig(png_file)

for root, dirs, _ in os.walk('.'):
    for d in dirs:
        for outputLine in doRoundtrip(d):
            print outputLine
