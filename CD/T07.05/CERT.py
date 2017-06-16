#!/usr/bin/python
from ftplib import *
import os

def obtenStartup(host, usuario, contra):
	try:
		ftp = FTP(host,usuario,contra)
		ftp.retrbinary('RETR startup-config',open('startup-config','wb').write)
	except:
		print "Error al establecer comunicacion"; exit(0)
	#print ftp.retrlines("RETR startup-config")
	return ftp

def cambiaStartup(listaCambios):
	startup=open('startup-config','r')
	archivo=startup.read()
	for cambio in listaCambios:
		if cambio[0] == 505:
			archivo=archivo.replace(cambio[1],cambio[2])
			#archivo=archivo.replace(cambio[1],cambio[2],numOcurrencias)
		elif cambio[0] == 448:
			acumulador = ''
			for campo in archivo.split("!"):
				if campo.find(cambio[1]) != -1:
					if campo.find(cambio[2]) == -1
						campo=campo+cambio[2]+"\n"
				acumulador=acumulador+campo+"!"
			archivo=acumulador
		elif cambio[0] == 83:
			archivo=archivo.replace(cambio[1],'')
		else:
			print "No se pudo aplicar el cambio: "+cambio
	newStartup=open('startup-config','w')
	newStartup.write(archivo)

def enviaStartup(ftp):
	#ftp.storbinary('STOR startup-config',open('startup-config','rb').read)
	ftp.storbinary('STOR startup-config',open('startup-config','r'))

def ejecutaComandos(ftp,listaComandos):
	for comando in listaComandos:
		resp = ftp.sendcmd(comando)
		#print resp

def cambios(path,listaComandos,listaCambios):
	fRouter = open(path,"r")
	while True:
		router=fRouter.readline()
		if not router:break
		host,usr,pas=router.split(" ")
		pas = pas.replace("\n",'')
		ftp = obtenStartup(host,usr,pas)
		print("Se obtuvo el archivo startup-config de ",host)
		ejecutaComandos(ftp, listaComandos)
		cambiaStartup(listaCambios)
		enviaStartup(ftp)
		ftp.quit()
	os.remove('startup-config')

#main
cambios("datosServer.data",["ACCT"],
						  [[505,"network 192.168.2.0/24","network 192.168.123.0/24"],
						   [448,"service","service dhcp"],
						   [83,"service tftp"]])