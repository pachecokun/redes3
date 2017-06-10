import subprocess

def snmpget(ip,oid):
	#ejecutamos comando
	o= subprocess.check_output(['snmpget','-Os','-c','public','-v','2c',ip,oid])
	#obtenemos variable
	return o.strip().split('=')[1].split(':')

def snmpwalk(ip,oid):
	#ejecutamos comando
	o = subprocess.check_output(['snmpwalk','-Os','-c','public','-v','2c',ip,oid])
	#separamos lineas
	lines = o.split('\n')[:-1]
	res = []
	#obtenemos variables
	for l in lines:
		res.append(l.strip().split('=')[1].split(':')[1].strip())
	return res
