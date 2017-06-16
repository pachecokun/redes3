from snmp import *
import time
import pprint
from gui import *
import threading

nodes = []
interval = 0.1
pp = pprint.PrettyPrinter(indent=4)

gui = GUI()

def initUI():
	gui.init()

t = threading.Thread(target=initUI)
t.start()
time.sleep(1)

conf = open('monitor.conf')

for l in conf.readlines():
	l = l.strip()
	if l != '':
		toks = l.split()
		if toks[0]=='monitor':
			nodes.append(toks[1])
		elif toks[0]=='interval':
			interval = float(toks[1])
conf.close()

data = {}

for node in nodes:
	gui.add_node(node)
	data[node] = []
print(data)

def monitor():
	for node in data:
		names = snmpwalk(node,'interfaces.ifTable.ifEntry.ifDescr')
		rx = snmpwalk(node,'interfaces.ifTable.ifEntry.ifInOctets')
		tx = snmpwalk(node,'interfaces.ifTable.ifEntry.ifOutOctets')

		for i in range(len(names)):
			if len(data[node])< i+1:
				data[node].append({
					'name':names[i],
					'bwr':[],
					'bwt':[],
					'file':open('bw/'+node+'_'+str(i)+'.txt','w')
				})
				data[node][i]['file'].write(names[i]+'\n')
			else:
				bwr = (int(rx[i])-data[node][i]['rx'])/interval
				bwt = (int(tx[i])-data[node][i]['tx'])/interval
				data[node][i]['file'].write(str(bwr)+' '+str(bwt)+'\n')
				data[node][i]['bwr'].append(bwr)
				data[node][i]['bwt'].append(bwt)
				if len(data[node][i]['bwr'])>10:
					data[node][i]['bwr'] = data[node][i]['bwr'][-10:]
					data[node][i]['bwt'] = data[node][i]['bwt'][-10:]


			data[node][i]['rx'] = int(rx[i])
			data[node][i]['tx'] = int(tx[i])
	gui.update(data)
	#gui.update(data)
	#pp.pprint(data)
	if t.is_alive():
		timer = threading.Timer(interval,monitor)
		timer.start()

monitor()

t.join()
